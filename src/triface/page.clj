(ns triface.page
  (:use triface.debug)
  (:use clojure.contrib.str-utils)
  (:use compojure.core)
  (:require [triface.model :as model]
            [triface.db :as db]
            [triface.app.controller :as controller]
            [compojure.route :as route]
            [ring.adapter.jetty :as ring]
            [compojure.handler :as handler]))

(def pages (ref ()))
(def actions (ref {}))

(def triface-properties 
  (into {} (doto (java.util.Properties.)
    (.load (-> (Thread/currentThread)
    (.getContextClassLoader)
    (.getResourceAsStream "triface.properties"))))))

(defn default-action [params]
  (assoc params :result (str params)))

(defn render-template [template env]
  (env :result))

(defn make-route
  "make a single route for a single page, given its overarching path (above-path)
  and action directory on disk (above-action)."
  [page above-path above-action]
  (let [path (str above-path "/" (name (page :path)))
        action-path (str above-action "/" (page :action))
        route `(GET ~path {~(symbol "params") :params} ((actions ~(keyword (page :action))) ~(symbol "params")))]
    (do
      (try
        (load-file (str action-path ".clj"))
        (catch Exception e)) ;; controller file does not exist
      (let [action (or controller/action default-action)
            wrapper (fn [params]
                      (let [env (action (merge params {:template (page :template) :page page}))]
                        (log :action (str (page :action) " - path: " path " - params: " (str params) " - rendering template: " (page :template)))
                        (render-template (page :template) env)))]
        (dosync
         (alter actions merge {(keyword (page :action)) wrapper}))
        (controller/reset-action)))
    (concat (list route) (apply concat (map #(make-route % path action-path) (page :children))))))

(defn generate-routes
  "given a tree of pages construct and return a list of corresponding routes."
  [pages]
  (debug (apply
   concat
   (doall
    (map
     #(make-route % "" (str (triface-properties "applicationPath") "/app/controller"))
     pages)))))

(defn invoke-pages
  "call up the pages and arrange them into a tree."
  []
  (let [rows (db/query "select * from page")
        tree (model/arrange-tree rows)]
    (dosync
     (alter pages (fn [a b] b) tree))))

(defmacro invoke-routes
  "invoke pages from the db and generate the routes based on them."
  []
  (invoke-pages)
  (let [generated (generate-routes @pages)]
    `(defroutes all-routes ~@generated)))

(defn init
  "initialize page related activities"
  []
  (model/init)
  (invoke-routes)
  (def app (handler/site all-routes)))

(defn start [port]
  (init)
  (ring/run-jetty (var app) {:port (or port 22212) :join? false}))

