(ns fork.core
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [fork.federation :as federation]
            [fork.http.handler.outbox :as outbox]
            [fork.http.handler.actor :as actor]
            [fork.http.handler.feed :as feed]
            [fork.http.handler.forum :as forum]
            [fork.http.handler.ui :as ui]
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
      (store/add-peer! (str/replace seed #"/$" "")))))

(defn wrap-access-log [handler]
  (fn [req]
    (let [start (System/nanoTime)
          resp  (handler req)
          ms    (/ (- (System/nanoTime) start) 1e6)]
      (log/info {:method (:request-method req)
                 :uri (:uri req)
                 :status (:status resp)
                 :duration-ms ms})
      resp)))

(def app
  (-> (ring/ring-handler
       (ring/router
        [["/" {:get ui/dashboard-handler}]
         ["/health" {:get (fn [_]
                            (resp/json-response
                             {:service "fork"
                              :status "ok"}))}]
         ["/f/:forum" {:get ui/forum-page-handler}]
         ["/f" {:post forum/create-forum-handler}]
         ["/post/:forum" {:post forum/create-post-handler}]
         ["/feed" {:get feed/feed-handler}]
         ["/actor/:forum" {:get actor/forum-handler}]
         ["/outbox/:forum" {:get outbox/forum-handler}]
         ["/.well-known/webfinger" {:get wellknown/webfinger-handler}]])
       (ring/create-default-handler
        {:not-found (fn [_] (resp/not-found-response))}))
      wrap-access-log
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
    (log/infof "Starting server on http://%s:%s" host port)
    (run-jetty app {:port port :host host :join? false})))