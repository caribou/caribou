(ns triface.core
  (:use compojure.core)
  (:use triface.debug)
  (:require [triface.db :as db]
            [triface.model :as model]
            [compojure.route :as route]
            [ring.adapter.jetty :as ring]
            [compojure.handler :as handler]
            [clojure.contrib.json :as json]))

(def error
  {:message "Unable to process request"
   :slug nil})

(defn content-list [slug]
  (db/query "select * from %1" slug))

(defn content-item [slug id]
  (first (db/query "select * from %1 where id = %2" slug id)))

(defn content-field [slug id field]
  ((content-item slug id) field))

(defn render [slug content]
  (let [model (model/models (keyword slug))]
    (model/model-render model content)))

(defn render-field [slug content field]
  (model/render (((model/models (keyword slug)) :fields) (keyword field)) content))

;; actions ------------------------------------------------

(defn home []
  (try
    (json/json-str {:message "welcome to interface"})
  (catch Exception e
    (log (str "Error rendering [/]: " e))
    (json/json-str error))))

(defn list-all [slug]
  (try
    (json/json-str (map #(render slug %) (content-list slug)))
  (catch Exception e
    (log (str "Error rendering [/" slug "]: " e))
    (json/json-str (assoc error :slug slug)))))

(defn model-spec [slug]
  (try
    (json/json-str (render "model" (first (db/query "select * from model where name = '%1'" slug))))
  (catch Exception e
    (log (str "Error rendering [/" slug "/spec]: " e))
    (json/json-str (assoc error :slug slug)))))

(defn item-detail [slug id]
  (try
    (json/json-str (render slug (content-item slug id)))
  (catch Exception e
    (log (str "Error rendering [/" slug "/" id "]: " e))
    (json/json-str (conj (assoc error :slug slug) {:id id})))))

(defn field-detail [slug id field]
  (try
    (json/json-str (render-field slug (content-item slug id) field))
  (catch Exception e
    (log (str "Error rendering [/" slug "/" id "/" field "]: " e))
    (json/json-str (conj (assoc error :slug slug) {:id id :field field})))))

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
    (model/invoke-models)
    (start port)))