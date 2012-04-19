(defproject antler/caribou-api "0.2.2"
  :description "The api ring handler for caribou"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [antler/caribou-core "0.4.5"]
                 [clj-http "0.3.6"]
                 [compojure "1.0.1"]
                 [ring/ring-core "1.0.2"
                  :exclusions [org.clojure/clojure
                               clj-stacktrace]]
                 [hiccup "0.3.6"]
                 [org.clojars.doo/cheshire "2.2.3"]
                 ;; [cheshire "3.1.0"]
                 [org.clojure/data.xml "0.0.3"]
                 [clojure-csv/clojure-csv "1.3.2"]
                 [org.clojars.cjschroed/sandbar "0.4.0"]]
                 ;; [antler/sandbar "0.4.0-SNAPSHOT"]]
  :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
  :ring {:handler caribou.api.core/app
         :servlet-name "caribou-api"
         :init caribou.api.core/init
         :port 33443})

