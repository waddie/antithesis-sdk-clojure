(ns antithesis.assert.interface
  (:require [antithesis.assert.core :as core]))

(defmacro always
  "Assert condition is true every time and is reached at least once."
  [condition message details]
  `(core/assert! {:assert-type  "always"
                  :column       ~(:column (meta &form))
                  :display-type "Always"
                  :file         ~*file*
                  :hit          true
                  :line         ~(:line (meta &form))
                  :must-hit     true
                  :ns-name      ~(str *ns*)}
                 ~condition
                 ~message
                 ~details))

(defmacro always-or-unreachable
  "Assert condition is true every time, but the property passes if never reached."
  [condition message details]
  `(core/assert! {:assert-type  "always"
                  :column       ~(:column (meta &form))
                  :display-type "AlwaysOrUnreachable"
                  :file         ~*file*
                  :hit          true
                  :line         ~(:line (meta &form))
                  :must-hit     false
                  :ns-name      ~(str *ns*)}
                 ~condition
                 ~message
                 ~details))

(defmacro sometimes
  "Assert condition is true at least once."
  [condition message details]
  `(core/assert! {:assert-type  "sometimes"
                  :column       ~(:column (meta &form))
                  :display-type "Sometimes"
                  :file         ~*file*
                  :hit          true
                  :line         ~(:line (meta &form))
                  :must-hit     true
                  :ns-name      ~(str *ns*)}
                 ~condition
                 ~message
                 ~details))

(defmacro reachable
  "Assert that this line of code is reached at least once."
  [message details]
  `(core/assert! {:assert-type  "reachability"
                  :column       ~(:column (meta &form))
                  :display-type "Reachable"
                  :file         ~*file*
                  :hit          true
                  :line         ~(:line (meta &form))
                  :must-hit     true
                  :ns-name      ~(str *ns*)}
                 true
                 ~message
                 ~details))

(defmacro unreachable
  "Assert that this line of code is never reached."
  [message details]
  `(core/assert! {:assert-type  "reachability"
                  :column       ~(:column (meta &form))
                  :display-type "Unreachable"
                  :file         ~*file*
                  :hit          true
                  :line         ~(:line (meta &form))
                  :must-hit     false
                  :ns-name      ~(str *ns*)}
                 false
                 ~message
                 ~details))

(defmacro always-greater-than
  "Assert left > right always, with numeric guidance."
  [left right message details]
  `(let [l# (double ~left)
         r# (double ~right)
         d# (merge ~details
                   {:left  l#
                    :right r#})]
     (core/assert! {:assert-type  "always"
                    :column       ~(:column (meta &form))
                    :display-type "Always"
                    :file         ~*file*
                    :hit          true
                    :line         ~(:line (meta &form))
                    :must-hit     true
                    :ns-name      ~(str *ns*)}
                   (> l# r#)
                   ~message
                   d#)
     (core/guidance! {:column        ~(:column (meta &form))
                      :file          ~*file*
                      :guidance-type "numeric"
                      :hit           true
                      :line          ~(:line (meta &form))
                      :maximize      false
                      :ns-name       ~(str *ns*)}
                     {:gap   (- l# r#)
                      :left  l#
                      :right r#}
                     ~message)))

(defmacro always-greater-than-or-equal-to
  "Assert left >= right always, with numeric guidance."
  [left right message details]
  `(let [l# (double ~left)
         r# (double ~right)
         d# (merge ~details
                   {:left  l#
                    :right r#})]
     (core/assert! {:assert-type  "always"
                    :column       ~(:column (meta &form))
                    :display-type "Always"
                    :file         ~*file*
                    :hit          true
                    :line         ~(:line (meta &form))
                    :must-hit     true
                    :ns-name      ~(str *ns*)}
                   (>= l# r#)
                   ~message
                   d#)
     (core/guidance! {:column        ~(:column (meta &form))
                      :file          ~*file*
                      :guidance-type "numeric"
                      :hit           true
                      :line          ~(:line (meta &form))
                      :maximize      false
                      :ns-name       ~(str *ns*)}
                     {:gap   (- l# r#)
                      :left  l#
                      :right r#}
                     ~message)))

(defmacro always-less-than
  "Assert left < right always, with numeric guidance."
  [left right message details]
  `(let [l# (double ~left)
         r# (double ~right)
         d# (merge ~details
                   {:left  l#
                    :right r#})]
     (core/assert! {:assert-type  "always"
                    :column       ~(:column (meta &form))
                    :display-type "Always"
                    :file         ~*file*
                    :hit          true
                    :line         ~(:line (meta &form))
                    :must-hit     true
                    :ns-name      ~(str *ns*)}
                   (< l# r#)
                   ~message
                   d#)
     (core/guidance! {:column        ~(:column (meta &form))
                      :file          ~*file*
                      :guidance-type "numeric"
                      :hit           true
                      :line          ~(:line (meta &form))
                      :maximize      true
                      :ns-name       ~(str *ns*)}
                     {:gap   (- l# r#)
                      :left  l#
                      :right r#}
                     ~message)))

(defmacro always-less-than-or-equal-to
  "Assert left <= right always, with numeric guidance."
  [left right message details]
  `(let [l# (double ~left)
         r# (double ~right)
         d# (merge ~details
                   {:left  l#
                    :right r#})]
     (core/assert! {:assert-type  "always"
                    :column       ~(:column (meta &form))
                    :display-type "Always"
                    :file         ~*file*
                    :hit          true
                    :line         ~(:line (meta &form))
                    :must-hit     true
                    :ns-name      ~(str *ns*)}
                   (<= l# r#)
                   ~message
                   d#)
     (core/guidance! {:column        ~(:column (meta &form))
                      :file          ~*file*
                      :guidance-type "numeric"
                      :hit           true
                      :line          ~(:line (meta &form))
                      :maximize      true
                      :ns-name       ~(str *ns*)}
                     {:gap   (- l# r#)
                      :left  l#
                      :right r#}
                     ~message)))

(defmacro sometimes-greater-than
  "Assert left > right at least once, with numeric guidance."
  [left right message details]
  `(let [l# (double ~left)
         r# (double ~right)
         d# (merge ~details
                   {:left  l#
                    :right r#})]
     (core/assert! {:assert-type  "sometimes"
                    :column       ~(:column (meta &form))
                    :display-type "Sometimes"
                    :file         ~*file*
                    :hit          true
                    :line         ~(:line (meta &form))
                    :must-hit     true
                    :ns-name      ~(str *ns*)}
                   (> l# r#)
                   ~message
                   d#)
     (core/guidance! {:column        ~(:column (meta &form))
                      :file          ~*file*
                      :guidance-type "numeric"
                      :hit           true
                      :line          ~(:line (meta &form))
                      :maximize      false
                      :ns-name       ~(str *ns*)}
                     {:gap   (- l# r#)
                      :left  l#
                      :right r#}
                     ~message)))

(defmacro sometimes-greater-than-or-equal-to
  "Assert left >= right at least once, with numeric guidance."
  [left right message details]
  `(let [l# (double ~left)
         r# (double ~right)
         d# (merge ~details
                   {:left  l#
                    :right r#})]
     (core/assert! {:assert-type  "sometimes"
                    :column       ~(:column (meta &form))
                    :display-type "Sometimes"
                    :file         ~*file*
                    :hit          true
                    :line         ~(:line (meta &form))
                    :must-hit     true
                    :ns-name      ~(str *ns*)}
                   (>= l# r#)
                   ~message
                   d#)
     (core/guidance! {:column        ~(:column (meta &form))
                      :file          ~*file*
                      :guidance-type "numeric"
                      :hit           true
                      :line          ~(:line (meta &form))
                      :maximize      false
                      :ns-name       ~(str *ns*)}
                     {:gap   (- l# r#)
                      :left  l#
                      :right r#}
                     ~message)))

(defmacro sometimes-less-than
  "Assert left < right at least once, with numeric guidance."
  [left right message details]
  `(let [l# (double ~left)
         r# (double ~right)
         d# (merge ~details
                   {:left  l#
                    :right r#})]
     (core/assert! {:assert-type  "sometimes"
                    :column       ~(:column (meta &form))
                    :display-type "Sometimes"
                    :file         ~*file*
                    :hit          true
                    :line         ~(:line (meta &form))
                    :must-hit     true
                    :ns-name      ~(str *ns*)}
                   (< l# r#)
                   ~message
                   d#)
     (core/guidance! {:column        ~(:column (meta &form))
                      :file          ~*file*
                      :guidance-type "numeric"
                      :hit           true
                      :line          ~(:line (meta &form))
                      :maximize      true
                      :ns-name       ~(str *ns*)}
                     {:gap   (- l# r#)
                      :left  l#
                      :right r#}
                     ~message)))

(defmacro sometimes-less-than-or-equal-to
  "Assert left <= right at least once, with numeric guidance."
  [left right message details]
  `(let [l# (double ~left)
         r# (double ~right)
         d# (merge ~details
                   {:left  l#
                    :right r#})]
     (core/assert! {:assert-type  "sometimes"
                    :column       ~(:column (meta &form))
                    :display-type "Sometimes"
                    :file         ~*file*
                    :hit          true
                    :line         ~(:line (meta &form))
                    :must-hit     true
                    :ns-name      ~(str *ns*)}
                   (<= l# r#)
                   ~message
                   d#)
     (core/guidance! {:column        ~(:column (meta &form))
                      :file          ~*file*
                      :guidance-type "numeric"
                      :hit           true
                      :line          ~(:line (meta &form))
                      :maximize      true
                      :ns-name       ~(str *ns*)}
                     {:gap   (- l# r#)
                      :left  l#
                      :right r#}
                     ~message)))

(defmacro always-some
  "Assert at least one of the conditions map values is true, always."
  [conditions message details]
  `(let [conds# ~conditions
         gdata# (into {} (map (fn [[k# v#]] [k# (boolean v#)]) conds#))
         d#     (merge ~details gdata#)]
     (core/assert! {:assert-type  "always"
                    :column       ~(:column (meta &form))
                    :display-type "Always"
                    :file         ~*file*
                    :hit          true
                    :line         ~(:line (meta &form))
                    :must-hit     true
                    :ns-name      ~(str *ns*)}
                   (some true? (vals conds#))
                   ~message
                   d#)
     (core/guidance! {:column        ~(:column (meta &form))
                      :file          ~*file*
                      :guidance-type "boolean"
                      :hit           true
                      :line          ~(:line (meta &form))
                      :maximize      false
                      :ns-name       ~(str *ns*)}
                     gdata#
                     ~message)))

(defmacro sometimes-all
  "Assert all conditions map values are true, at least once."
  [conditions message details]
  `(let [conds# ~conditions
         gdata# (into {} (map (fn [[k# v#]] [k# (boolean v#)]) conds#))
         d#     (merge ~details gdata#)]
     (core/assert! {:assert-type  "sometimes"
                    :column       ~(:column (meta &form))
                    :display-type "Sometimes"
                    :file         ~*file*
                    :hit          true
                    :line         ~(:line (meta &form))
                    :must-hit     true
                    :ns-name      ~(str *ns*)}
                   (every? true? (vals conds#))
                   ~message
                   d#)
     (core/guidance! {:column        ~(:column (meta &form))
                      :file          ~*file*
                      :guidance-type "boolean"
                      :hit           true
                      :line          ~(:line (meta &form))
                      :maximize      true
                      :ns-name       ~(str *ns*)}
                     gdata#
                     ~message)))

(defn raw-assert
  "Low-level assertion for framework adapters."
  {:malli/schema [:=>
                  [:cat :string :string :string :string :string
                   :int :int :string :boolean :string [:maybe :map]
                   :boolean :boolean]
                  :nil]}
  [assert-type display-type class-name _function-name file-name
   begin-line begin-column _id condition message details hit must-hit]
  (core/assert! {:assert-type  assert-type
                 :column       begin-column
                 :display-type display-type
                 :file         file-name
                 :hit          hit
                 :line         begin-line
                 :must-hit     must-hit
                 :ns-name      class-name}
                condition
                message
                details))

(defn raw-guidance
  "Low-level guidance for framework adapters."
  {:malli/schema [:=>
                  [:cat :string :map :boolean :string :string :string
                   :int :int :string :string :boolean]
                  :nil]}
  [guidance-type guidance-data maximize class-name _function-name file-name
   begin-line begin-column _id message hit]
  (core/guidance! {:column        begin-column
                   :file          file-name
                   :guidance-type guidance-type
                   :hit           hit
                   :line          begin-line
                   :maximize      maximize
                   :ns-name       class-name}
                  guidance-data
                  message))
