(ns triface.core
  (:use compojure.core)
  (:require [triface.db :as db]
            [triface.model :as model]
            [compojure.route :as route]
            [ring.adapter.jetty :as ring]
            [compojure.handler :as handler]
            [clojure.contrib.json :as json]))


(defn content-list [slug]
  (db/query "select * from %1" slug))

(defn content-item [slug id]
  (first (db/query "select * from %1 where id = %2" slug id)))

(defn content-field [slug id field]
  ((content-item slug id) field))

(defn render [slug content]
  (model/model-render (model/models (keyword slug)) content))

(defn render-field [slug content field]
  (model/render (((model/models (keyword slug)) :fields) (keyword field)) content))

;; actions ------------------------------------------------

(defn home []
  (json/json-str {:message "welcome to interface"}))

(defn list-all [slug]
  (json/json-str (map #(render slug %) (content-list slug))))

(defn model-spec [slug]
  (json/json-str (render "model" (first (db/query "select * from model where name = '%1'" slug)))))

(defn item-detail [slug id]
  (json/json-str (render slug (content-item slug id))))

(defn field-detail [slug id field]
  (json/json-str (render-field slug (content-item slug id) field)))

;; routes --------------------------------------------------

(defroutes main-routes
  (GET "/" [] (home))
  (GET "/:slug" [slug] (list-all slug))
  (GET "/:slug/spec" [slug] (model-spec slug))
  (GET "/:slug/:id" [slug id] (item-detail slug id))
  (GET "/:slug/:id/:field" [slug id field] (field-detail slug id field))
  (route/resources "/")
  (route/not-found "NONONONONONON"))

(def app (handler/site main-routes))

(defn start [port]
  (ring/run-jetty (var app) {:port (or port 33333) :join? false}))

(defn -main []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "33333"))]
    (dosync (alter model/models
                   merge
                   (reduce #(assoc %1 (keyword (%2 :name)) (model/fetch-fields %2)) {}
                   (db/query "select * from model"))))
    (start port)))