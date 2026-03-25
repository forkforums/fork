(ns fork.discovery
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [fork.store :as store]))

(defn webfinger-resource [forum]
  (str "forum:" forum))

(defn webfinger-response [actor-url forum]
  {:subject (webfinger-resource forum)
   :links [{:rel "self"
            :type "application/activity+json"
            :href actor-url}]})

(defn actor-url-from-webfinger [body]
  (some (fn [link]
          (when (= "self" (:rel link))
            (:href link)))
        (:links body)))

(defn fetch-webfinger [base-url forum]
  (let [resource (webfinger-resource forum)
        url (str (str/replace base-url #"/$" "") "/.well-known/webfinger")
        response (http/get url
                           {:query-params {:resource resource}
                            :accept :json
                            :as :json
                            :throw-exceptions false})]
    (when (= 200 (:status response))
      (actor-url-from-webfinger (:body response)))))

(defn resolve-forum [forum]
  (or (store/cached-remote-forum forum)
      (some (fn [seed]
              (when-let [actor-url (fetch-webfinger seed forum)]
                (store/add-seed! seed)
                (store/cache-remote-forum! forum actor-url)))
            (store/seeds))))

(defn parse-resource [resource]
  (when (and resource (str/starts-with? resource "forum:"))
    (subs resource (count "forum:"))))
