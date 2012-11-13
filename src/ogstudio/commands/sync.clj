(ns ogstudio.commands.sync
  (:gen-class)
  (:require
   [ogstudio.views    :as views]
   [ogstudio.core     :as core]
   [geoscript.style   :as gstyle]
   [clj-yaml.core     :as yaml]
   [clojure.data.json :as json]
   [clj-http.client   :as client]))

(def ^:dynamic *geoserver-url* "http://localhost:8080/geoserver/rest")
;; (def ^:dynamic *geoserver-url* "http://nielsen.dev.opengeo.org:8080/geoserver/rest")
(def ^:dynamic *auth* ["admin" "geoserver"])

(defn request [url & {:keys [method data content-type query-params]}]
  (let [request-info {:request-method (or method :get)
                      :url url
                      :query-params query-params
                      :as :json
                      :headers (merge
                                {"Accept" "application/json"}
                                (or content-type {"Content-type" "application/json"}))
                      :basic-auth *auth*}]
    (if data
      (client/request (assoc request-info :body data))
      (client/request request-info))))

(defn encode [s]
  (java.net.URLEncoder/encode s))

(defn url-join [parts]
  (clojure.string/join "/" parts))

(defn format-args [args]
  (map #(encode (name %)) args))

(defn make-workspace-url [& [name]]
  (let [url [*geoserver-url* "workspaces"]]
    (if name
      (conj url name)
      url)))

(defn make-datastore-url [ws & [ds]]
  (let [url (conj (make-workspace-url ws) "datastores")]
    (if ds
      (conj url ds)
      url)))

(defn make-feature-url [ws ds & [f]]
  (let [url (conj (make-datastore-url ws ds) "featuretypes")]
    (if f
      (conj url f)
      url)))

(defn make-layer-group [& [name]]
  (let [url [*geoserver-url* "layergroups"]]
    (if name
      (conj url name)
      url)))

;; workspaces
(defn get-workspaces []
  (request (url-join (make-workspace-url))))

(defn get-workspace [& args]
  (let [[name] (format-args args)]
    (request (url-join (make-workspace-url name)))))

(defn create-workspace [name]
  (request (url-join (make-workspace-url))
           :data (json/json-str {:workspace {:name name}})
           :method :post))

(defn delete-workspace [& args]
  (let [[name] (format-args args)]
    (request
     (url-join (make-workspace-url name))
     :query-params {"recurse" "true"}
     :method :delete)))

;; datastores
(defn get-datastores-by-workspace [& args]
  (let [[ws] (format-args args)]
    (request (url-join (make-datastore-url ws)))))

(defn get-datastore [& args]
  (let [[ws ds] (format-args args)
        ds (:dataStore (request (url-join (make-datastore-url ws ds))))]
    (assoc ds :features-types (request (:featureTypes ds)))))


(defn create-datastore
  [workspace datastore]
  (request (url-join (make-datastore-url workspace))
           :method :post
           :data (json/json-str {:dataStore datastore})))


(defn delete-datastore [& args]
  (let [[ws ds] (format-args args)]
    (request (url-join (make-datastore-url ws ds))
             :method :delete)))


;; featureTypes
(defn get-feature-type [& args]
  (let [[ws ds ft] (format-args args)]
    (request (url-join (make-feature-url ws ds ft)))))

(defn delete-feature-type [& args]
  (let [[ws ds ft] (format-args args)]
    (request (url-join (make-feature-url ws ds ft)) :method :delete)))

(defn create-feature-type [ws ds ft]
  (request (url-join (make-feature-url ws ds))
           :data (json/json-str {:featureType ft})
           :method :post))

;; layers

(defn get-layers []
  (request (url-join [*geoserver-url* "layers"])))

(defn get-layer [& args]
  (let [[name] (format-args args)]
    (request (url-join [*geoserver-url* "layers" name]))))

(defn delete-layer [& args]
  (let [[name] (format-args args)]
    (request (url-join [*geoserver-url* "layers" name]) :method :delete)))

(defn get-styles []
  (request (url-join [*geoserver-url* "styles"])))


(defn create-style [workspace style body]
  (request (url-join [*geoserver-url* "styles"])
           :data (clojure.java.io/input-stream  (.getBytes body))
           :query-params {"name" style}
           :content-type {"Content-type" "application/vnd.ogc.sld+xml"}
           :method :post))

(defn delete-style [& args]
  (let [[style] (format-args args)]
    (request
     (url-join [*geoserver-url* "styles" style])
     :query-params {"purge" true}
     :method :delete)))


(defn get-layer-groups []
  (request (url-join (make-layer-group))))

(defn get-layer-group [group-name]
  (request (url-join (make-layer-group (name group-name)))))

(defn delete-layer-group [group-name]
  (request (url-join (make-layer-group group-name))
           :method :delete))


(defn create-layer-group [name layers styles]
  (request (url-join (make-layer-group))
           :data
           (json/json-str
            {:layerGroup
             {:name name
              :layers {:layer layers}
              :styles {:style styles}}})
           :method :post))


(defn test-lg []
  (create-layer-group "test5" [{:name "airport_clip"}] [{:name "airport"}]))


(defn create-all-datastores [catalog]
  (doseq [ds (:datastores catalog)]
    (println "Creating datastore " (:name ds))
    (create-datastore
     (:workspace catalog)
     {:name (:name ds)
      :connectionParameters
      {:database (:name ds)
       :user   (or  (:user ds) "postgres")
       :host   (or  (:host ds) "localhost")
       :port   (or  (:port ds) 5432)
       :passwd "pass"
       :dbtype (:type ds)}})))

(defn create-all-tables [catalog]
  (doseq [table (:tables catalog)]
    (println "Creating feature type" (:name table))
    (create-feature-type (:workspace catalog) (:datastore table) {:name (:name table)})))

(defn create-all-styles [catalog]
  (doseq [style (:styles catalog)]
    (let [style-obj (core/load-style style)]

      (try
        (delete-style (:name style))
        (catch Exception e (println e)))

      (println "Creating style" (:name style))

      (create-style
       (:workspace catalog)
       (:name style)
       (gstyle/style->sld
        (gstyle/make-sld {:name (:name style)
                          :style style-obj}))))))

(defn create-all-maps [catalog]
  (doseq [m (:maps catalog)]
    (do
      (try
        (delete-layer-group (:name m))
        (catch Exception e (println e)))
      (println "Creating map" (:name m))
      (create-layer-group
       (:name m)
       (for [l (:layers m)]
         {:name  (:table l)})
       (for [l (:layers m)]
         {:name (:style l)})))))

(defn update-remote-geoserver [catalog]

  (let [workspace (:workspace catalog)]

    (try
      (delete-workspace workspace)
      (catch Exception e (println e)))

    (println "Creating workspace" workspace)

    (create-workspace workspace)
    
    (create-all-datastores catalog)
    (create-all-tables     catalog)
    (create-all-styles     catalog)

    (create-all-maps       catalog)))


(defn -main [& args]
  (update-remote-geoserver (yaml/parse-string (slurp (first args)))))