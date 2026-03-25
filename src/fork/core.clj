(ns fork.core
  (:require [clojure.string :as str]
            [fork.federation :as federation]
            [fork.http.handler.outbox :as outbox]
            [fork.http.handler.actor :as actor]
            [fork.http.handler.feed :as feed]
            [fork.http.handler.forum :as forum]
            [fork.http.handler.wellknown :as wellknown]
            [fork.http.resp :as resp]
            [fork.store :as store]
            [reitit.ring :as ring]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]))

(defn configure-seeds! []
  (when-let [configured (System/getenv "FORK_SEEDS")]
    (doseq [seed (remove str/blank? (map str/trim (str/split configured #",")))]
      (store/add-seed! (str/replace seed #"/$" "")))))

(def app
  (-> (ring/ring-handler
       (ring/router
        [["/" {:get (fn [_]
                      (resp/json-response
                       {:service "fork"
                        :status "ok"}))}]
         ["/forum" {:post forum/create-forum-handler}]
         ["/post/:forum" {:post forum/create-post-handler}]
         ["/forum/:forum/posts" {:get forum/list-posts-handler}]
         ["/feed" {:get feed/feed-handler}]
         ["/subscribe" {:post forum/subscribe-handler}]
         ["/actor/:forum" {:get actor/forum-handler}]
         ["/outbox/:forum" {:get outbox/forum-handler}]
         ["/.well-known/webfinger" {:get wellknown/webfinger-handler}]])
       (ring/create-default-handler
        {:not-found resp/not-found-response}))
      wrap-keyword-params
      wrap-params))

(defn env-port []
  (or (some-> (System/getenv "FORK_PORT") Integer/parseInt)
      5000))

(defn env-host []
  (or (System/getenv "FORK_HOST")
      "0.0.0.0"))

(defn -main [& _]
  (let [port (env-port)
        host (env-host)]
    (configure-seeds!)
    (federation/start-worker!)
    (println (str "Starting server on http://" host ":" port))
    (run-jetty app {:port port :host host :join? false})))