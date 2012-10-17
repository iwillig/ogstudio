(defproject ogstudio "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :repositories {"OpenGeo Maven Repository" "http://repo.opengeo.org"}
  :main ogstudio.handler
  :uberjar-name "ogstudio.jar"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.cli "0.2.2"]
                 [compojure "1.1.1"]
                 [ring "1.1.6"]
                 [clj-yaml "0.4.0"]
                 [hiccup "1.0.1"]
                 [geoscript "0.1.0-SNAPSHOT"]]
  :plugins [[lein-ring "0.7.3"] [lein-swank "1.4.4"]]
  :ring {:init ogstudio.core/load-default-catalog
         :destroy ogstudio.core/reset-catalog
         :handler ogstudio.handler/app}
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.3"]]}})