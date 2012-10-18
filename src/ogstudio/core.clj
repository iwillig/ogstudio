(ns ogstudio.core
  (:import
   [java.io File]
   [java.util Date])
  (:use
   [geoscript.style]
   [geoscript.workspace])
  (:require
   [clj-yaml.core :as yaml]))


(def catalog
  (atom
   {:path nil
    :last-modified nil
    :tables {}
    :datastores {}
    :styles {}
    :maps {}}))

(declare load-catalog)

(defn get-resource [resource rs-name]
  (get (resource @catalog) (keyword rs-name)))

(defn get-datastore [ds-name]
  (get-resource :datastores ds-name))

(defn get-table [tb-name]
  (get-resource :tables tb-name))

(defn get-map [map-name]
  (get-resource :maps map-name))

(defn get-style [s-name]
  (get-resource :styles s-name))

(defn add-resource! [resource resource-info]
  (swap! catalog update-in [resource] assoc (keyword (:name resource-info)) resource-info))

(defmulti load-datastore (fn [x] (keyword (:type x))))

(defmethod load-datastore :postgis
  [{:keys [port host user passwd name]
    :or {port "5432" host "localhost" user "postgres" passwd ""}}]
  (make-datastore
   {:port port
    :host host
    :user user
    :passwd passwd
    :dbtype "postgis"
    :database name}))

(defmethod load-datastore :shapefile
  [params]
  (shape :path (:path params)))

(defn add-datastore! [datastore-info]
  (add-resource! :datastores (assoc datastore-info :gt (load-datastore datastore-info))))


(defn add-table! [table-info]
  (let [fs (.getFeatureSource (:gt (get-datastore (:datastore table-info))) (:name table-info))]
    (add-resource! :tables (assoc table-info :gt fs))))


(defmulti load-style (fn [style-info] (keyword (:type style-info))))

(defmethod load-style :yaml [style-info]
  (make-style
   (if (:body style-info)
     (:body style-info)
     (yaml/parse-string (slurp (:path style-info))))))

(defn style-last-modified [style-info]
  (when-let [path  (:path style-info)]
    (.lastModified (File. path))))

(defn add-style! [style-info]
  (add-resource!
   :styles (assoc style-info
             :last-modified (style-last-modified style-info)
             :gt (load-style style-info))))

(defn add-map! [map-info]
  (add-resource! :maps map-info))


(defn watch [rate func]
  (future
    (while true
      (Thread/sleep rate)
      (try 
        (func)
        (catch Exception e (println e))))))

(defn is-modified
  ;; if current is not equal to the last modified
  [old]
  (not= (:last-modified  old) (.lastModified (File. (:path old)))))

(defn watch-catalog []
  (watch
   100
   (fn []
     (when (is-modified @catalog)
       (println "Reloading main catalog object")
       (load-catalog (:path @catalog))))))

(defn watch-styles []
  (watch
   100
   (fn []
     (doseq [[style-name style] (:styles @catalog)]
       (when-not (:body style)
         (when (is-modified style)
           (println "Updating style" style-name)
           (add-style! style)))))))


(defn load-catalog [path]
  (let [file (File. path)
        {:keys [datastores tables styles maps]} (yaml/parse-string (slurp file))]
    (swap! catalog assoc :path (.getAbsolutePath file) :last-modified (.lastModified file))

    (doseq [datastore datastores]
      (println "Loading datastore " (:name datastore))
      (add-datastore! datastore))

    (doseq [table tables]
      (println "Loading table     " (:name table))
      (add-table! table))

    (doseq [style styles]
      (println "Loading style     " (:name style))
      (add-style! style))

    (doseq [m maps]
      (println "Loading map       " (:name m))
      (add-map! m))
    (println "----------------------------------------")))

(defn load-watchers []
  (watch-catalog)
  (watch-styles))

(defn reset-catalog! []
  (reset! catalog {}))

(defn load-default-catalog []
  (load-catalog "./osm.yml"))