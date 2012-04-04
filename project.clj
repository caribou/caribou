(defproject antler/caribou-core "0.4.0-SNAPSHOT"
  :description "caribou: type structure interaction api"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/java.jdbc "0.0.6"]
                 [postgresql/postgresql "8.4-702.jdbc4"]
                 [clj-time "0.3.6"]
                 [clj-http "0.3.2"]
                 [clj-yaml "0.3.1"]
                 [geocoder-clj "0.0.3"]
                 [org.freemarker/freemarker "2.3.18"]
                 ;; [antler/clojure-solr "0.3.0-SNAPSHOT"]
                 ;; --------- THESE DEPS ARE NOT ON 1.3 -------------
                 ;; [aleph "0.2.0-rc2"]
                 ;; [org.clojars.serabe/rinzelight "0.0.3"]
                 ;; -------------------------------------------------
                 [clj-logging-config "1.9.5"]
                 [log4j "1.2.16"]]
  :dev-dependencies [; [autodoc "0.9.0"]
                     [swank-clojure "1.4.0-SNAPSHOT"]
                     [lein-eclipse "1.0.0"]]
  :main caribou.core
  :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
  :aot [caribou.model]
  :autodoc {:name "Caribou" :page-title "Caribou API Documentation"}
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"})
                 ;; "battlecat" "http://battlecat:8080/nexus/content/groups/public"
                 ;; "snapshots" "http://battlecat:8080/nexus/content/repositories/snapshots"
                 ;; "releases" "http://battlecat:8080/nexus/content/repositories/releases"})
