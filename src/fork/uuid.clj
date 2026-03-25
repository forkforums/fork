(ns fork.uuid)

(defn new-uuid []
  (str (java.util.UUID/randomUUID)))