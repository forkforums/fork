(ns fork.http.req)

(defn request-base-url [request]
  (or (System/getenv "FORK_BASE_URL")
      (let [scheme (name (:scheme request))
            host (or (get-in request [:headers "host"])
                     (str (:server-name request)
                          (when-not (#{80 443} (:server-port request))
                            (str ":" (:server-port request)))))]
        (str scheme "://" host))))