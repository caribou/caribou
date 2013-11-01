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
   "deploying"])

(def pre-layout (slurp "resources/header.html"))

(comment
  "<!DOCTYPE html>
<html>
  <head>
    <meta charset='utf-8'>
    <meta http-equiv=\"X-UA-Compatible\" content=\"chrome=1\">

    <script type=\"text/javascript\" src=\"js/shCore.js\"></script>
    <script type=\"text/javascript\" src=\"js/shAutoloader.js\"></script>
    <script type=\"text/javascript\" src=\"js/shBrushClojure.js\"></script>
    <script type=\"text/javascript\" src=\"js/shBrushBash.js\"></script>
    <script type=\"text/javascript\" src=\"js/shBrushXml.js\"></script>
    <link rel=\"stylesheet\" type=\"text/css\" href=\"stylesheets/stylesheet.css\" media=\"screen\" />
    <link rel=\"stylesheet\" type=\"text/css\" href=\"stylesheets/shCore.css\" media=\"screen\" />
    <link rel=\"stylesheet\" type=\"text/css\" href=\"stylesheets/shThemeRDark.css\" media=\"screen\" />
    <link rel=\"stylesheet\" type=\"text/css\" href=\"stylesheets/shClojureExtra.css\" media=\"screen\" />

    <title>Caribou Documentation</title>
  </head>

  <body>

    <header>
      <div class=\"container\">
        <h1><a href=\"outline.html\">Caribou Documentation</a></h1>
      </div>
    </header>

    <div class=\"container\">
      <section id=\"main_content\">
")

(def post-layout (slurp "resources/footer.html"))

(comment
  "
      </section>
    </div>

  </body>
</html>

<script type=\"text/javascript\">
  SyntaxHighlighter.defaults['gutter'] = false;
  SyntaxHighlighter.defaults['toolbar'] = false;
  SyntaxHighlighter.autoloader(
    'clj clojure js/shBrushClojure.js',
    'xml html js/shBrushXml.js',
    'sh bash js/shBrushBash.js'
  );
  SyntaxHighlighter.all()
</script>
")

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
