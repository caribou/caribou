(ns caribou.app.config
  (:use [caribou.debug])
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]))

(def caribou-properties 
  (into {} (doto (java.util.Properties.)
    (.load
      (-> (Thread/currentThread)
          (.getContextClassLoader)
          (.getResourceAsStream "caribou.properties"))))))

(def db (ref {}))

(def file-sep 
  (str (.get (java.lang.System/getProperties) "file.separator")))

(defn process-db-config
  "given the path to a yaml config file, produce the map representing it.
  the config is of the form:
  production:
      classname: org.postgresql.Driver
      subprotocol: postgresql
      host: localhost
      database: caribou
      user: postgres"
  [filename env]
  (let [raw (slurp filename)
        config ((yaml/parse-string raw) (keyword env))
        host (or (config :host) "localhost")
        subname (or (config :subname) (str "//" host "/" (config :database)))]
    (assoc config :subname subname)))

(defn init
  "initialize the app's config.  expects the environment (hard-coded to :production for now)"
  [env]
  (let [app-path (caribou-properties "applicationPath")
        db-config-file (str app-path file-sep "config" file-sep "database.yml")
        yaml-config (process-db-config db-config-file env)]
    (dosync
      (alter db merge yaml-config))))

(init :production)
