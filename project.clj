(defproject triface "1.0.0-SNAPSHOT"
  :description "Interface: type structure interaction api"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/java.jdbc "0.0.6"]
                 [postgresql/postgresql "8.4-702.jdbc4"]
                 [compojure "0.6.4"]
                 [hiccup "0.3.6"]
                 [clojure-solr "0.2.0"]
                 [org.clojure/data.json "0.1.1"]
                 [org.clojars.ninjudd/data.xml "0.0.1-SNAPSHOT"]
                 [ring/ring-jetty-adapter "0.3.10"]
                 [com.instrument.triface/triface-action-adapter "1.0-SNAPSHOT"]
                 [clojure-csv/clojure-csv "1.3.2"]]
  :dev-dependencies [[lein-ring "0.4.5"]
                     [org.clojure/java.jdbc "0.0.6"]
                     [postgresql/postgresql "8.4-702.jdbc4"]
                     [swank-clojure "1.4.0-SNAPSHOT"]
                     [clojure-source "1.2.0"]
                     [com.instrument.triface/triface-action-adapter "1.0-SNAPSHOT"]
                     [lein-eclipse "1.0.0"]]
  :java-source-path [["src/java"]
                     ["test/java" :debug true]]
  :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
  :ring {:handler triface.core/app
         :servlet-name "triface"
         :init triface.core/init}
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"})
