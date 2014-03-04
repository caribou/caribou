(defproject caribou "0.12.0"
  :description "Documentation generator"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [markdown-clj "0.9.41"]]
  :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
  :main caribou.md
  :source-paths ["src"]
  :resource-paths ["resources/"])
