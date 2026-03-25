(ns fork.http.handler.wellknown
  (:require [fork.discovery :as discovery]
            [fork.http.resp :as resp]
            [fork.forum :as forum]
            [fork.store :as store]))

(defn webfinger-handler [request]
  (let [forum-name (some-> (get-in request [:query-params "resource"])
                           discovery/parse-resource
                           store/normalize-forum-name)
        forum-data (and forum-name (store/get-forum forum-name))]
    (if (and forum-data (:local? forum-data))
      (resp/json-response
       (discovery/webfinger-response (str (forum/request-base-url request) "/actor/" forum-name)
                                     forum-name))
      (resp/not-found-response))))