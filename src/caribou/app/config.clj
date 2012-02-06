(ns caribou.app.config
  (:use [caribou.debug]
        [clojure.walk :only (keywordize-keys)]
        [clojure.string :only (join)])
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]))

(def app
  (ref
   (keywordize-keys
    (into {} (doto (java.util.Properties.)
               (.load
                (-> (Thread/currentThread)
                    (.getContextClassLoader)
                    (.getResourceAsStream "caribou.properties"))))))))

(def db (ref {}))
(def root (.getAbsolutePath (io/file "")))

(def file-sep 
  (str (.get (java.lang.System/getProperties) "file.separator")))

(defn load-yaml
  [filename]
  (let [raw (slurp filename)
        config (yaml/parse-string raw)]
    config))

(defn load-db-config
  "given the path to a yaml config file, produce the map representing it.
  the config is of the form:
  production:
      classname: org.postgresql.Driver
      subprotocol: postgresql
      host: localhost
      database: caribou
      user: postgres"
  [filename env]
  (let [config ((load-yaml filename) (keyword env))
        host (or (config :host) "localhost")
        subname (or (config :subname) (str "//" host "/" (config :database)))]
    (assoc config :subname subname)))

(defn init
  "initialize the app's config.  expects the environment (hard-coded to :production for now)"
  [env]
  (let [db-config-file (join file-sep [root "config" "database.yml"])
        db-config (load-db-config db-config-file env)]
    (dosync
      (alter db merge db-config))))

(init (app :environment))
