(ns triface.logger
  (:use clojure.tools.logging)
  (:use clj-logging-config.log4j))

(set-logger!
  :pattern "%-5p %20.20c: %m%n"
  :level :warn
  :out "triface.log"
  :name "triface-api-file")

(set-logger!
  :pattern "%-5p %20.20c: %m%n"
  :level :warn
  :out :console
  :name "triface-api-console")