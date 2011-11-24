(ns caribou.page
  (:use caribou.debug
        compojure.core
        [ring.middleware file file-info stacktrace reload])
  (:require [caribou.model :as model]
            [caribou.db :as db]
            [caribou.app.controller :as controller]
            [caribou.app.view :as view]
            [clojure.string :as string]
            [clojure.java.jdbc :as sql]
            [compojure.route :as route]
            [ring.adapter.jetty :as ring]
            [compojure.handler :as handler]
            [caribou.app.config :as config]))

(import (java.io File))

(def pages (ref ()))
(def actions (ref {}))
(def templates (ref {}))

(defn default-action [params]
  (assoc params :result (str params)))

(defn default-template [env]
  (env :result))

(defn render-template [env]
  (let [template (or (templates (keyword (env :template))) default-template)]
    (template env)))

(defn make-route
  "make a single route for a single page, given its overarching path (above-path)
  and action directory on disk (above-action)."
  [page above-path above-action]
  (let [path (str above-path "/" (name (page :path)))
        action-path (str above-action "/" (page :action))
        route `(GET ~path {~(symbol "params") :params} ((actions ~(keyword (page :action))) ~(symbol "params")))]
    (do
      (let [action-file (str action-path ".clj")
            action (if (.exists (new File action-file))
                     (do (load-file action-file) controller/action)
                     default-action)
            wrapper
            (fn [params]
              (let [full (merge params {:template (page :template) :page page})
                    env (action full)]
                (log :action (str (page :action) " - path: " path " - params: " (str params) " - rendering template: " (page :template)))
                (render-template env)))]
        (dosync
         (alter actions merge {(keyword (page :action)) wrapper}))
        (controller/reset-action)))
    (concat (list route) (apply concat (map #(make-route % path action-path) (page :children))))))

(defn load-templates
  "recurse through the view directory and add all the templates that can be found"
  [template-path]
  (let [base (str template-path "/app/view")]
    (loop [fseq (file-seq (new File base))]
      (if fseq
        (let [filename (.toString (first fseq))]
          (if (.isFile (first fseq))
            (do
              (load-file filename)
              (let [template view/template
                    template-key (keyword
                                  (string/replace 
                                   (string/replace filename (str base "/") "")
                                   ".clj" ""))]
                (dosync
                 (alter templates merge {template-key template})))))
          (recur (next fseq)))))))

(defn generate-routes
  "given a tree of pages construct and return a list of corresponding routes."
  [pages app-path]
  (apply
   concat
   (doall
    (map
     #(make-route % "" (str app-path "/app/controller"))
     pages))))

(def all-routes)

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
  (sql/with-connection @config/db
    (let [app-path (config/caribou-properties "applicationPath")
          _pages (invoke-pages)
          _templates (load-templates app-path)
          generated (generate-routes @pages app-path)]
      `(defroutes all-routes ~@generated))))

(defn dbinit []
  (model/init)
  (invoke-routes))

(defn init
  "initialize page related activities"
  []
  (sql/with-connection @config/db (dbinit)))

(defn start [port db]
  (let [full-db (merge @config/db db)]
    (sql/with-connection full-db (dbinit))
    (def app (-> all-routes
                 (wrap-file (str (config/caribou-properties "applicationPath") "/public"))
                 (wrap-file-info)
                 (wrap-stacktrace)
                 (handler/site)
                 (db/wrap-db full-db)))
    (ring/run-jetty (var app) {:port (or port 22212) :join? false})))

(defn go []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "22212"))]
    (start port @config/db)))

(defn -main []
  (go))
