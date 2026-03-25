(ns fork.store
  (:require [clojure.string :as str]))

(defonce db
  (atom {:forums {}
         :posts {}
         :subscriptions #{}
         :remote-forum-cache {}
         :seeds #{}}))

(defn normalize-forum-name [forum]
  (some-> forum str str/trim str/lower-case not-empty))

(defn forum-exists? [forum]
  (contains? (:forums @db) forum))

(defn local-forum? [forum]
  (true? (get-in @db [:forums forum :local?])))

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

(defn cache-remote-forum! [forum actor-url]
  (swap! db assoc-in [:remote-forum-cache forum] actor-url)
  actor-url)

(defn cached-remote-forum [forum]
  (get-in @db [:remote-forum-cache forum]))

(defn remote-forum-cache []
  (:remote-forum-cache @db))

(defn add-seed! [seed-url]
  (when (seq seed-url)
    (swap! db update :seeds conj seed-url))
  (:seeds @db))

(defn seeds []
  (:seeds @db))

(defn subscribe! [forum]
  (swap! db update :subscriptions conj forum)
  (:subscriptions @db))

(defn subscriptions []
  (:subscriptions @db))

(defn subscribed? [forum]
  (contains? (:subscriptions @db) forum))

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
  (let [subs (subscriptions)]
    (->> (all-posts)
         (filter #(or (local-forum? (:forum %))
                      (contains? subs (:forum %))))
         vec)))