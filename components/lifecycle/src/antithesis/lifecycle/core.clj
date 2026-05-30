(ns antithesis.lifecycle.core
  (:require [antithesis.handler.interface :as handler]))

(defonce ^:private setup-sent (atom false))

(defn setup-complete!
  "Emit setup-complete exactly once; subsequent calls are no-ops."
  {:malli/schema [:=> [:cat [:maybe :map]] :nil]}
  [details]
  (when (compare-and-set! setup-sent false true)
    (handler/output-json! {:antithesis_setup {:details (or details {})
                                              :status  "complete"}})))

(defn send-event!
  "Emit a named event, normalizing blank names to \"anonymous\"."
  {:malli/schema [:=> [:cat :string [:maybe :map]] :nil]}
  [name details]
  (let [event-name (let [t (-> name
                               str
                               .trim)]
                     (if (empty? t) "anonymous" t))]
    (handler/output-json! {(keyword event-name) (or details {})})))
