(ns caribou.logger
  (:use clojure.tools.logging)
  (:use clj-logging-config.log4j))

(set-logger!
  :pattern "%-5p %20.20c: %m%n"
  :level :warn
  :out "caribou.log"
  :name "caribou-api-file")

(set-logger!
  :pattern "%-5p %20.20c: %m%n"
  :level :warn
  :out :console
  :name "caribou-api-console")