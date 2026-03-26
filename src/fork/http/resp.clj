(ns fork.http.resp
  (:require [cheshire.core :as json]))

(defn response [html]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body html})

(defn redirect-response
  ([location]
   (redirect-response location 303))
  ([location status]
   {:status status
    :headers {"Location" location}
    :body ""}))

(defn json-response
  ([data]
   (json-response data 200))
  ([data status]
   {:status status
    :headers {"Content-Type" "application/json"}
    :body (json/generate-string data)}))

(defn activity-json-response [data]
  {:status 200
   :headers {"Content-Type" "application/activity+json"}
   :body (json/generate-string data)})

(defn bad-request-response [message]
  {:status 400
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string {:error message})})

(defn not-found-response []
  {:status 404
   :headers {"Content-Type" "text/plain"}
   :body "404 Not Found"})

(defn conflict-response [message]
  {:status 409
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string {:error message})})