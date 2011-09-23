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

(defn default-execute [params]
  (debug params)
  (str params))

(defn make-route
  "make a single route for a single page, given its overarching path (above-path)
  and action directory on disk (above-action)."
  [page above-path above-action]
  (let [path (str above-path "/" (name (page :path)))
        action-path (str above-action "/" (page :action))
        route `(GET ~path {~(symbol "params") :params} ((or (actions ~(keyword (page :template))) default-execute) ~(symbol "params")))]
    (try 
      (do
        (load-file (str action-path ".clj"))
        (dosync
         (alter actions merge {(keyword (page :action)) controller/action}))
        (controller/reset-action))
      (catch Exception e))
    (concat (list route) (apply concat (map #(make-route % path action-path) (page :children))))))

(defn generate-routes
  "given a tree of pages construct and return a list of corresponding routes."
  [pages]
  (apply concat
         (doall (map #(make-route % "" (str (triface-properties "applicationPath") "/app/controller")) pages))))

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
  (invoke-routes))

(defn start [port]
  (init)
  (def app (handler/site all-routes))
  (ring/run-jetty (var app) {:port (or port 22212) :join? false}))

