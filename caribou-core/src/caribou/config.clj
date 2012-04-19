(ns caribou.config
  (:use [caribou.debug]
        [clojure.walk :only (keywordize-keys)]
        [clojure.string :only (join)]
        [caribou.util :only (map-vals pathify file-exists?)])
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

(defn app-value-eq
  [kw value]
  (= (@app kw) value))

(def root (.getAbsolutePath (io/file "")))
(def db (ref {}))
(def all-db (ref {}))

(def default-db-config
  {:production
   {:classname "org.postgresql.Driver"
    :subprotocol "postgres"
    :host "localhost"
    :database "caribou"
    :user "postgres"}
   :development
   {:classname "org.postgresql.Driver"
    :subprotocol "postgres"
    :host "localhost"
    :database "caribou_development"
    :user "postgres"}
   :test
   {:classname "org.postgresql.Driver"
    :subprotocol "postgres"
    :host "localhost"
    :database "caribou_test"
    :user "postgres"}})

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

(defn assoc-subnames
  [configs]
  (map-vals assoc-subname configs))

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
  (assoc-subnames (load-yaml filename)))

(defn caribou-home
  []
  (pathify [(System/getProperty "user.home") ".caribou"]))

(defn init
  "initialize the app's config.  expects the environment"
  [env]
  (let [db-config-paths ["config" "database.yml"]
        db-config-root (pathify (cons root db-config-paths))
        db-config-parent (pathify (concat [root ".."] db-config-paths))
        db-config-home (pathify (cons (caribou-home) db-config-paths))
        db-config
        (cond
         (file-exists? db-config-root) (load-db-config db-config-root)
         (file-exists? db-config-parent) (load-db-config db-config-parent)
         (file-exists? db-config-home) (load-db-config db-config-home)
         :else (assoc-subnames default-db-config))
        env-config (db-config (keyword env))]
    (dosync
     (alter all-db merge db-config))
    (dosync
     (alter db merge env-config))))

(init (app :environment))
