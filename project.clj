(defproject nsorg-cli "0.3.1"
  :description "Command line tool for organizing ns form"
  :url "https://github.com/immoh/nsorg-cli"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[clojure-term-colors "0.1.0"]
                 [nsorg "0.2.0"]
                 [org.clojure/tools.cli "0.4.2"]]
  :profiles {:dev {:dependencies [[midje "1.9.9"]
                                  [org.clojure/clojure "1.10.1"]]
                   :plugins      [[lein-midje "3.2.1"]]}})
