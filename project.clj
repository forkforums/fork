(defproject fork "0.1.0-SNAPSHOT"
  :main fork.core
  :description ""
  :license {:name "AGPL-3.0"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [ring/ring-core "1.12.1"]
                 [ring/ring-jetty-adapter "1.12.1"]
                 [metosin/reitit "0.7.0"]
                 [metosin/reitit-ring "0.7.0"]
                 [cheshire/cheshire "5.12.0"]
                 [clj-http/clj-http "3.12.3"]]
  :source-paths ["src"])
