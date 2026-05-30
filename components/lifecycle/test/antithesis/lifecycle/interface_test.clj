(ns antithesis.lifecycle.interface-test
  (:require [antithesis.handler.interface :as handler]
            [antithesis.lifecycle.core :as core]
            [antithesis.lifecycle.interface :as sut]
            [clojure.test :refer [deftest is testing use-fixtures]]))

(defn reset-setup! [f] (reset! (var-get #'core/setup-sent) false) (f))

(use-fixtures :each reset-setup!)

(deftest setup-complete-idempotent
  (let [outputs (atom [])]
    (with-redefs [handler/output-json!
                  #(swap! outputs conj %)]
      (sut/setup-complete! {:service "db"})
      (sut/setup-complete! {:service "db"})
      (is (= 1 (count @outputs)))
      (is (= "complete"
             (get-in (first @outputs) [:antithesis_setup :status]))))))

(deftest send-event-name-normalization
  (let [outputs (atom [])]
    (with-redefs [handler/output-json!
                  #(swap! outputs conj %)]
      (sut/send-event! "   " {})
      (is (contains? (first @outputs) :anonymous)))))

(deftest send-event-uses-name-as-key
  (let [outputs (atom [])]
    (with-redefs [handler/output-json!
                  #(swap! outputs conj %)]
      (sut/send-event! "my-event" {:x 1})
      (is (contains? (first @outputs) :my-event)))))

(deftest send-event-whitespace-only-normalized
  (testing "tab-only name is normalized to anonymous"
    (let [outputs (atom [])]
      (with-redefs [handler/output-json! #(swap! outputs conj %)]
        (sut/send-event! "\t" {})
        (is (contains? (first @outputs) :anonymous)))))
  (testing "mixed whitespace name is normalized to anonymous"
    (let [outputs (atom [])]
      (with-redefs [handler/output-json! #(swap! outputs conj %)]
        (sut/send-event! "  \t  " {})
        (is (contains? (first @outputs) :anonymous)))))
  (testing "padded name is trimmed but preserves inner content"
    (let [outputs (atom [])]
      (with-redefs [handler/output-json! #(swap! outputs conj %)]
        (sut/send-event! "  my-event  " {})
        (is (contains? (first @outputs) :my-event))))))
