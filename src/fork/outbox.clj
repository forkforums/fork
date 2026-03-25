(ns fork.outbox
  (:require [fork.store :as store]))

(defn note-object [post]
  {:id (:id post)
   :type "Note"
   :content (:content post)
   :published (:created_at post)
   :attributedTo (:forum post)})

(defn create-activity [base-url forum post]
  {:id (str (:id post) "#activity")
   :type "Create"
   :actor (str base-url "/actor/" forum)
   :object (note-object post)
   :published (:created_at post)})

(defn forum-outbox [base-url forum]
  (let [posts (store/get-posts forum)]
    {:id (str base-url "/outbox/" forum)
     :type "OrderedCollection"
     :totalItems (count posts)
     :orderedItems (mapv #(create-activity base-url forum %) posts)}))
