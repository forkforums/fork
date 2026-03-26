(ns fork.http.handler.ui
  (:require [fork.http.req :refer [request-base-url]]
            [fork.http.resp :as resp]
            [fork.store :as store]
            [hiccup2.core :as h]))

(def styles
  "body{font-family:Inter,system-ui,sans-serif;background:#0b1020;color:#e5e7eb;margin:0;}main{max-width:1100px;margin:0 auto;padding:32px 20px 48px;}a{color:#7dd3fc;text-decoration:none;}a:hover{text-decoration:underline;}h1,h2,h3{margin:0 0 12px;}p{color:#94a3b8;margin:0;}section{background:#111827;border:1px solid #1f2937;border-radius:16px;padding:18px;margin:0 0 18px;} .grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:18px;} .stack{display:grid;gap:12px;}form{display:grid;gap:10px;}input,textarea{width:100%;box-sizing:border-box;border:1px solid #334155;background:#020617;color:#e5e7eb;border-radius:10px;padding:10px 12px;}textarea{min-height:120px;resize:vertical;}button{background:#2563eb;color:white;border:none;border-radius:10px;padding:10px 14px;font-weight:600;cursor:pointer;}button:hover{background:#1d4ed8;}ul{list-style:none;padding:0;margin:0;display:grid;gap:10px;}li{border:1px solid #1f2937;border-radius:12px;padding:12px;background:#0f172a;} .meta{font-size:12px;color:#94a3b8;} .pill{display:inline-block;font-size:12px;border-radius:999px;padding:3px 8px;background:#1e293b;color:#cbd5e1;margin-left:8px;} .empty{color:#64748b;font-style:italic;} .topbar{display:flex;justify-content:space-between;align-items:flex-end;gap:16px;margin-bottom:20px;} .small{font-size:14px;color:#94a3b8;} .split{display:grid;grid-template-columns:2fr 1fr;gap:18px;} @media(max-width:860px){.split{grid-template-columns:1fr;}}")

(defn page [title & body]
  (str
   (h/html
    [:html
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:title title]
      [:style styles]]
     [:body (into [:main] body)]])))

(defn list-view [items]
  (into [:ul] items))

(defn local-forums []
  (->> (store/list-forums)
       (filter :local?)
       (sort-by :name)
       vec))

(defn feed-item [post]
  [:li
   [:div
    [:a {:href (str "/f/" (:forum post))} (str "/f/" (:forum post))]
    (when (:remote? post)
      [:span.pill "remote"])]
   [:p (:content post)]
   [:div.meta (:created_at post)]])

(defn forum-item [forum]
  [:li
   [:a {:href (str "/f/" (:name forum))} (str "/f/" (:name forum))]])

(defn subscription-item [forum actor-url]
  [:li
   [:div
    [:a {:href (str "/f/" forum)} (str "/f/" forum)]
    (when actor-url
      [:span.pill "resolved"])]
   [:div.meta (or actor-url "waiting for discovery")]])

(defn dashboard-handler [request]
  (let [base-url (request-base-url request)
        forums (local-forums)
        feed (store/feed-posts)]
    (resp/response
     (page
      "Fork"
      [:div.topbar
       [:div
        [:h1 "Fork"]
        [:p "A tiny federated forum node with local forums, remote discovery, and pull-based feeds."]]
       [:div.small base-url]]
      [:div.grid
       [:section.stack
        [:h2 "Join forum"]
        [:form {:method "post" :action "/f"}
         [:input {:type "text" :name "name" :placeholder "tech" :required true}]
         [:button {:type "submit"} "Join forum"]]]
       [:section.stack
        [:h2 "Forums"]
        (if (seq forums)
          (list-view (map forum-item forums))
          [:div.empty "No forums yet."])]]
      [:section.stack
       [:h2 "Aggregated feed"]
       (if (seq feed)
         (list-view (map feed-item feed))
         [:div.empty "Your feed is empty."])]))))

(defn forum-page-handler [request]
  (let [forum (store/normalize-forum-name (get-in request [:path-params :forum]))
        forum-data (store/get-forum forum)
        posts (store/get-posts forum)
        actor-url (or (:actor-url forum-data)
                      (get (store/remote-forum-cache) forum)
                      "unknown")]
    (if-not forum-data
      (resp/not-found-response)
      (resp/response
       (page
        (str "Fork /f/" forum)
        [:div.topbar
         [:div
          [:h1 (str "/f/" forum)]
          [:p (if (:local? forum-data)
                "Local forum hosted on this node."
                "Forum view backed by local cache and federation polling.")]]
         [:a {:href "/"} "← Back"]]
        [:div.split
         [:section.stack
          [:h2 "Posts"]
          (if (seq posts)
            (list-view (map feed-item posts))
            [:div.empty "No posts yet."])]
         [:section.stack
          [:h2 "Details"]
          [:div.meta (str "Actor: " actor-url)]
          (when (:local? forum-data)
            [:form {:method "post" :action (str "/post/" forum)}
             [:textarea {:name "content" :placeholder "Write a new post..." :required true}]
             [:button {:type "submit"} "Publish post"]])]])))))