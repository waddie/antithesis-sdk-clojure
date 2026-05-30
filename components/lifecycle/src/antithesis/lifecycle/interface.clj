(ns antithesis.lifecycle.interface
  (:require [antithesis.lifecycle.core :as core]))

(defn setup-complete!
  "Signal that setup is complete. Idempotent — only the first call emits."
  {:malli/schema [:=> [:cat [:maybe :map]] :nil]}
  [details]
  (core/setup-complete! details))

(defn send-event!
  "Emit a named structured event. Blank/whitespace name is normalized to \"anonymous\"."
  {:malli/schema [:=> [:cat :string [:maybe :map]] :nil]}
  [name details]
  (core/send-event! name details))
