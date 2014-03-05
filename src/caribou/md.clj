(ns caribou.md
  (:require [clojure.string :as string]
            [markdown.core :as md]))

(def targets
  ["outline"
   "quickstart"
   "what-is-caribou"
   "getting-started"
   "components"
   "basic-concepts"
   "philosophy"
   "structure"
   "configuring"
   "models"
   "content"
   "migrations"
   "localization"
   "routes"
   "controllers"
   "templates"
   "misc"
   "tutorial"
   "api"
   "deploying"
   "debugging"
   "upgrading"
   "roadmap"])

(def pre-layout (slurp "resources/header.html"))
(def post-layout (slurp "resources/footer.html"))

(defn wrap-layout
  [html]
  (str pre-layout html post-layout))

(defn convert-links
  [md]
  (string/replace md #"\(([^.]+)\.md\)" "($1.html)"))

(defn compile-targets
  []
  (doseq [target targets]
    (let [original (slurp (str target ".md"))
          converted (convert-links original)
          html (md/md-to-html-string converted)
          wrapped (wrap-layout html)]
      (spit (str "docs/" target ".html") wrapped))))

(defn -main
  []
  (compile-targets))
