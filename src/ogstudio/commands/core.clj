(ns ogstudio.commands.core
  (:require
   [ogstudio.core     :as core]
   [clojure.java.io   :as io]
   [geoscript.style   :as gstyle]
   [clj-yaml.core     :as yaml]))

(defn load-cat [path] (yaml/parse-string (slurp path)))

(defn write-style [style-info]
  (let [style-gt (core/load-style style-info) ;; get the geotools style object
        sld (gstyle/style->sld
             (gstyle/make-sld {:name (:name style-info)
                               :style style-gt}))]
    (spit (io/file "out" (str (:name style-info) ".sld"))  sld)))

(defn do-write-style [& args]
  (let [[_ config-path style-name] args]
    (doseq [style (:styles (load-cat config-path))]
      (when (= (:name style) style-name)
        (write-style style)))))

(defn do-write-styles [& args]
  (let [[_ config-path out] args]
    (if config-path
      (do (map write-style (:styles (load-cat config-path)))))))


(defn -main [& args]
  (let [command (first args)]

    (case command
      "write-style"    (apply do-write-style args)
      "write-styles"   (apply do-write-styles args))))