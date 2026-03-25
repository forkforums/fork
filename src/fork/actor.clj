(ns fork.actor)

(defn forum-actor [base-url forum]
  {:id (str base-url "/actor/" forum)
   :type "Group"
   :preferredUsername forum
   :inbox (str base-url "/inbox")
   :outbox (str base-url "/outbox/" forum)})
