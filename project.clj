(defproject triface "1.0.0-SNAPSHOT"
  :description "Interface: type structure interaction api"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.clojure/java.jdbc "0.0.3-SNAPSHOT"]
                 [postgresql/postgresql "8.4-702.jdbc4"]
                 [compojure "0.6.4"]
                 [ring/ring-jetty-adapter "0.3.10"]]
  :dev-dependencies [[lein-ring "0.4.5"]
                     [org.clojure/java.jdbc "0.0.3-SNAPSHOT"]
                     [postgresql/postgresql "8.4-702.jdbc4"]]
  :ring {:handler triface.core/app
         :servlet-name "triface"
         :init triface.core/init}
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  )
