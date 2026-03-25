(ns fork.http.handler.actor
  (:require [fork.store :as store]
            [fork.http.resp :as resp]
            [fork.http.req :refer [request-base-url]]
            [fork.actor :as actor]))

(defn forum-handler [request]
  (let [forum-name (store/normalize-forum-name (get-in request [:path-params :forum]))
        forum-data (store/get-forum forum-name)]
    (if (and forum-data (:local? forum-data))
      (resp/activity-json-response (actor/forum-actor (request-base-url request) forum-name))
      (resp/not-found-response))))