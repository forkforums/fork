(ns fork.http.handler.forum
  (:require [clojure.string :as str]
            [fork.store :as store]
            [fork.http.resp :as resp]
            [fork.http.req :refer [request-base-url parse-json-body request-value]]))
(defn json-request? [request]
  (some-> (get-in request [:headers "content-type"])
          str/lower-case
          (str/starts-with? "application/json")))

(defn form-request? [request]
  (not (json-request? request)))

(defn respond [request data status & [location]]
  (if (form-request? request)
    (resp/redirect-response location)
    (resp/json-response data status)))

(defn request-data [request]
  (if (json-request? request)
    (parse-json-body request)
    {:name (request-value request :name)
     :forum (request-value request :forum)
     :peer (request-value request :peer)
     :content (request-value request :content)}))

(defn create-forum-handler [request]
  (let [{:keys [name]} (request-data request)
        forum (store/normalize-forum-name name)]
    (cond
      (nil? forum)
      (resp/bad-request-response "Forum name is required")
      :else
      (let [created-forum (store/create-forum! forum (str (request-base-url request) "/actor/" forum))]
        (respond request {:forum created-forum} 201 (str "/f/" forum))))))

(defn create-post-handler [request]
  (let [forum (store/normalize-forum-name (get-in request [:path-params :forum]))
        {:keys [content]} (request-data request)]
    (cond
      (nil? content)
      (resp/bad-request-response "Post content is required")
      (not (store/local-forum? forum))
      (resp/not-found-response)
      :else
      (let [base-url (request-base-url request)
            post-id (str base-url "/f/" forum "/posts/" (random-uuid))
            post {:id post-id
                  :forum forum
                  :content content
                  :created_at (.toString (java.time.Instant/now))
                  :remote? false}]
        (store/add-post! forum post)
        (respond request {:post post} 201 (str "/f/" forum))))))

(defn list-posts-handler [request]
  (let [forum (store/normalize-forum-name (get-in request [:path-params :forum]))]
    (if (store/forum-exists? forum)
      (resp/json-response {:forum forum
                           :posts (store/get-posts forum)})
      (resp/not-found-response))))