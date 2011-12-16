(ns caribou.logger
  (:require [clojure.tools.logging :as logging]
            [clj-logging-config.log4j :as log4j]))

(log4j/set-logger!
  :pattern "%-5p %20.20c: %m%n"
  :level :warn
  :out "caribou.log"
  :name "caribou-api-file")

(log4j/set-logger!
  :pattern "%-5p %20.20c: %m%n"
  :level :warn
  :out :console
  :name "caribou-api-console")

(defn info 
  "Log an info message"
  [msg]
  (logging/info msg))

(defn debug 
  "Log a debug message"
  [msg]
  (logging/debug msg))

(defn warn 
  "Log a warning message"
  [msg]
  (logging/warn msg))

(defn error 
  "Log an error message"
  [msg]
  (logging/error msg))