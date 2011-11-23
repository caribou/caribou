(ns triface.app.config
  (:use [triface.debug])
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
))

(def triface-properties 
  (into {} (doto (java.util.Properties.)
    (.load (-> (Thread/currentThread)
    (.getContextClassLoader)
    (.getResourceAsStream "triface.properties"))))))

(def db (ref {}))


(def file-sep 
  (str (.get (java.lang.System/getProperties) "file.separator")))

(defn init
  "initialize the app's config.  expects the environment (hard-coded to :production for now)"
  [env]

  ; get the database config
  (let [app-path (triface-properties "applicationPath")
        db-config-file (str app-path file-sep "config" file-sep "database.yml")
        yaml-config (yaml/parse-string (slurp db-config-file))]
    (dosync
      (alter db merge (yaml-config (keyword env))))))

(init :production)
