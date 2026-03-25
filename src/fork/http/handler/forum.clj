(ns fork.http.handler.forum
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [fork.store :as store]
            [fork.http.resp :as resp]
            [fork.http.req :refer [request-base-url]]))

(defn parse-json-body [request]
  (when-let [body (:body request)]
    (let [payload (slurp body)]
      (when (seq (str/trim payload))
        (json/parse-string payload true)))))

(defn create-forum-handler [request]
  (let [{:keys [name]} (parse-json-body request)
        forum (store/normalize-forum-name name)]
    (cond
      (nil? forum)
      (resp/bad-request-response "Forum name is required")

      (or (store/forum-exists? forum)
          (store/cached-remote-forum forum))
      (resp/conflict-response "Forum already exists")

      :else
      (resp/json-response
       {:forum (store/create-forum! forum (str (request-base-url request) "/actor/" forum))}
       201))))

(defn create-post-handler [request]
  (let [forum (store/normalize-forum-name (get-in request [:path-params :forum]))
        {:keys [content]} (parse-json-body request)]
    (cond
      (nil? content)
      (resp/bad-request-response "Post content is required")
      (not (store/local-forum? forum))
      (resp/not-found-response)
      :else
      (let [base-url (request-base-url request)
            post-id (str base-url "/forum/" forum "/posts/" (random-uuid))
            post {:id post-id
                  :forum forum
                  :content content
                  :created_at (.toString (java.time.Instant/now))
                  :remote? false}]
        (store/add-post! forum post)
        (resp/json-response {:post post} 201)))))

(defn list-posts-handler [request]
  (let [forum (store/normalize-forum-name (get-in request [:path-params :forum]))]
    (if (or (store/forum-exists? forum)
            (store/subscribed? forum))
      (resp/json-response {:forum forum
                           :posts (store/get-posts forum)})
      (resp/not-found-response))))

(defn subscribe-handler [request]
  (let [{:keys [forum seed]} (parse-json-body request)
        normalized-forum (store/normalize-forum-name forum)]
    (cond
      (nil? normalized-forum)
      (resp/bad-request-response "Forum is required")
      :else
      (do
        (when (seq seed)
          (store/add-seed! (str/replace seed #"/$" "")))
        (store/subscribe! normalized-forum)
        (resp/json-response {:forum normalized-forum
                             :subscribed true
                             :seed seed}
                            201)))))
