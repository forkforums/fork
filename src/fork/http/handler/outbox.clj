(ns fork.http.handler.outbox
  (:require [fork.http.resp :as resp]
            [fork.http.req :refer [request-base-url]]
            [fork.outbox :as outbox]
            [fork.store :as store]))

(defn forum-handler [request]
  (let [forum-name (store/normalize-forum-name (get-in request [:path-params :forum]))]
    (if (store/local-forum? forum-name)
      (resp/activity-json-response (outbox/forum-outbox (request-base-url request) forum-name))
      (resp/not-found-response))))