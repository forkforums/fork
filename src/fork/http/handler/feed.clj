(ns fork.http.handler.feed
  (:require [fork.http.resp :as resp]
            [fork.store :as store]))

(defn feed-handler [_]
  (resp/json-response {:posts (store/feed-posts)}))