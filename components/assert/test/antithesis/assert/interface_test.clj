(ns antithesis.assert.interface-test
  (:require [antithesis.assert.core :as core]
            [antithesis.assert.interface :as sut]
            [antithesis.handler.interface :as handler]
            [clojure.test :refer [deftest is testing use-fixtures]]))

(defn reset-trackers!
  [f]
  (reset! (var-get #'core/tracker) {})
  (reset! (var-get #'core/guidance-tracker) {})
  (f))

(use-fixtures :each reset-trackers!)

(deftest always-deduplication
  (testing "first pass emits, second pass is silent"
    (let [outputs (atom [])]
      (with-redefs [handler/output-json!
                    #(swap! outputs conj %)]
        (sut/always true "dedup-pass" {})
        (sut/always true "dedup-pass" {})
        (is (= 1 (count @outputs))))))
  (testing "first fail emits after first pass"
    (let [outputs (atom [])]
      (with-redefs [handler/output-json!
                    #(swap! outputs conj %)]
        (sut/always true "dedup-fail" {})
        (sut/always false "dedup-fail" {})
        (sut/always false "dedup-fail" {})
        (is (= 2 (count @outputs)))))))

(deftest sometimes-emits-on-first-pass-and-fail
  (let [outputs (atom [])]
    (with-redefs [handler/output-json!
                  #(swap! outputs conj %)]
      (sut/sometimes false "sometimes-test" {})
      (sut/sometimes true "sometimes-test" {})
      (sut/sometimes true "sometimes-test" {})
      (is (= 2 (count @outputs))))))

(deftest reachable-emits-once
  (let [outputs (atom [])]
    (with-redefs [handler/output-json!
                  #(swap! outputs conj %)]
      (sut/reachable "reach-me" {})
      (sut/reachable "reach-me" {})
      (is (= 1 (count @outputs))))))

(deftest unreachable-emits-on-first-call
  (let [outputs (atom [])]
    (with-redefs [handler/output-json!
                  #(swap! outputs conj %)]
      (sut/unreachable "unreachable-me" {})
      (sut/unreachable "unreachable-me" {})
      (is (= 1 (count @outputs))))))

(deftest numeric-guidance-deduplication
  (testing "numeric guidance emits only when gap strictly improves"
    (let [outputs (atom [])]
      (with-redefs [handler/output-json!
                    #(swap! outputs conj %)]
        (sut/always-greater-than 5 3 "numeric-test" {}) ; gap=2, improves
                                                        ; from -Inf
        (sut/always-greater-than 4 3 "numeric-test" {}) ; gap=1, worse — no
                                                        ; emit
        (sut/always-greater-than 6 3 "numeric-test" {}) ; gap=3, improves
        ;; assert emits: 1 (first pass) + 2 more passes (no emit from
        ;; assert)
        ;; guidance emits: 2 (gap 2, gap 3)
        (let [guidance-outputs (filter #(contains? % :antithesis_guidance)
                                       @outputs)]
          (is (= 2 (count guidance-outputs))))))))

;; --- rule-success.AlwaysOrUnreachableAssert ---

(deftest always-or-unreachable-deduplication
  (testing "first pass emits, subsequent passes are silent"
    (let [outputs (atom [])]
      (with-redefs [handler/output-json! #(swap! outputs conj %)]
        (sut/always-or-unreachable true "aou-dedup" {})
        (sut/always-or-unreachable true "aou-dedup" {})
        (is (= 1 (count @outputs))))))
  (testing "must-hit is false in output"
    (let [outputs (atom [])]
      (with-redefs [handler/output-json! #(swap! outputs conj %)]
        (sut/always-or-unreachable true "aou-must-hit" {})
        (is (false? (get-in (first @outputs)
                            [:antithesis_assert :must_hit]))))))
  (testing "first fail emits once"
    (let [outputs (atom [])]
      (with-redefs [handler/output-json! #(swap! outputs conj %)]
        (sut/always-or-unreachable false "aou-fail" {})
        (sut/always-or-unreachable false "aou-fail" {})
        (is (= 1 (count @outputs)))))))

;; --- rule-success.CatalogRegistration ---

(deftest catalog-registration-always-emits
  (testing "catalog calls (hit=false) always emit regardless of call count"
    (let [outputs (atom [])]
      (with-redefs [handler/output-json! #(swap! outputs conj %)]
        (sut/raw-assert "always"
                        "Always" "my.ns"
                        "" "file.clj"
                        1 1
                        "" false
                        "catalog-msg" nil
                        false true)
        (sut/raw-assert "always"
                        "Always" "my.ns"
                        "" "file.clj"
                        1 1
                        "" false
                        "catalog-msg" nil
                        false true)
        (is (= 2 (count @outputs))))))
  (testing "catalog does not populate the assertion tracker"
    (with-redefs [handler/output-json! (constantly nil)]
      (sut/raw-assert "always"
                      "Always" "my.ns"
                      "" "file.clj"
                      1 1
                      "" false
                      "catalog-no-track" nil
                      false true))
    (is (nil? (get @(var-get #'core/tracker) "catalog-no-track")))))

;; --- rule-success.BooleanGuidanceEmitted ---

(deftest boolean-guidance-always-emits
  (testing "boolean guidance emits on every call with no deduplication"
    (let [outputs (atom [])]
      (with-redefs [handler/output-json! #(swap! outputs conj %)]
        (sut/always-some {:a true
                          :b false}
                         "bool-guidance-test"
                         {})
        (sut/always-some {:a true
                          :b false}
                         "bool-guidance-test"
                         {})
        (sut/always-some {:a true
                          :b false}
                         "bool-guidance-test"
                         {}))
      (let [guidance-outs (filter #(contains? % :antithesis_guidance) @outputs)]
        (is (= 3 (count guidance-outs)))))))

;; --- entity-fields.Assertion, entity-fields.Location ---

(deftest assertion-output-shape
  (testing "assertion output contains all spec-required fields"
    (let [outputs (atom [])]
      (with-redefs [handler/output-json! #(swap! outputs conj %)]
        (sut/always true "shape-test" {:extra "data"}))
      (let [out (get-in (first @outputs) [:antithesis_assert])]
        (is (= "always" (:assert_type out)))
        (is (true? (:condition out)))
        (is (= "shape-test" (:message out)))
        (is (= "shape-test" (:id out)))
        (is (true? (:must_hit out)))
        (is (true? (:hit out)))
        (is (map? (:location out)))
        (is (map? (:details out)))))))

(deftest location-fields
  (testing "location map contains all declared fields with correct types"
    (let [outputs (atom [])]
      (with-redefs [handler/output-json! #(swap! outputs conj %)]
        (sut/always true "location-test" {}))
      (let [loc (get-in (first @outputs) [:antithesis_assert :location])]
        (is (contains? loc :begin_column))
        (is (contains? loc :begin_line))
        (is (contains? loc :class))
        (is (contains? loc :file))
        (is (contains? loc :function))
        (is (integer? (:begin_line loc)))
        (is (integer? (:begin_column loc)))
        (is (string? (:class loc)))
        (is (string? (:file loc)))
        (is (string? (:function loc)))))))

;; --- entity-fields.GuidanceSignal ---

(deftest guidance-output-shape
  (testing "numeric guidance output contains all spec-required fields"
    (let [outputs (atom [])]
      (with-redefs [handler/output-json! #(swap! outputs conj %)]
        (sut/always-greater-than 5 3 "guidance-shape-test" {}))
      (let [guidance-outs (filter #(contains? % :antithesis_guidance) @outputs)
            out (:antithesis_guidance (first guidance-outs))]
        (is (some? out))
        (is (contains? out :guidance_type))
        (is (contains? out :guidance_data))
        (is (contains? out :maximize))
        (is (contains? out :message))
        (is (contains? out :id))
        (is (contains? out :location))
        (is (contains? out :hit))))))

;; --- entity-optional.GuidanceSignal.mark ---

(deftest guidance-signal-mark-optional
  (testing "mark is absent before any numeric emission for a message"
    (is (nil? (get @(var-get #'core/guidance-tracker) "mark-absent-test"))))
  (testing "mark is set after first non-NaN numeric emission"
    (with-redefs [handler/output-json! (constantly nil)]
      (core/guidance! {:column        1
                       :file          "f.clj"
                       :guidance-type "numeric"
                       :hit           true
                       :line          1
                       :maximize      false
                       :ns-name       "ns"}
                      {:gap 2.0}
                      "mark-present-test"))
    (is (= 2.0
           (:mark (get @(var-get #'core/guidance-tracker)
                       "mark-present-test"))))))

(deftest numeric-guidance-nan-behavior
  (testing
    "NaN gap emits but does not update mark; next real value still sees no-mark and emits"
    (let [outputs (atom [])
          opts    {:column        1
                   :file          "f.clj"
                   :guidance-type "numeric"
                   :hit           true
                   :line          1
                   :maximize      true
                   :ns-name       "ns"}]
      (with-redefs [handler/output-json! #(swap! outputs conj %)]
        (core/guidance! opts {:gap Double/NaN} "nan-test") ; no-mark → emit,
                                                           ; no update
        (core/guidance! opts {:gap Double/NaN} "nan-test") ; still no-mark →
                                                           ; emit, no update
        (core/guidance! opts {:gap 5.0} "nan-test")        ; no-mark → emit,
                                                           ; set mark=5.0
        (core/guidance! opts {:gap Double/NaN} "nan-test") ; NaN always
                                                           ; emits, mark
                                                           ; stays
        (core/guidance! opts {:gap 3.0} "nan-test")) ; 3.0 < 5.0, maximize →
                                                     ; no emit
      (let [guidance-outs (filter #(contains? % :antithesis_guidance) @outputs)]
        (is (= 4 (count guidance-outs)))))))

;; --- config-default.first_emission_count ---

(deftest config-first-emission-count-is-one
  (testing "first pass emits exactly once; further passes are silent"
    (let [outputs (atom [])]
      (with-redefs [handler/output-json! #(swap! outputs conj %)]
        (sut/always true "emission-count-pass-test" {})
        (is (= 1 (count @outputs)))
        (sut/always true "emission-count-pass-test" {})
        (is (= 1 (count @outputs))))))
  (testing "first fail emits exactly once; further fails are silent"
    (let [outputs (atom [])]
      (with-redefs [handler/output-json! #(swap! outputs conj %)]
        (sut/always false "emission-count-fail-test" {})
        (is (= 1 (count @outputs)))
        (sut/always false "emission-count-fail-test" {})
        (is (= 1 (count @outputs)))))))

;; --- invariant.UniqueAssertionId ---

(deftest invariant-unique-assertion-id
  (testing "distinct messages create separate tracker entries"
    (with-redefs [handler/output-json! (constantly nil)]
      (sut/always true "uniq-msg-1" {})
      (sut/always true "uniq-msg-2" {}))
    (let [t @(var-get #'core/tracker)]
      (is (contains? t "uniq-msg-1"))
      (is (contains? t "uniq-msg-2"))
      (is (= 2 (count (select-keys t ["uniq-msg-1" "uniq-msg-2"]))))))
  (testing "repeated calls with the same message update a single tracker entry"
    (with-redefs [handler/output-json! (constantly nil)]
      (sut/always true "shared-msg-test" {})
      (sut/always true "shared-msg-test" {}))
    (let [t @(var-get #'core/tracker)]
      (is (= 1 (count (filter #(= "shared-msg-test" %) (keys t)))))
      (is (= 2 (:pass-count (get t "shared-msg-test")))))))

;; --- invariant.AlwaysWithFailureIsViolated ---

(deftest invariant-always-with-failure-is-violated
  (testing
    "always assertion emits output (violation) on first failing evaluation"
    (let [outputs (atom [])]
      (with-redefs [handler/output-json! #(swap! outputs conj %)]
        (sut/always false "violated-inv-test" {}))
      (is (= 1 (count @outputs)))
      (is (false? (get-in (first @outputs) [:antithesis_assert :condition])))))
  (testing "always-or-unreachable also emits on failure"
    (let [outputs (atom [])]
      (with-redefs [handler/output-json! #(swap! outputs conj %)]
        (sut/always-or-unreachable false "aou-violated-inv-test" {}))
      (is (= 1 (count @outputs)))
      (is (false? (get-in (first @outputs) [:antithesis_assert :condition]))))))

;; --- derived.Assertion.has_failed ---

(deftest derived-has-failed
  (testing "fail_count > 0 after a failing evaluation"
    (with-redefs [handler/output-json! (constantly nil)]
      (sut/always false "has-failed-test" {}))
    (let [state (get @(var-get #'core/tracker) "has-failed-test")]
      (is (pos? (:fail-count state)))))
  (testing "fail_count = 0 when only passing evaluations have occurred"
    (with-redefs [handler/output-json! (constantly nil)]
      (sut/always true "not-failed-test" {}))
    (let [state (get @(var-get #'core/tracker) "not-failed-test")]
      (is (zero? (:fail-count state))))))

;; --- derived.Assertion.is_violated ---

(deftest derived-is-violated
  (testing "always assertion emits output on first failing evaluation"
    (let [outputs (atom [])]
      (with-redefs [handler/output-json! #(swap! outputs conj %)]
        (sut/always false "is-violated-test" {}))
      (is (= 1 (count @outputs)))))
  (testing
    "subsequent failing evaluations do not emit additional violation output"
    (let [outputs (atom [])]
      (with-redefs [handler/output-json! #(swap! outputs conj %)]
        (sut/always false "is-violated-dedup-test" {})
        (sut/always false "is-violated-dedup-test" {}))
      (is (= 1 (count @outputs))))))
