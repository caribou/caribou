(ns caribou.logger
  (:require [clojure.tools.logging :as logging]
            [clj-logging-config.log4j :as log4j]))

;; define the log appenders / define layouts
(log4j/set-logger!
   :pattern "%-6p %d{HH:mm:ss}     %m%n"
  :level :debug
  :out "caribou.log"
  :name "caribou-api-file")

(log4j/set-logger!
  :pattern "%-6p %d{HH:mm:ss}     %m%n"
  :level :debug
  :out :console
  :name "caribou-api-console")

(defmacro debug 
  "Log a debug message (with an optional prefix)"
  [msg & prefix]
  (if (nil? prefix) (logging/debug msg)
  (logging/debug (str (name (first prefix)) ": " msg))))

(defmacro info 
  "Log an info message (with an optional prefix)"
  [msg & prefix]
  (if (nil? prefix) (logging/info msg)
  (logging/info (str (name (first prefix)) ": " msg))))

(defmacro warn 
  "Log a warning message (with an optional prefix)"
  [msg & prefix]
  (if (nil? prefix) (logging/warn msg)
  (logging/warn (str (name (first prefix)) ": " msg))))

(defmacro error 
  "Log an error message (with an optional prefix)"
  [msg & prefix]
  (if (nil? prefix) (logging/error msg)
  (logging/error (str (name (first prefix)) ": " msg))))
