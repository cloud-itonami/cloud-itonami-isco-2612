(ns arbitration.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [arbitration.store :as store]
            [arbitration.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Arbitration"})
    (store/register-case! st {:case-id "C-1" :client-id "client-1"
                              :name "case-042"
                              :max-award-amount 100000
                              :recusal-check-cleared? true})
    st))

(defn- finding-op [amount]
  {:op :approve-draft-finding :effect :propose :case-id "C-1"
   :award-amount amount :confidence 0.9 :stake :low})

(def ^:private req {:client-id "client-1"})

(deftest ok-within-ceiling-and-cleared
  (let [st (fresh-store)
        v (governor/check req {} (finding-op 50000) st)]
    (is (:ok? v))))

(deftest ok-at-exact-ceiling-boundary
  (testing "the award-amount ceiling is inclusive"
    (let [st (fresh-store)
          v (governor/check req {} (finding-op 100000) st)]
      (is (:ok? v)))))

(deftest hard-on-award-exceeds-ceiling
  (testing "awarding beyond the case's registered ceiling is an ultra vires ruling, not a generous award"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (finding-op 500000) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :award-exceeds-ceiling (:rule %)) (:violations v))))))

(deftest hard-on-recusal-check-not-cleared
  (testing "drafting a finding without a cleared recusal check is a conflict-of-interest violation, not efficient service"
    (let [st (store/mem-store)]
      (store/register-client! st {:client-id "client-1" :name "Kobo Arbitration"})
      (store/register-case! st {:case-id "C-1" :client-id "client-1"
                                :name "case-042"
                                :max-award-amount 100000
                                :recusal-check-cleared? false})
      (let [v (governor/check req {} (assoc (finding-op 50000) :confidence 0.99) st)]
        (is (:hard? v))
        (is (some #(= :recusal-check-not-cleared (:rule %)) (:violations v)))))))

(deftest hard-on-unknown-case
  (let [st (fresh-store)
        v (governor/check req {} (assoc (finding-op 50000) :case-id "C-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-case (:rule %)) (:violations v)))))

(deftest hard-on-foreign-case
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other"})
    (let [v (governor/check {:client-id "client-2"} {} (finding-op 50000) st)]
      (is (:hard? v))
      (is (some #(= :case-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {} (finding-op 50000) st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (finding-op 50000) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest always-escalates-binding-award-issuance-even-at-high-confidence
  (testing "no binding award issuance without the governor gate"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-binding-award-issuance :effect :propose
                                    :case-id "C-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-case-acceptance-even-at-high-confidence
  (testing "accepting a case always requires human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-case-acceptance :effect :propose
                                    :case-id "C-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (finding-op 50000) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
