(ns fork.store
  (:require [clojure.string :as str]))

(defonce db
  (atom {:forums {}
         :posts {}
         :remote-forum-cache {}
         :peers #{}}))

(defn normalize-forum-name [forum]
  (some-> forum str str/trim str/lower-case not-empty))

(defn forum-exists? [forum]
  (contains? (:forums @db) forum))

(defn local-forum? [forum]
  (true? (get-in @db [:forums forum :local?])))

(defn forums []
  (vals (:forums @db)))

(defn create-forum! [forum actor-url]
  (swap! db assoc-in [:forums forum]
         {:name forum
          :local? true
          :actor-url actor-url})
  (get-in @db [:forums forum]))

(defn get-forum [forum]
  (get-in @db [:forums forum]))

(defn list-forums []
  (vals (:forums @db)))

(defn sync-remote-forum! [forum actor-urls]
  (swap! db assoc-in [:remote-forum-cache forum] actor-urls)
  actor-urls)

(defn cached-remote-forum [forum]
  (get-in @db [:remote-forum-cache forum]))

(defn remote-forum-cache []
  (:remote-forum-cache @db))

(defn add-peer! [peer-url]
  (when (and (seq peer-url) (not (contains? (:peers @db) peer-url)))
    (swap! db update :peers conj peer-url))
  (:peers @db))

(defn peers []
  (:peers @db))

(defn upsert-post! [post]
  (swap! db assoc-in [:posts (:id post)] post)
  post)

(defn add-post! [forum post]
  (upsert-post! (assoc post :forum forum)))

(defn get-posts [forum]
  (->> (:posts @db)
       vals
       (filter #(= forum (:forum %)))
       (sort-by :created_at)
       reverse
       vec))

(defn all-posts []
  (->> (:posts @db)
       vals
       (sort-by :created_at)
       reverse
       vec))

(defn feed-posts []
  (let [forums (forums)]
    (->> (all-posts)
         (filter #(contains? (set forums) (:forum %)))
         vec)))
