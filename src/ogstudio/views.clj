(ns ogstudio.views
  (:require [ogstudio.core :as core])
  (:use
   [hiccup.element :only (javascript-tag)]
   [hiccup.form]
   [hiccup.page :only (html5 include-js include-css)]))



(defn static-url [req url]
  (format "http://%s:%s/public/%s" (:server-name req) (:server-port req) url))

(defn layout
  [request body & {:keys [title js] :or {title "Map the planet"}}]
  (html5
   [:head
    [:meta {:http-equiv "content-type" :content "text/html; charset=utf-8"}]
    [:meta {:charset "utf-8"}]
    (include-css (static-url request "bootstrap/css/bootstrap.min.css"))
    [:title title]]
   [:body
    [:div.navbar
     [:div.navbar-inner]]
    [:div.container
     body]
    js]))

(defn index [req]
  (layout req [:div (str @core/catalog)]))