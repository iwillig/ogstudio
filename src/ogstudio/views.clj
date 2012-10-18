(ns ogstudio.views
  (:require [ogstudio.core :as core])
  (:use
   [clojure.data.json :only (json-str)]
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
    (include-css (static-url request "css/bootstrap.min.css"))
    (include-css (static-url request "css/site.css"))
    (include-css (static-url request "js/openlayers/theme/default/style.css"))
    [:title title]]
   [:body
    [:div.navbar
     [:div.navbar-inner [:a.brand {:href "/"} "OpenGeo Studio"]]]
    [:div.container-fluid
     body]
    js]))

(defn show-map [req]
  (let [map-name (:name (:route-params req))
        map-info (core/get-map map-name)]
    (when map-info
      (layout
       req
       [:div.row-fluid
        [:div.span2
         [:h2 "Map : " (:name map-info)]]
        [:div#show-map.map.span10]]
       :js (list (include-js
                  (static-url req "js/openlayers/OpenLayers.js")
                  (static-url req "js/jquery-1.8.2.min.js")
                  (static-url req "js/show-map.js"))
                 (javascript-tag (format "load_main({mapInfo: %s});" (json-str map-info))))))))

(defn index [req]
  (let [{:keys [tables maps datastores styles]} @core/catalog]
    (layout
     req
     [:div
      [:ul
       [:li [:a {:href "#maps"} "Maps"]]
       [:li [:a {:href "#datastores"} "Datastores"]]
       [:li [:a {:href "#tables"} "Tables"]]
       [:li [:a {:href "#styles"} "Styles"]]]
      [:div [:h2 "Maps"]
       [:a {:id "maps"}]
       [:ul
        (for [[map-name m] maps]
          [:li [:a {:href (str "/maps/" (name map-name))} (name  map-name)]])]]
      [:div [:h2 "Datastores"]
       [:a {:id "datastores"}]
       [:ul
        (for [[ds-name ds] datastores]
          [:li [:p (name ds-name)]])]]
      [:div [:h2 "Data tables"]
       [:a {:id "tables"}]
       [:ol
        (for [[table-name table] tables]
          [:li [:p (name table-name)]])]]
      [:div [:h2 "Styles"]
       [:a {:id "styles"}]
       [:ol
        (for [[style-name style] styles]
          [:li [:p (name style-name)]])]]])))