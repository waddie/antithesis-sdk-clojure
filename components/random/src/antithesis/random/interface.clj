(ns antithesis.random.interface
  (:require [antithesis.random.core :as core]))

(defn get-random
  "Returns a platform-controlled random long. Consume immediately at the decision point."
  {:malli/schema [:=> [:cat] :int]}
  []
  (core/get-random))

(defn random-choice
  "Returns a randomly chosen element from coll, or nil if coll is empty."
  {:malli/schema [:=> [:cat [:sequential :any]] [:maybe :any]]}
  [coll]
  (core/random-choice coll))
