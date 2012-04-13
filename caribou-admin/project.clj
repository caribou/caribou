(defproject antler/caribou-admin "0.2.0"
  :description "Flexible and adaptive admin for caribou-api"
  :url "http://github.com/antler/caribou-admin"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[antler/caribou-api "0.2.0"]]
  :ring {:handler caribou.admin.core/app
         :servlet-name "caribou-admin"
         :init caribou.admin.core/init
         :port 33553})

