(ns arbitration.governor
  "ArbitrationGovernor — the independent safety/traceability layer
  named in this repository's README/business-model.md, gating every
  finding an advisor may draft for a case. The governor never
  dispatches hardware itself and never issues a binding award.
  Modeled on cloud-itonami-isco-4311's bookkeeping.governor. Task
  twist: a proposed award amount is an arithmetic ceiling against the
  case's registered jurisdictional/agreed award ceiling, and a finding
  cannot be drafted for a case until its recusal/conflict-of-interest
  check has cleared.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance   — the disputing party/firm/institution must
                             be registered.
    2. no-actuation        — proposal :effect must be :propose (the
                             governor never dispatches hardware and
                             never issues a binding award; it only
                             gates what the advisor may draft).
    3. case basis          — a finding proposal must cite a
                             REGISTERED case belonging to this client.
    4. award-amount ceiling — the proposed award amount must not
                             exceed the case's registered
                             `:max-award-amount` (awarding beyond the
                             case's registered ceiling is an ultra
                             vires ruling, not a generous award).
    5. recusal-check cleared — the case must have
                             `:recusal-check-cleared?` true before any
                             finding can be drafted (drafting a
                             finding without a cleared recusal check
                             is a conflict-of-interest violation, not
                             efficient service).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls — these are :high/
  :safety-critical regardless of confidence):
    6. :op :approve-binding-award-issuance (no binding award issuance
                             without the governor gate).
    7. :op :approve-case-acceptance (accepting a case always requires
                             human sign-off).
    8. low confidence (< `confidence-floor`)."
  (:require [arbitration.store :as store]))

(def confidence-floor 0.6)

(def ^:private always-escalate-ops #{:approve-binding-award-issuance
                                     :approve-case-acceptance})

(defn- hard-violations [{:keys [request proposal]} client-record c]
  (let [{:keys [op award-amount]} proposal
        finding? (= :approve-draft-finding op)]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor は拘束力ある裁定を直接発行しない）"})

      (and finding? (nil? c))
      (conj {:rule :unknown-case :detail "未登録 case への finding 起案は不可"})

      (and finding? c (not= (:client-id c) (:client-id request)))
      (conj {:rule :case-wrong-client :detail "case が別 client のもの"})

      (and finding? c (number? award-amount) (> award-amount (:max-award-amount c)))
      (conj {:rule :award-exceeds-ceiling
             :detail (str "裁定額 " award-amount " > 登録済み管轄上限 "
                          (:max-award-amount c) "（登録済み上限を超える裁定は越権行為であって寛大な裁定ではない）")})

      (and finding? c (not (:recusal-check-cleared? c)))
      (conj {:rule :recusal-check-not-cleared
             :detail "忌避/利益相反チェックが完了していない case への finding 起案は利益相反違反であって効率的サービスではない"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `arbitration.store/Store`. Pure — never mutates
  the store, never issues a binding award."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        c (some->> (:case-id proposal) (store/case-record store))
        hard (hard-violations {:request request :proposal proposal}
                              client-record c)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
