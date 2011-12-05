(ns caribou.app.template.freemarker)
(import '(freemarker.template Configuration DefaultObjectWrapper))
(import '(freemarker.cache NullCacheStorage))
(import '(java.io StringWriter File))

(def freemarker-config (Configuration.))

(defn init
  "Set up our Freemarker config"
  [template-path]
  (doto freemarker-config
    (.setDirectoryForTemplateLoading (File. template-path))
    (.setCacheStorage (NullCacheStorage.))
    (.setObjectWrapper (DefaultObjectWrapper.))))

(defn get-template
  "Gets a Freemarker template from the Configuration"
  [template-name]
  (.getTemplate freemarker-config template-name))
 
(defn render
  "Process a Freemarker template, returns a String"
  ([template root template-length]
  (let [out (StringWriter. template-length)]
    (.process template root out)
    (.toString out)))
 
  ([template root]
    (let [template-length (.length (.toString template))]
      (render template root template-length))))

(defn render-wrapper
  "Wraps a template filename in a render"
  [template-name]
  (fn [root] 
    ; we put get-template inside the func call because we want freemarker to handle 
    ; caching/reloading for us
    (let [template (get-template template-name)
          template-length (.length (.toString template))]
        (render template root template-length))))
