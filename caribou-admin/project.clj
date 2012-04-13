(defproject antler/caribou-admin "0.1.0-SNAPSHOT"
  :description "Flexible and adaptive admin for caribou-api"
  :url "http://github.com/antler/caribou-admin"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[antler/caribou-core "0.4.4-SNAPSHOT"]]
  :ring {:handler caribou.admin.core/app
         :servlet-name "caribou-admin"
         :init caribou.admin.core/init})

