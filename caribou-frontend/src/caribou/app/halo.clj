(ns caribou.app.halo
  (:use compojure.core
        [caribou.debug])
  (:require [clojure.string :as string]
            [caribou.config :as config]
            [caribou.model :as model]))

(defn check-key
  "Inspects the X-Halo-Key request header and validates it"
  [request f]
    (let [headers (request :headers)
          req-key (headers "x-halo-key")]
      (if (= (config/app :halo-key) req-key)
        (do
          (f request))
        {:status 401 :body "Forbidden"})))

(def route-reloader (ref (fn [] "The route reloader has not been set")))

(defn reload-routes
  "reloads the routes in this Caribou app"
  [request]
  (route-reloader))

(defn reload-models
  "reloads the models in this Caribou app"
  [request]
  (model/invoke-models))

(def halo-routes
  ; we need a better way to do this so we don't have to wrap each one in check-key
  (list
    (GET (str (config/app :halo-prefix) "/" "reload-routes") [] (fn [request] (check-key request reload-routes)))
    (GET (str (config/app :halo-prefix) "/" "reload-models") [] (fn [request] (check-key request reload-models)))))

(defn generate-routes
  [_route-reloader]
  (if (config/app-value-eq :halo-enabled "true")
    (do
      (dosync
        (ref-set route-reloader (fn [] (_route-reloader) "Routes reloaded.")))
      halo-routes)))
