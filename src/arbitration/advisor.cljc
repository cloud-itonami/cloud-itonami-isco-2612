(ns arbitration.advisor
  "Arbitration Advisor — the advisor named in this repository's
  README, proposing an arbitration operation (draft a finding, approve
  binding award issuance, approve case acceptance) from a case
  submission, party consent and hearing record. Swappable mock/llm;
  the advisor ONLY proposes — `arbitration.governor` checks the
  award-amount ceiling and recusal-check clearance independently and
  always escalates binding-award-issuance and case-acceptance
  decisions. Modeled on cloud-itonami-isco-4311's advisor.

  A proposal: {:op :approve-draft-finding|:approve-binding-award-issuance|:approve-case-acceptance
               :effect :propose :case-id str :award-amount number
               :stake kw :confidence n :rationale str}")

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake case-id award-amount] :as request}]
  {:op op
   :effect :propose
   :case-id case-id
   :award-amount award-amount
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are an arbitration advisor. Given a request, propose an :op,
   the :case-id and :award-amount, an honest :confidence and a :stake.
   Never propose an award amount beyond the case's registered ceiling,
   or a finding for a case without a cleared recusal check — the
   governor checks both against the registered case record. Binding
   award issuance and accepting a case always require human sign-off
   regardless of confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
