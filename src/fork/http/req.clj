(ns fork.http.req
  (:require [cheshire.core :as json]
            [clojure.string :as str]))

(defn request-base-url [request]
  (or (System/getenv "FORK_BASE_URL")
      (let [scheme (name (:scheme request))
            host (or (get-in request [:headers "host"])
                     (str (:server-name request)
                          (when-not (#{80 443} (:server-port request))
                            (str ":" (:server-port request)))))]
        (str scheme "://" host))))

(defn parse-json-body [request]
  (when-let [body (:body request)]
    (let [payload (slurp body)]
      (when (seq (str/trim payload))
        (json/parse-string payload true)))))

(defn request-value [request key]
  (or (get-in request [:params key])
      (get-in request [:params (name key)])))