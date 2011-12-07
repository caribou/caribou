(ns caribou.app
  (:require [caribou.api :as api]
            [caribou.page :as page]))

(defn run-api
  [port]
  (api/start port))

(defn run-pages
  [port]
  (page/start port))