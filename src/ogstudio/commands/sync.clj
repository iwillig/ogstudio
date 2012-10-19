(ns ogstudio.commands.sync
  (:require
   [ogstudio.views    :as views]
   [ogstudio.core     :as core]
   [geoscript.style   :as gstyle]
   [clj-yaml.core     :as yaml]
   [clojure.data.json :as json]
   [clj-http.client   :as client]))

(def ^:dynamic *geoserver-url* "http://localhost:8080/geoserver/rest")
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
    (:body
     (if data
       (client/request (assoc request-info :body data))
       (client/request request-info)))))

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

(comment


  )

(defn create-style [workspace style body]
  (request (url-join (conj (make-workspace-url workspace) "styles"))
           :data (clojure.java.io/input-stream  (.getBytes body))
           :content-type {"Content-type" "application/vnd.ogc.sld+xml"}
           :method :post))

(defn delete-style [& args]
  (let [[style] (format-args args)]
    (request
     (url-join [*geoserver-url* "styles" style])
     :method :delete)))


(defn -main [& args]
  ;; try deleting the workspace, if it already exists move on the next one
  (let [catalog (yaml/parse-string (slurp "nielsen-data/catalog.yml"))
        workspace "nielsen"]
    (try
      (delete-workspace workspace)
      (catch Exception e (println e)))

    (create-workspace workspace)
    
    (doseq [ds (:datastores catalog)]
      (create-datastore workspace
       {:name (:name ds)
        :connectionParameters
        {:database (:name ds)
         :user (:user ds)
         :host (:host ds)
         :port (:port ds)
         :dbtype "postgis"}}))

    (doseq [table (:tables catalog)]
      (create-feature-type workspace (:datastore table) {:name (:name table)}))

    (doseq [style (take 55 (:styles catalog))]
      (let [style-obj (core/load-style style)]
        (create-style
         workspace
         (:name style)
         (gstyle/style->sld
          (gstyle/make-sld {:name (:name style)
                            :style style-obj})))))))