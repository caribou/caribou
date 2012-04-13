(ns caribou.app.core
  (:use compojure.core
        [clojure.string :only (join)]
        [clojure.walk :only (stringify-keys)]
        [ring.middleware file file-info stacktrace reload])
  (:require [clojure.string :as string]
            [clojure.java.jdbc :as sql]
            [clojure.java.io :as io]
            [caribou.model :as model]
            [caribou.util :as util]
            [caribou.db :as db]
            [caribou.config :as config]
            [caribou.app.controller :as controller]
            [caribou.app.template :as template]
            [caribou.app.view :as view]
            [compojure.route :as route]
            [compojure.handler :as handler]))

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

(defn retrieve-action
  "Given the controller-key and action-key, return the function that is correspondingly defined by a controller."
  [controller-key action-key]
  (let [controller (@controller/controllers controller-key)
        action (or (action-key controller) default-action)]
    action))

(defn generate-action
  "Depending on the application environment, reload controller files (or not)."
  [page template controller-key action-key]
  (if (= (@config/app :environment) "development")
    (fn [params]
      (do
        (controller/load-controllers "app/controllers")
        (template/load-templates (util/pathify [config/root "app" "templates"]))
        (let [action (retrieve-action controller-key action-key)
              found-template
              (or template
                  (do
                    (template/load-templates (util/pathify [config/root "app" "templates"]))
                    (@template/templates (keyword (page :template)))))]
          (if found-template
            (found-template (stringify-keys (action (assoc params :page page))))
            (str "No template by the name " (page :template))))))
    (let [action (retrieve-action controller-key action-key)]
      (if template
        (fn [params]
          (template (stringify-keys (action (assoc params :page page)))))
        (fn [params] (str "No template by the name " (page :template)))))))

(defn match-action-to-template
  "Make a single route for a single page, given its overarching path (above-path)"
  [page above-path]
  (let [page-path (page :path)
        path (str above-path "/" (if page-path (name page-path) ""))
        controller-key (keyword (page :controller))
        action-key (keyword (page :action))
        method-key (page :method)
        template (@template/templates (keyword (page :template)))
        full (generate-action page template controller-key action-key)]
    (dosync
     (alter actions merge {(keyword (page :action)) full}))
    (concat
     [[path action-key method-key]]
     (mapcat #(match-action-to-template % path) (page :children)))))

(defn make-route
  [[path action method]]
  (cond
   (= method "GET")    (GET path {params :params} ((actions action) params))
   (= method "POST")   (POST path {params :params} ((actions action) params))
   (= method "PUT")    (PUT path {params :params} ((actions action) params))
   (= method "DELETE") (DELETE path {params :params} ((actions action) params))
   :else               (GET path {params :params} ((actions action) params))))

(defn generate-routes
  "Given a tree of pages construct and return a list of corresponding routes."
  [pages]
  (let [routes (apply concat (map #(match-action-to-template % "") pages))
        direct (map make-route routes)
        unslashed (filter #(empty? (re-find #"/$" (first %))) routes)
        slashed (map #(cons (str (first %) "/") (rest %)) unslashed)
        indirect (map make-route slashed)]
    (concat direct indirect)))

(defn invoke-pages
  "Call up the pages and arrange them into a tree."
  []
  (let [rows (db/query "select * from page")
        tree (model/arrange-tree rows)]
    (dosync
     (alter pages (fn [a b] b) tree))))

(defn invoke-routes
  "Invoke pages from the db and generate the routes based on them."
  []
  (template/load-templates (util/pathify [config/root "app" "templates"]))
  (sql/with-connection @config/db
    (let [_pages (invoke-pages)
          generated (doall (generate-routes @pages))]
      (apply routes generated))))

(declare reload-routes)

(defn page-init []
  (model/init)
  (model/add-hook :page :after_save :reload-routes
    (fn [env]
      (reload-routes @config/db)
      env))
  (controller/load-controllers "app/controllers")
  (def all-routes (invoke-routes)))

(defn init
  "Initialize page related activities"
  []
  (sql/with-connection @config/db (page-init)))

(declare app)

(defn reload-routes
  [db]
  (sql/with-connection db (page-init))
  (def app (-> all-routes
               (wrap-file (util/pathify [config/root "public"]))
               (wrap-file-info)
               (wrap-stacktrace)
               (handler/site)
               (db/wrap-db db))))

(defn init
  []
  (reload-routes @config/db))

;; (defn start
;;   ([port ssl-port] (start port ssl-port {}))
;;   ([port ssl-port user-db]
;;      (let [db (merge @config/db user-db)]
;;        (reload-routes db)
;;        (ring/run-jetty (var app) {:port port :join? false :host "127.0.0.1"}))))

;; (defn go []
;;   (let [port (Integer/parseInt (or (@config/app :pages-port) "22212"))
;;         ssl-port (Integer/parseInt (or (@config/app :pages-ssl-port) "22242"))]
;;     (start port ssl-port @config/db)))

;; (defn -main []
;;   (go))


