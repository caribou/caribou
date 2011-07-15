(ns triface.core
  (:use compojure.core)
  (:require [triface.db :as db]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.contrib.json :as json]))

(defn stringify [that]
  (reduce #(assoc %1 %2 (.toString (that %2))) {} (keys that)))

(defn model-spec [slug]
  (first (db/query "select * from model where name = '%1'" slug)))

(defn content-list [slug]
  (db/query "select * from %1" slug))

(defn content-item [slug id]
  (first (db/query "select * from %1 where id = %2" slug id)))

(defn content-field [slug id field]
  ((content-item slug id) field))

(defroutes main-routes
  (GET "/" [] (json/json-str {:message "welcome to interface"}))
  (GET "/:slug" [slug] (json/json-str (map stringify (content-list slug))))
  (GET "/:slug/spec" [slug] (json/json-str (stringify (model-spec slug))))
  (GET "/:slug/:id" [slug id] (json/json-str (stringify (content-item slug id))))
  (GET "/:slug/:id/:field" [slug id field] (json/json-str (stringify (content-field slug id field))))
  (route/resources "/")
  (route/not-found "NONONONONONON"))

(def app (handler/site main-routes))

