(defproject martian-robots "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :main martian-robots.core
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.trace "0.7.9"]]
  :profiles
    {:dev
     {:source-paths ["dev" "src" "test"]
       :dependencies [[org.clojure/tools.namespace "0.2.11"]]}})
