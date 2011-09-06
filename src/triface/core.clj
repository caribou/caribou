(ns triface.core
  (:use compojure.core)
  (:use clojure.contrib.str-utils)
  (:use triface.debug)
  (:require [triface.db :as db]
            [triface.model :as model]
            [compojure.route :as route]
            [ring.adapter.jetty :as ring]
            [compojure.handler :as handler]
            [clojure.contrib.string :as string]
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

(defn render [slug content opts]
  (let [model (model/models (keyword slug))]
    (model/model-render model content (debug opts))))

(defn render-field [slug content field opts]
  (model/render (((model/models (keyword slug)) :fields) (keyword field)) content (debug opts)))

;; actions ------------------------------------------------

(defn render-exception [e]
  (let [cause (.getCause e)]
    (if cause
      (if (isa? cause SQLException)
        (let [next (.getNextException cause)]
          (str next (.printStackTrace next)))
        (str cause (.printStackTrace cause)))
      (str e (.printStackTrace e)))))

(defn process-include [include]
  (if (and include (not (empty? include)))
    (let [clauses (string/split #"," include)
          paths (map #(string/split #"\." %) clauses)
          maps (reduce #(assoc %1 (keyword (first %2)) (process-include (str-join "." (rest %2)))) {} paths)]
      maps)
    {}))

(defmacro action [slug path-args expr]
  `(defn ~slug [~(first path-args)]
     (log :action (str ~(name slug) " => " ~(first path-args)))
     (let ~(vec (apply concat (map (fn [p] [`~p `(~(first path-args) ~(keyword p))]) (rest path-args))))
       (try
         (json/json-str ~expr)
         (catch Exception e#
           (log :error (str "error rendering /" (str-join "/" [~@(rest path-args)]) ": "
                     (render-exception e#)))
           (json/json-str
            ~(reduce #(assoc %1 (keyword %2) %2) error path-args)))))))

(action home [params]
  {:message "welcome to interface"})

(action list-all [params slug]
  (let [include (process-include (params :include))
        included (assoc params :include include)]
    (map #(render slug % included) (content-list slug))))

(action model-spec [params slug]
  (render "model" (first (db/query "select * from model where name = '%1'" slug)) {:include {:fields {}}}))

(action item-detail [params slug id]
  (let [include (process-include (params :include))]
    (render slug (content-item slug id) (assoc params :include include))))

(action field-detail [params slug id field]
  (let [include {(keyword field) (process-include (params :include))}]
    (render-field slug (content-item slug id) field (assoc params :include include))))

(action create-content [params slug]
  (render slug (model/create-content slug (params (keyword slug))) params))

(action update-content [params slug id]
  (let [content (model/update-content slug id (params (keyword slug)))]
    (render slug (db/choose slug id) params)))

(action delete-content [params slug id]
  (let [content (model/delete-content slug id)]
    (render slug content params)))

;; routes --------------------------------------------------

(defroutes main-routes
  (GET  "/" {params :params} (home params))
  (GET  "/:slug" {params :params} (list-all params))
  (POST "/:slug" {params :params} (create-content params))
  (GET  "/:slug/spec" {params :params} (model-spec params))
  (GET  "/:slug/:id" {params :params} (item-detail params))
  (PUT  "/:slug/:id" {params :params} (update-content params))
  (DELETE  "/:slug/:id" {params :params} (delete-content params))
  (GET  "/:slug/:id/:field" {params :params} (field-detail params))
  (route/resources "/")
  (route/not-found "NONONONONONON"))

(def app (handler/site main-routes))

(defn init []
  (model/invoke-models)
  "models invoked")

(defn start [port]
  (ring/run-jetty (var app) {:port (or port 33333) :join? false}))

(defn go []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "33333"))]
    (init)
    (start port)))

(defn -main []
  (go))