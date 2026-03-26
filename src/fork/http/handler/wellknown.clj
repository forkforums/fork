(ns fork.http.handler.wellknown
  (:require [fork.discovery :as discovery]
            [fork.http.resp :as resp]
            [fork.store :as store]
            [fork.http.req :refer [request-base-url]]))

(defn- webfinger-resource [request]
  (or (get-in request [:params :resource])
      (get-in request [:params "resource"])
      (get-in request [:query-params :resource])
      (get-in request [:query-params "resource"])))

(defn webfinger-handler [request]
  (let [resource (webfinger-resource request)
        forum-name (some-> resource
                           discovery/parse-resource
                           store/normalize-forum-name)
        forum-data (and forum-name (store/get-forum forum-name))]
    (cond
      (nil? resource)
      (resp/bad-request-response "resource query parameter is required")
      (and forum-data (:local? forum-data))
      (resp/json-response
       (discovery/webfinger-response (str (request-base-url request) "/actor/" forum-name)
                                     forum-name))
      :else
      (resp/not-found-response))))