(ns caribou.md
  (:require [markdown.core :as md]))

(def targets
  ["outline"
   "quickstart"
   "what-is-caribou"
   "getting-started"
   "components"
   "basic-concepts"
   "philosophy"
   "configuring"
   "models"
   "routes"
   "controllers"
   "templates"
   "misc"
   "tutorial"
   "api"
   "deploying"])

(defn compile-targets
  []
  (doseq [target targets]
    (md/md-to-html (str target ".md") (str "docs/" target ".html"))))

(defn -main
  []
  (compile-targets))
