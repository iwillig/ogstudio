(ns ogstudio.handler
  (:gen-class)
  (:use
   [clojure.tools.cli :only [cli]]
   [ring.adapter.jetty :only (run-jetty)]
   [compojure.core])
  (:require
   [ogstudio.core       :as core]
   [ogstudio.views      :as views]
   [compojure.handler   :as handler]
   [compojure.route     :as route]))

(defroutes app-routes
  (GET "/" [] views/index)
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))


(defn -main [& args]
  (let [[options args banner]
        (cli
         args
         ["--config" "Configuration file" :default false]
         ["--port" "Port number" :default 3000]
         ["--help" "Show help" :default false  :flag true])]

    (when (:help options)
      (println banner)
      (System/exit 0))

    (when-not (:config options)
      (println "You must provide a configuration file.")
      (System/exit 0))

    (core/load-catalog (:config options))

    (run-jetty #'app {:port (:port options) :join? false})))