#!/usr/bin/env boot

(set-env!
  :source-paths #{"src" "style"}
  :resource-paths #{"resources/public"}
  :dependencies '[[org.clojure/clojure "1.7.0"]

                  [adzerk/boot-cljs "0.0-3308-0"]
                  [adzerk/boot-cljs-repl "0.1.9"]
                  [adzerk/boot-reload "0.3.1"]
                  [pandeiro/boot-http "0.6.2"]

                  [org.webjars/bootstrap "2.3.2"
                   :exclusions [org.webjars/jquery]]
                  [deraen/boot-less "0.4.0"]

                  [org.clojure/clojurescript "0.0-3308" :classifier "aot"
                   :exclusions [org.clojure/tools.reader
                                org.clojure/data.json]]
                  [org.clojure/tools.reader "0.9.2" :classifier "aot"]
                  [org.clojure/data.json "0.2.6" :classifier "aot"]
                  [org.clojure/tools.nrepl "0.2.10"]

                  [cljsjs/react-with-addons "0.12.2-8"]
                  [quiescent "0.1.4"
                   :exclusions [org.clojure/clojurescript
                                org.clojure/clojure]]
                  [datascript "0.11.5"]
                  [keybind "1.0.0"]])

(task-options!
  pom {:project 'showkr
       :version "0.1.1"})

(require
  '[adzerk.boot-cljs :refer [cljs]]
  '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
  '[adzerk.boot-reload :refer [reload]]
  '[pandeiro.boot-http :refer [serve]]
  '[deraen.boot-less :refer [less]])

(deftask dev
  "Start development environment"
  []
  (comp (serve :dir "target"
               :port 3000)
        (watch)
        (speak)
        (reload :on-jsload 'showkr/trigger-render)
        (cljs-repl)
        (cljs :source-map true
              :optimizations :none
              :compiler-options {:warnings {:single-segment-namespace false}})
        (less :source-map true)))

(deftask prod
  "Compile production version"
  []
  (set-env! {:target-path "prod"})
  (comp
    (cljs :source-map false
          :optimizations :advanced
          :compiler-options {:warnings {:single-segment-namespace false}})
    (less :compression true)))
