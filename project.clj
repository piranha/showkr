(defproject showkr "0.1.0-SNAPSHOT"
  :description "Showkr"
  :url "http://showkr.solovyov.net/"

  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-figwheel "0.1.3-SNAPSHOT"]]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2280"]
                 [com.facebook/react "0.11.1"]
                 [quiescent "0.1.4"]
                 [keybind "0.1.0"]
                 [figwheel "0.1.3-SNAPSHOT"]]

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
                         :externs ["react/externs/react.js"]}}]}

  :figwheel {
    :port 8888
    :css-dirs ["resources/public"]
  })
