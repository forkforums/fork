(ns fork.federation
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [fork.discovery :as discovery]
            [fork.store :as store]))

(defonce worker-state (atom {:running? false :future nil}))

(defn actor->outbox-url [actor-url]
  (str/replace actor-url #"/actor/" "/outbox/"))

(defn activity->post [forum activity]
  (let [object (:object activity)]
    {:id (:id object)
     :forum forum
     :content (:content object)
     :created_at (or (:published object)
                     (:published activity))
     :remote? true}))

(defn fetch-outbox-posts [forum actor-url]
  (let [response (http/get (actor->outbox-url actor-url)
                           {:accept :json
                            :as :json
                            :throw-exceptions false})]
    (when (= 200 (:status response))
      (map #(activity->post forum %)
           (get-in response [:body :orderedItems] [])))))

(defn sync-forum! [forum]
  (when-not (store/local-forum? forum)
    (when-let [actor-url (discovery/resolve-forum forum)]
      (doseq [post (fetch-outbox-posts forum actor-url)]
        (store/upsert-post! post)))))

(defn sync-subscriptions! []
  (doseq [forum (store/subscriptions)]
    (sync-forum! forum)))

(defn stop-worker! []
  (when-let [running-future (:future @worker-state)]
    (future-cancel running-future))
  (reset! worker-state {:running? false :future nil}))

(defn start-worker! []
  (when-not (:running? @worker-state)
    (let [running-future
          (future
            (while true
              (try
                (sync-subscriptions!)
                (catch Exception _))
              (Thread/sleep 10000)))]
      (reset! worker-state {:running? true :future running-future})))
  @worker-state)
