(ns antithesis.handler.core
  (:require [jsonista.core :as json])
  (:import [com.antithesis.ffi.internal FfiHandler FfiWrapperJNI OutputHandler]
           [java.io File FileWriter]
           [java.util Random]))

(def ^:private sdk-version "1.5.0")
(def ^:private protocol-version "1.1.0")

(defn- ffi-available?
  "Return true if the Antithesis JNI library is on the classpath."
  []
  (try FfiWrapperJNI/LOAD_LIBRARY_MARKER (catch Throwable _ false)))

(defn- local-output-path
  "Return the ANTITHESIS_SDK_LOCAL_OUTPUT env var value, or nil."
  []
  (some-> (System/getenv "ANTITHESIS_SDK_LOCAL_OUTPUT")
          not-empty))

(defn- select-handler
  "Build an OutputHandler: FFI if available, local file if env var set, otherwise no-op."
  ^OutputHandler []
  (cond (ffi-available?) (FfiHandler.)
        (local-output-path) (let [path (local-output-path)
                                  f    (File. ^String path)]
                              (binding [*out* *err*]
                                (println (str
                                          "Assertion output will be sent to: \""
                                          (.getAbsolutePath f)
                                          "\"")))
                              (reify
                               OutputHandler
                                 (output [_ value]
                                   (try (with-open [w (FileWriter. f true)]
                                          (.write w ^String value)
                                          (.write w "\n")
                                          (.flush w))
                                        (catch Exception _)))
                                 (random [_] (.nextLong (Random.)))))
        :else (reify
               OutputHandler
                 (output [_ _] nil)
                 (random [_] (.nextLong (Random.))))))

(defn- emit-version-info!
  "Write SDK and protocol version metadata to h."
  [^OutputHandler h]
  (.output h
           (json/write-value-as-string
            {:antithesis_sdk {:language         {:name    "Clojure"
                                                 :version (clojure-version)}
                              :protocol_version protocol-version
                              :sdk_version      sdk-version}})))

(defonce ^:private handler
  (delay (let [^OutputHandler h (select-handler)]
           (emit-version-info! h)
           h)))

(defn output-json!
  "Serialize m to JSON and write to the active handler."
  {:malli/schema [:=> [:cat :map] :nil]}
  [m]
  (.output ^OutputHandler @handler (json/write-value-as-string m)))

(defn rand-long
  "Return a random long from the active handler."
  {:malli/schema [:=> [:cat] :int]}
  []
  (.random ^OutputHandler @handler))
