(ns antithesis.handler.interface
  (:require [antithesis.handler.core :as core]))

(defn output-json!
  "Serialize m to JSON and route to the selected output handler."
  {:malli/schema [:=> [:cat :map] :nil]}
  [m]
  (core/output-json! m))

(defn rand-long
  "Return a random long from the selected handler's random source."
  {:malli/schema [:=> [:cat] :int]}
  []
  (core/rand-long))
