(ns antithesis.random.core
  (:require [antithesis.handler.interface :as handler]))

(defn get-random
  "Return a random long from the handler."
  {:malli/schema [:=> [:cat] :int]}
  []
  (handler/rand-long))

(defn random-choice
  "Return a randomly chosen element from coll, or nil if empty."
  {:malli/schema [:=> [:cat [:sequential :any]] [:maybe :any]]}
  [coll]
  (let [n (count coll)]
    (cond (zero? n) nil
          (= 1 n) (first coll)
          :else (nth coll
                     (int (Long/remainderUnsigned (handler/rand-long) n))))))
