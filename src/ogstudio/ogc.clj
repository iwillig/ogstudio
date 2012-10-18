(ns ogstudio.ogc
  (:use [compojure.core])
  (:require
   [ogstudio.core :as catalog]
   [compojure.handler :as handler]
   [compojure.route :as route])
  (:import
   [java.io ByteArrayOutputStream ByteArrayInputStream]
   [javax.imageio ImageIO]
   [org.geotools.referencing CRS]
   [java.awt Rectangle RenderingHints Color]
   [java.awt.image BufferedImage]
   [org.geotools.geometry.jts ReferencedEnvelope]
   [org.geotools.styling StyleImpl BasicLineStyle NamedStyleImpl]
   [org.geotools.renderer.lite StreamingRenderer]
   [org.geotools.map MapContent MapViewport FeatureLayer]))

(defn with-ogc-parameters
  [handler]
  (fn [req]
    (let [params (:query-params req)]
      (handler
       (assoc req :ogc-params
         (reduce (fn [r p]
                   (assoc r (keyword (.toLowerCase (p 0))) (p 1))) {}
                   params))))))

(defn parse-bounding-box
  "Parse a bounding box from a ring request"
  [params]
  (let [[minx miny maxx maxy] (map #(Double/parseDouble %) (.split (:bbox params) ","))
        proj (CRS/decode (:srs params))]
    (ReferencedEnvelope. minx maxx miny maxy proj)))


(defn build-mapcontent
  "Function to build a GeoTools map content object from a list of layers and styles"
  [layers styles]
  (let [mc (doto (MapContent. )
             (.setTitle "OGC Map"))]
    (doall
     (map-indexed
      (fn [idx layer]
        (let [style (nth styles idx)]
          (.addLayer mc (FeatureLayer. (:gt layer)
                                       (:gt  style)))))
      layers))
    mc))

(defn generate-tile-stream
  [layers-str styles-str bbox height-str width-str bg-hex]
  (let [layers (map catalog/get-table (.split layers-str ",")) ;; grab the layers from the catalog
        styles (map catalog/get-style (.split styles-str ","))
        height (Integer/parseInt height-str) ;; height and width need to be integers
        width (Integer/parseInt width-str)
        bgcolor (or bg-hex "#fffffff") ;; background color for map
        render (doto (StreamingRenderer.)
                    (.setJava2DHints
                     (RenderingHints.
                      RenderingHints/KEY_ANTIALIASING
                      RenderingHints/VALUE_ANTIALIAS_ON)))
        output-image (BufferedImage. width height BufferedImage/TYPE_INT_ARGB)
        out-stream (ByteArrayOutputStream.)
        screen-area (Rectangle. 0 0 width height)
        graphics (.createGraphics output-image)
        mapcontent (build-mapcontent layers styles)]
    (.setMapContent render mapcontent)
    (.paint render graphics screen-area bbox)
    (ImageIO/write output-image "png" out-stream)
    (.dispose mapcontent)
    (ByteArrayInputStream. (.toByteArray out-stream))))


(defn parse-get-map
  "Function to pull the parameters out of ring request map"
  [req]
  (let [{:keys [version request layers styles height width bgcolor]} (:ogc-params req)
        bbox (parse-bounding-box (:ogc-params req))]
    (generate-tile-stream layers styles bbox height width bgcolor)))

(defn ogc-handler
  "Main function to dispatch the core OGC services. Each OGC service
includes a request key, we dispatch on that value calling the correct function"
  [req]
  (let [params (:ogc-params req)]
    (case (:request params)
      "GetMap" (parse-get-map req)
      :else (str "We do not support your request type"))))


(defroutes service-routes
  (ANY "/" [] ogc-handler))