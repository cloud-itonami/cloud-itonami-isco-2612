(ns arbitration.store
  "SSoT for the ISCO-08 2612 independent arbitration & adjudication
  practice actor (itonami actor pattern, ADR-2607011000 / CLAUDE.md
  Actors section; README's 'Robotics premise' — a hearing-record and
  exhibit-handling robot performs exhibit intake, transcript binding
  and hearing-room material organization under this advisor/governor
  pair, which never dispatches hardware itself and never issues a
  binding award). This practice offers private arbitration, mediation
  and adjudication services, not public judicial office. Modeled on
  cloud-itonami-isco-4311's bookkeeping.store.

  Domain:

    client — a registered disputing party/firm/institution
             (:client-id, :name)
    case   — a registered arbitration case {:case-id :client-id :name
             :max-award-amount number :recusal-check-cleared?
             boolean}. `:max-award-amount` is the registered
             jurisdictional/agreed award ceiling a proposed award
             amount must not exceed — awarding beyond the case's
             registered ceiling is an ultra vires ruling, not a
             generous award. `:recusal-check-cleared?` records whether
             a recusal/conflict-of-interest disclosure has cleared for
             this case — drafting a finding for a case without a
             cleared recusal check is a conflict-of-interest
             violation, not efficient service.
    record — a committed operating record (a drafted finding) —
             written ONLY via commit-record!.
    ledger — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (client [s client-id])
  (case-record [s case-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (register-case! [s c])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (case-record [_ case-id] (get-in @a [:cases case-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (register-case! [s c]
    (swap! a assoc-in [:cases (:case-id c)] c) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :cases {} :records [] :ledger []}
                                   seed)))))
