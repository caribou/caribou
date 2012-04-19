(ns caribou.app.core
  (:use compojure.core
        [clojure.string :only (join)]
        [caribou.debug])
  (:require [clojure.string :as string]
            [clojure.java.jdbc :as sql]
            [clojure.java.io :as io]
            [caribou.model :as model]
            [caribou.util :as util]
            [caribou.db :as db]
            [caribou.config :as config]
            [caribou.app.controller :as controller]
            [caribou.app.routing :as routing]
            [caribou.app.template :as template]
            [caribou.app.view :as view]
            [compojure.route :as route]))

(import (java.io File))

(defn default-template [env]
  (env :result))

(defn render-template [env]
  (let [template (or (@template/templates (keyword (env :template))) default-template)]
    (template env)))

(defn page-init []
  (model/init)
  (controller/load-controllers "app/controllers"))

(defn handler
  [request]
  ; put app-level middleware here
  (routing/dispatcher request))

(defn init
  []
  (page-init))

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


