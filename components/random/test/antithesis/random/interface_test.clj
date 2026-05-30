(ns antithesis.random.interface-test
  (:require [antithesis.random.interface :as sut]
            [clojure.test :refer [deftest is]]))

(deftest get-random-returns-long (is (int? (sut/get-random))))

(deftest random-choice-empty-coll (is (nil? (sut/random-choice []))))

(deftest random-choice-single-element
  (is (= :only (sut/random-choice [:only]))))

(deftest random-choice-within-bounds
  (let [coll [:a :b :c :d :e]]
    (dotimes [_ 50]
      (is (contains? (set coll) (sut/random-choice coll))))))
