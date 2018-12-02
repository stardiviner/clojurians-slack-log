(defproject clojurians-slack-log "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 ;; HTTP
                 [clj-http "2.2.0"]
                 ;; HTML
                 [enlive "1.1.6"]
                 ;; Async
                 [org.clojure/core.async "0.4.490"]
                 ;; Date
                 [clojure.java-time "0.3.2"]
                 ;; Redis
                 [com.taoensso/carmine "2.18.1"]
                 ;; JDBC
                 [org.clojure/java.jdbc "0.7.8"]
                 ;; SQLite
                 [org.xerial/sqlite-jdbc "3.23.1"]
                 ;; MongoDB
                 [congomongo "1.0.1"]
                 [com.novemberain/monger "3.1.0"]
                 ;; Analysis
                 [incanter/incanter "1.9.3"]
                 ;; Dependency
                 [com.cemerick/pomegranate "1.0.0"] ; add-dependencies
                 ])
