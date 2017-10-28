(defproject shrimp-log "0.1.0-SNAPSHOT"
  :description "A tiny ClojureScript logging library for Node.js."
  :url "https://github.com/pepzer/shrimp-log"
  :license {:name "Mozilla Public License Version 2.0"
            :url "http://mozilla.org/MPL/2.0/"}

  :min-lein-version "2.7.1"

  :dependencies [[org.clojars.pepzer/redlobster "0.2.2"]
                 [shrimp "0.1.1-SNAPSHOT"]]

  :plugins [[lein-figwheel "0.5.13"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]

  :clean-targets ^{:protect false} ["target"]

  :source-paths ["src/cljs" "src/cljc" "test/cljs"]

  :cljsbuild {:test-commands {"node" ["node" "target/out-test/shrimp-log.js"]}
              :builds [{:id "dev"
                        :source-paths ["src/cljs" "src/cljc" "test/cljs"]
                        :figwheel true
                        :compiler {:main shrimp-log.dev
                                   :output-to "target/out/shrimp-log.js"
                                   :output-dir "target/out"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true }}
                       {:id "test-all"
                        :source-paths ["src/cljs" "src/cljc" "test/cljs"]
                        :compiler {:main shrimp-log.async-tests
                                   :output-to "target/out-test/shrimp-log.js"
                                   :output-dir "target/out-test"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true }}
                       #_{:id "prod"
                        :source-paths ["src/cljs" "src/cljc" "test/cljs"]
                        :compiler {:output-to "target/out-rel/shrimp-log.js"
                                   :output-dir "target/out-rel"
                                   :target :nodejs
                                   :optimizations :simple
                                   :source-map false }}]}

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/clojurescript "1.9.946"]
                                  [org.clojure/clojure "1.9.0-beta1"]]}}
  :figwheel {})

