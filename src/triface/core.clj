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

(defn render [slug content opts]
  (let [model (model/models (keyword slug))]
    (model/model-render model content opts)))

(defn render-field [slug content field opts]
  (model/render (((model/models (keyword slug)) :fields) (keyword field)) content opts))

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
  )

(defmacro action [slug path-args expr]
  `(defn ~slug [~(first path-args)]
     (log :action (str ~(name slug) " - args: " ~(first path-args)))
     (let ~(vec (apply concat (map (fn [p] [`~p `(~(first path-args) ~(keyword p))]) (rest path-args))))
       (try
         (json/json-str ~expr)
         (catch Exception e#
           (log :error (str "error rendering /" (str-join "/" ~path-args) ": "
                     (render-exception e#)))
           (json/json-str
            ~(reduce #(assoc %1 (keyword %2) %2) error path-args)))))))

(action home [params]
  {:message "welcome to interface"})

(action list-all [params slug]
  (map #(render slug % {:include {}}) (content-list slug)))

(action model-spec [params slug]
  (render "model" (first (db/query "select * from model where name = '%1'" slug)) {:include {}}))

(action item-detail [params slug id]
  (render slug (content-item slug id) {:include {}}))

(action field-detail [params slug id field]
  (render-field slug (content-item slug id) field {:include {}}))

;; (action list-all [slug]
;;   (map #(render slug %) (content-list slug)))

;; (action model-spec [slug]
;;   (render "model" (first (db/query "select * from model where name = '%1'" slug))))

;; (action item-detail [slug id]
;;   (render slug (content-item slug id)))

;; (action field-detail [slug id field]
;;   (render-field slug (content-item slug id) field))

;; routes --------------------------------------------------

(defroutes main-routes
  (GET "/" [] (home))
  (GET "/:slug" {params :params} (list-all params))
  (GET "/:slug/spec" {params :params} (model-spec params))
  (GET "/:slug/:id" {params :params} (item-detail params))
  (GET "/:slug/:id/:field" {params :params} (field-detail params))
  ;; (GET "/:slug" [slug] (list-all slug))
  ;; (GET "/:slug/spec" [slug] (model-spec slug))
  ;; (GET "/:slug/:id" [slug id] (item-detail slug id))
  ;; (GET "/:slug/:id/:field" [slug id field] (field-detail slug id field))
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