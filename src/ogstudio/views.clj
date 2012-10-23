(ns ogstudio.views
  (:import   
   [com.vividsolutions.jts.geom Geometry]
   [org.geotools.filter FilterFactoryImpl]
   [org.geotools.styling SLDTransformer]
   [java.awt GraphicsEnvironment]
   [org.geotools.factory CommonFactoryFinder])
  (:require [ogstudio.core :as core])
  (:use
   [geoscript.style]
   [geoscript.feature]
   [clojure.data.json :only (json-str)]
   [hiccup.element :only (javascript-tag)]
   [hiccup.form]
   [hiccup.util :only (escape-html)]
   [hiccup.page :only (html5 include-js include-css)]))


(def ^FilterFactoryImpl ff           (CommonFactoryFinder/getFilterFactory2 nil))


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


(defn show-style [{{name :name} :route-params :as req}]
  (when-let [style-info (core/get-style name)]
    (layout req [:div (str (:gt style-info)) [:pre (escape-html (style->sld (:gt style-info)))]])))

(defn show-map-layers [mapinfo]
  [:ul
   (for [{table :table style :style} (:layers mapinfo)]
     [:li [:h4 "Layer"]
      [:p "Table " [:a {:href (str "/tables/" table)} table]]
      [:p "Style " [:a {:href (str "/styles/" style)} style]]])])

(defn show-map [{{name :name} :route-params :as req}]
  (when-let [map-info (core/get-map name)]
    (layout
     req
     [:div.row-fluid
      [:div.span2
       [:h2 "Map : " (:name map-info)]
       [:div#mapInfo
        [:p#zoom]
        [:p#resolution]
        [:pre#bbox]
        (show-map-layers map-info)]]
      [:div#show-map.map.span10]]
     :js (list (include-js
                (static-url req "js/openlayers/OpenLayers.js")
                (static-url req "js/jquery-1.8.2.min.js")
                (static-url req "js/show-map.js"))
               (javascript-tag (format "load_main({mapInfo: %s});" (json-str map-info)))))))


(defn get-sample-features [fs]
  (let [query (doto (org.geotools.data.Query.)
                 (.setMaxFeatures 100))
        iter-feat (.iterator (.getFeatures fs query))
        sample (iterator-seq iter-feat)]
    sample))

(defn show-table [{{name :name} :route-params :as req}]
  (when-let [table-info (core/get-table name)]
    (let [fs (:gt table-info)
          features (.getFeatures fs)
          schema (get-fields (.getSchema fs))
          non-geom-schema (filter #(not (isa? (:type %) Geometry)) schema)
          sample (get-sample-features fs)
          bounds (.getBounds features)]
      (layout
       req
       [:div
        [:h3 "Layer name: "    (.getName fs)]
        [:p "Feature count: "  (.size features)]
        [:p "Projection: "     (.getCoordinateReferenceSystem bounds)]
        [:p "Bounding box: "   bounds]
        [:table.table.table-striped.table-bordered
         [:thead [:tr
                  [:th "Field Name"]
                  [:th "Field Type"]]]
           [:tbody
            (for [{name :name type :type} schema]
              [:tr
               [:td name]
               [:td type]])]]
        [:table.table.table-bordered.table-striped
         [:thead [:tr (for [{name :name type :type} non-geom-schema] [:th name])]]
         (for [feature sample]
           (do
             [:tr
              (for [{name :name type :type} non-geom-schema]
                [:td (.getAttribute feature name)])]))]]))))



(defn index [req]
  (let [{:keys [tables maps datastores styles]} @core/catalog
        graphics (GraphicsEnvironment/getLocalGraphicsEnvironment)]
    (layout
     req
     [:div
      [:ul
       [:li [:a {:href "#maps"}       "Maps"]]
       [:li [:a {:href "#datastores"} "Datastores"]]
       [:li [:a {:href "#tables"}     "Tables"]]
       [:li [:a {:href "#styles"}     "Styles"]]
       [:li [:a {:href "#fonts"}      "Fonts"]]
       ]
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
          [:li [:a {:href (str "/tables/" (name table-name))} (name table-name)]])]]
      [:div [:h2 "Styles"]
       [:a {:id "styles"}]
       [:ol
        (for [[style-name style] styles]
          [:li [:a {:href (str "/styles/" (name style-name))} (name style-name)]])]]
      [:div [:h2 "Fonts"]
       [:a {:id "fonts"}]
       (for [font (.getAvailableFontFamilyNames graphics)]
         [:p font]
         )
       ] 
      ])))

