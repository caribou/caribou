(ns caribou.app
  (:require [caribou.api :as api]
            [caribou.page :as page]))

(defn run-api
  []
  (api/go))

(defn run-pages
  []
  (page/go))

(defn launch
  []
  (run-api)
  (run-pages))