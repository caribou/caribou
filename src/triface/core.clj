(ns triface.core
  (:use compojure.core)
  (:use clojure.contrib.str-utils)
  (:use triface.debug)
  (:require [triface.db :as db]
            [triface.model :as model]
            [compojure.route :as route]
            [ring.adapter.jetty :as ring]
            [compojure.handler :as handler]
            [clojure.contrib.json :as json]))

(import java.sql.SQLException)

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
    (model/model-render model content {:include {:fields {}}})))

(defn render-field [slug content field]
  (model/render (((model/models (keyword slug)) :fields) (keyword field)) content {:include {}}))

;; actions ------------------------------------------------

(defn render-exception [e]
  (let [cause (.getCause e)]
    (if cause
      (if (isa? cause SQLException)
        (let [next (.getNextException cause)]
          (str next (.printStackTrace next)))
        (str cause (.printStackTrace cause)))
      (str e (.printStackTrace e)))))

(defmacro action [name path-args expr]
  `(defn ~name ~path-args
     (try
       (json/json-str ~expr)
       (catch Exception e#
         (log (str "error rendering /" (str-join "/" ~path-args) ": "
                   (render-exception e#)))
         (json/json-str
          ~(reduce #(assoc %1 (keyword %2) %2) error path-args))))))

(action home []
  {:message "welcome to interface"})

(action list-all [slug]
  (map #(render slug %) (content-list slug)))

(action model-spec [slug]
  (render "model" (first (db/query "select * from model where name = '%1'" slug))))

(action item-detail [slug id]
  (render slug (content-item slug id)))

(action field-detail [slug id field]
  (render-field slug (content-item slug id) field))

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

(defn init []
  (model/invoke-models))

(defn start [port]
  (ring/run-jetty (var app) {:port (or port 33333) :join? false}))

(defn -main []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "33333"))]
    (init)
    (start port)))