(ns fork.discovery
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [fork.store :as store]))

(defn webfinger-response [actor-url forum]
  (let [link {:rel "self"
              :type "application/activity+json"
              :href actor-url}
        remote-links (map #(hash-map :rel "remote" :type "application/activity+json" :href %)
                          (store/cached-remote-forum forum))]
    {:subject (str "forum:" forum)
     :links (concat [link] remote-links)}))

(defn actor-urls-from-webfinger [body]
  (->> (:links body)
       (keep :href)
       distinct
       vec))

(defn webfinger-url [base-url forum]
  (let [resource (str "forum:" forum)
        encoded-resource (java.net.URLEncoder/encode resource "UTF-8")]
    (str (str/replace base-url #"/$" "")
         "/.well-known/webfinger?resource="
         encoded-resource)))

(defn fetch-webfingers [base-url forum]
  (let [url (webfinger-url base-url forum)
        response (http/get url
                           {:headers {"accept" "application/json"}
                            :as :json
                            :throw-exceptions false})]
    (when (= 200 (:status response))
      (actor-urls-from-webfinger (:body response)))))

(defn resolve-forum [forum]
  (or (store/cached-remote-forum forum)
      (->> (store/peers)
           (keep (fn [peer]
                   (when-let [actor-urls (fetch-webfingers peer forum)]
                     (store/add-peer! peer)
                     (store/sync-remote-forum! forum actor-urls))))
           (concat)
           (distinct))))

(defn parse-resource [resource]
  (when (and resource (str/starts-with? resource "forum:"))
    (subs resource (count "forum:"))))
