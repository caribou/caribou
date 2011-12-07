(ns caribou.page
  (:use caribou.debug
        compojure.core
        [clojure.string :only (join)]
        [ring.middleware file file-info stacktrace reload])
  (:require [caribou.model :as model]
            [caribou.db :as db]
            [caribou.app.controller :as app]
            [caribou.app.template :as template]
            [caribou.app.view :as view]
            [clojure.string :as string]
            [clojure.java.jdbc :as sql]
            [compojure.route :as route]
            [ring.adapter.jetty :as ring]
            [compojure.handler :as handler]
            [caribou.app.config :as config]
            [clojure.java.io :as io]))

(import (java.io File))

(def pages (ref ()))
(def actions (ref {}))

(defn default-action [params]
  (assoc params :result (str params)))

(defn default-template [env]
  (env :result))

(defn render-template [env]
  (let [template (or (@template/templates (keyword (env :template))) default-template)]
    (template env)))

(defn match-action-to-template
  "make a single route for a single page, given its overarching path (above-path)"
  [page above-path]
  (let [path (str above-path "/" (name (page :path)))
        controller (@app/controllers (keyword (page :controller)))
        action-key (keyword (page :action))
        action (or (action-key (debug controller)) default-action)
        template (@template/templates (keyword (page :template)))
        full (fn [params] ((debug template) (action (merge params {"yellow" 555 :page page}))))]
    (dosync
     (alter actions merge {(keyword (page :action)) full}))
    (concat
     [[path action-key]]
     (apply
      concat
      (map #(match-action-to-template % path) (page :children))))))

(defn make-route
  [[path action]]
  `(GET ~path {~'params :params} ((~'actions ~action) ~'params)))

(defn generate-routes
  "given a tree of pages construct and return a list of corresponding routes."
  [pages]
  (let [routes (apply concat (map #(match-action-to-template % "") pages))]
    (doall (map make-route routes))))

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
  (template/load-templates (join config/file-sep [config/root "app" "templates"]))
  (sql/with-connection @config/db
    (let [_pages (invoke-pages)
          generated (debug (generate-routes @pages))]
      `(defroutes all-routes ~@generated))))

(defn dbinit []
  (model/init)
  (invoke-routes))

(defn init
  "initialize page related activities"
  []
  (sql/with-connection @config/db (dbinit)))

(defn start
  ([port] (start port {}))
  ([port user-db]
     (let [db (merge @config/db user-db)]
       (sql/with-connection db (dbinit))
       (def app (-> all-routes
                    (wrap-file (join config/file-sep [config/root "public"]))
                    (wrap-file-info)
                    (wrap-stacktrace)
                    (handler/site)
                    (db/wrap-db db)))
       (ring/run-jetty (var app) {:port (or port 22212) :join? false}))))

(defn go []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "22212"))]
    (start port @config/db)))

(defn -main []
  (go))
