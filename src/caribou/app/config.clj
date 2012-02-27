(ns caribou.app.config
  (:use [caribou.debug]
        [clojure.walk :only (keywordize-keys)]
        [clojure.string :only (join)])
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [caribou.util :as util]))

(def app
  (ref
   (keywordize-keys
    (into {} (doto (java.util.Properties.)
               (.load
                (-> (Thread/currentThread)
                    (.getContextClassLoader)
                    (.getResourceAsStream "caribou.properties"))))))))

(def root (.getAbsolutePath (io/file "")))
(def db (ref {}))
(def all-db (ref {}))

(def file-sep 
  (str (.get (java.lang.System/getProperties) "file.separator")))

(defn load-yaml
  [filename]
  (let [raw (slurp filename)
        config (yaml/parse-string raw)]
    config))

(defn assoc-subname
  [config]
  (let [host (or (config :host) "localhost")
        subname (or (config :subname) (str "//" host "/" (config :database)))]
    (assoc config :subname subname)))
  

(defn load-db-config
  "Given the path to a yaml config file, produce the map representing it.
  The config is of the form (for each environment):

  environment:
      classname: org.postgresql.Driver
      subprotocol: postgresql
      host: localhost
      database: caribou
      user: postgres"

  [filename]
  (util/map-vals assoc-subname (load-yaml filename)))

(defn init
  "initialize the app's config.  expects the environment"
  [env]
  (let [db-config-file (join file-sep [root "config" "database.yml"])
        db-config (load-db-config db-config-file)
        env-config (db-config env)]
    (dosync
     (alter all-db merge db-config))
    (dosync
     (alter db merge env-config))))

(init (app :environment))
