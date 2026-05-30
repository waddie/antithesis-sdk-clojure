(ns antithesis.handler.interface-test
  (:require [antithesis.handler.core :as core]
            [antithesis.handler.interface :as sut]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [jsonista.core :as json])
  (:import [java.io File]))

(deftest rand-long-returns-long
  (testing "rand-long returns a long" (is (int? (sut/rand-long)))))

(deftest output-json-does-not-throw
  (testing "output-json! with a map does not throw"
    (is (nil? (sut/output-json! {:test "value"})))))

(deftest local-handler-writes-jsonl
  (testing "local handler appends JSON lines to the configured output file"
    (let [tmp  (File/createTempFile "antithesis-test" ".jsonl")
          path (.getAbsolutePath tmp)]
      (try (with-redefs [core/local-output-path (constantly path)]
             (let [h ((var-get #'core/select-handler))]
               (.output h "{\"a\":1}")
               (.output h "{\"b\":2}")
               (is (= ["{\"a\":1}" "{\"b\":2}"]
                      (str/split-lines (slurp tmp))))))
           (finally (.delete tmp))))))

(deftest local-handler-random-returns-long
  (testing "local handler random method returns a long"
    (let [tmp  (File/createTempFile "antithesis-test-random" ".jsonl")
          path (.getAbsolutePath tmp)]
      (try (with-redefs [core/local-output-path (constantly path)]
             (is (int? (.random ((var-get #'core/select-handler))))))
           (finally (.delete tmp))))))

(deftest version-info-json-structure
  (testing
    "version info emission produces antithesis_sdk JSON with required keys"
    (let [tmp  (File/createTempFile "antithesis-version-test" ".jsonl")
          path (.getAbsolutePath tmp)]
      (try (with-redefs [core/local-output-path (constantly path)]
             (let [h ((var-get #'core/select-handler))]
               ((var-get #'core/emit-version-info!) h)
               (let [parsed (json/read-value
                             (first (str/split-lines (slurp tmp)))
                             (json/object-mapper {:decode-key-fn keyword}))]
                 (is (map? (:antithesis_sdk parsed)))
                 (is (= "Clojure"
                        (get-in parsed [:antithesis_sdk :language :name])))
                 (is (string? (get-in parsed [:antithesis_sdk :sdk_version])))
                 (is (string? (get-in parsed
                                      [:antithesis_sdk :protocol_version]))))))
           (finally (.delete tmp))))))
