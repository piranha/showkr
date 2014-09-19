(defproject showkr "0.1.0-SNAPSHOT"
  :description "Showkr"
  :url "http://showkr.solovyov.net/"

  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-figwheel "0.1.3-SNAPSHOT"]
            [lein-npm "0.4.0"]]

  :dependencies [[org.clojure/clojurescript "0.0-2342"]
                 [com.facebook/react "0.11.1"]
                 [quiescent "0.1.4"]
                 [datascript "0.4.1"]
                 [keybind "0.1.0"]]

  :profiles {:dev {:dependencies [[figwheel "0.1.3-SNAPSHOT" :exclusions [org.clojure/core.async]]
                                  [medley "0.5.0" :exclusions [org.clojure/clojure]]
                                  [org.clojure/clojure "1.6.0"]
                                  [org.clojure/core.async "0.1.338.0-5c5012-alpha"]]}}

  :node-dependencies [[less "1.4.2"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src/" "src-dev/"]
              :compiler {:output-to "resources/public/js/showkr.js"
                         :output-dir "resources/public/js/out"
                         :optimizations :none
                         :source-map true}}
             {:id "min"
              :source-paths ["src"]
              :compiler {
                         :output-to "www/showkr.min.js"
                         :optimizations :advanced
                         :pretty-print false
                         :preamble ["react/react.min.js"]
                         :externs ["react/externs/react.js"
                                   "datascript/externs.js"]}}]}

  :figwheel {
    :server-port 8888
    :css-dirs ["resources/public"]
  })
