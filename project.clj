(defproject triface "1.0.0-SNAPSHOT"
  :description "Interface: type structure interaction api"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/java.jdbc "0.0.6"]
                 [postgresql/postgresql "8.4-702.jdbc4"]
                 [compojure "0.6.4"]
                 [hiccup "0.3.6"]
                 [cheshire "2.0.2"]
                 ;; [org.clojure/data.json "0.1.1"]
                 [org.clojars.ninjudd/data.xml "0.0.1-SNAPSHOT"]
                 [ring/ring-jetty-adapter "0.3.10"]
                 [geocoder-clj "0.0.3"]
                 [clojure-csv/clojure-csv "1.3.2"]
                 [com.instrument.triface/triface-action-adapter "1.0-SNAPSHOT"]
                 ;; --------- THESE DEPS ARE NOT ON 1.3 -------------
                 ;; [aleph "0.2.0-rc2"]
                 ;; [sandbar "0.4.0-SNAPSHOT"]
                 ;; [clojure-solr "0.2.0"]
                 ;; [org.clojars.serabe/rinzelight "0.0.3"]
                 ;; [clj-logging-config "1.7.0"]
                 ;; -------------------------------------------------
                 [log4j "1.2.16"]]
  :dev-dependencies [[lein-ring "0.4.5"]
                     [org.clojure/java.jdbc "0.0.6"]
                     [postgresql/postgresql "8.4-702.jdbc4"]
                     [swank-clojure "1.4.0-SNAPSHOT"]
                     [com.instrument.triface/triface-action-adapter "1.0-SNAPSHOT"]
                     [geocoder-clj "0.0.3"]
                     [lein-eclipse "1.0.0"]
                     [clj-logging-config "1.7.0"]
                     [log4j "1.2.16"]]
  :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
  :aot [triface.model]
  :ring {:handler triface.api/app
         :servlet-name "triface"
         :init triface.api/init}
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"})
