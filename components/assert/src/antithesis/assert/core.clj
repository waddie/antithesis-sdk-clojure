(ns antithesis.assert.core
  (:require [antithesis.handler.interface :as handler]))

;; {:message-id {:pass-count 0 :fail-count 0}}
(defonce ^:private tracker (atom {}))

;; {:message-id {:mark value}} -- absent until first non-NaN emission
(defonce ^:private guidance-tracker (atom {}))

(defn- location-map
  "Build a location map from namespace, file, line, and column."
  [ns-name file line column]
  {:begin_column column
   :begin_line   line
   :class        ns-name
   :file         file
   ;; Compile-time capture cannot recover a function name (no runtime stack
   ;; walking). Matches Java SDK's @JsonProperty("function") key with empty
   ;; value.
   :function     ""})

(defn- track-assertion!
  "Update the assertion tracker for message and return the new state."
  {:malli/schema [:=> [:cat :string :boolean] :map]}
  [message condition]
  (swap! tracker (fn [t]
                   (if (contains? t message)
                     (update-in t
                                [message (if condition :pass-count :fail-count)]
                                inc)
                     (assoc t
                            message
                            {:fail-count (if condition 0 1)
                             :pass-count (if condition 1 0)})))))

(def ^:private AssertOpts
  [:map
   [:assert-type :string]
   [:column :int]
   [:display-type :string]
   [:file :string]
   [:hit :boolean]
   [:line :int]
   [:must-hit :boolean]
   [:ns-name :string]])

(def ^:private GuidanceOpts
  [:map
   [:column :int]
   [:file :string]
   [:guidance-type :string]
   [:hit :boolean]
   [:line :int]
   [:maximize :boolean]
   [:ns-name :string]])

(defn assert!
  "Emit an assertion JSON record, deduplicating by message."
  {:malli/schema [:=> [:cat AssertOpts :boolean :string [:maybe :map]] :nil]}
  [{:keys [assert-type display-type must-hit ns-name file line column hit]}
   condition message details]
  (let [loc  (location-map ns-name file line column)
        emit (fn []
               (handler/output-json! {:antithesis_assert
                                      {:assert_type  assert-type
                                       :condition    condition
                                       :details      (or details {})
                                       :display_type display-type
                                       :hit          hit
                                       :id           message
                                       :location     loc
                                       :message      message
                                       :must_hit     must-hit}}))]
    (if-not hit
      ;; catalog registration — always emit
      (emit)
      (let [after (track-assertion! message condition)
            info  (get after message)]
        (when (or (and condition (= 1 (:pass-count info)))
                  (and (not condition) (= 1 (:fail-count info))))
          (emit))))))

(defn- numeric-should-send?
  "Return true if a numeric guidance value for message should be emitted."
  {:malli/schema [:=> [:cat :string :boolean :double] :boolean]}
  [message maximize gap]
  (let [nan?      (Double/isNaN gap)
        [old new] (swap-vals!
                   guidance-tracker
                   (fn [t]
                     (let [mark (get-in t [message :mark])]
                       (if (and (not nan?)
                                (or (nil? mark)
                                    (if maximize (> gap mark) (< gap mark))))
                         (assoc-in t [message :mark] gap)
                         t))))]
    (or nan? (not= (get-in old [message :mark]) (get-in new [message :mark])))))

(defn guidance!
  "Emit a guidance JSON record, with numeric deduplication when hit is true."
  {:malli/schema [:=> [:cat GuidanceOpts :map :string] :nil]}
  [{:keys [guidance-type maximize ns-name file line column hit]}
   gap-or-data message]
  (let [loc  (location-map ns-name file line column)
        emit (fn []
               (handler/output-json! {:antithesis_guidance
                                      {:guidance_data gap-or-data
                                       :guidance_type guidance-type
                                       :hit           hit
                                       :id            message
                                       :location      loc
                                       :maximize      maximize
                                       :message       message}}))]
    (if-not hit
      (emit)
      (case guidance-type
        "numeric"
        (when (numeric-should-send? message maximize (:gap gap-or-data)) (emit))
        (emit)))))
