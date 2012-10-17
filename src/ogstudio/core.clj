(ns ogstudio.core
  (:use
   [geoscript.style]
   [geoscript.workspace])
  (:require
   [clj-yaml.core :as yaml]))


(def catalog (atom {:tables {} :datastores {}  :styles {} :maps {}}))

(defn add-resource! [resource resource-info]
  (swap! catalog update-in [resource] assoc (keyword (:name resource-info)) resource-info))

(defmulti load-datastore (fn [x] (keyword (:type x))))

(defmethod load-datastore :postgis
  [params]
  (postgis :database (:name params)))

(defmethod load-datastore :shapefile
  [params]
  (shape :path (:path params)))

(defn add-datastore! [datastore-info]
  (add-resource! :datastores (assoc datastore-info :gt (load-datastore datastore-info))))

(defn get-datastore [ds-name]
  (get (:datastores @catalog) (keyword ds-name)))

(defn add-table! [table-info]
  (let [fs (.getFeatureSource (:gt (get-datastore (:datastore table-info))) (:name table-info))]
    (add-resource! :tables (assoc table-info :gt fs))))


(defmulti load-style (fn [style-info] (keyword (:type style-info))))

(defmethod load-style :yaml [style-info]
  (make-style
   (if (:body style-info)
     (:body style-info)
     (yaml/parse-string (slurp (:path style-info))))))


(defn add-style! [style-info]
  (add-resource! :styles (assoc style-info :gt (load-style style-info))))

(defn add-map! [map-info]
  (add-resource! :maps map-info))


(defn load-catalog [path]
  (let [{:keys [datastores tables styles maps]} (yaml/parse-string (slurp path))]
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

(defn reset-catalog! []
  (reset! catalog {}))

(defn load-default-catalog []
  (load-catalog "./catalog.yml"))