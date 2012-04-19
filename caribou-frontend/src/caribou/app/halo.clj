(ns caribou.app.halo
  (:use compojure.core
        [caribou.debug])
  (:require [clojure.string :as string]
            [caribou.model :as model]))

(defn check-key
  [request f]
    (log :halo "check-key")
    (f request)
  )

(def route-reloader (ref (fn [] "The route reloader has not been set")))

(defn reload-routes
  "reloads the routes in this Caribou app"
  [request]
  (route-reloader))

(defn generate-routes
  [_route-reloader]
  (dosync
    (ref-set route-reloader (fn [] (_route-reloader) "Routes reloaded.")))
  (let [halo-routes
        (list (GET "/_halo/reload-routes" [] reload-routes))]
    (map (fn [route] (fn [request] (check-key request route))) halo-routes)))
