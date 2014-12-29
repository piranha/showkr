#!/usr/bin/env boot

(set-env!
  :source-paths #{"src"}
  :resource-paths #{"resources/public"}
  :dependencies '[[adzerk/boot-cljs "0.0-2411-7" :scope "test"]
                  [adzerk/boot-cljs-repl "0.1.7" :scope "test"]
                  [adzerk/boot-reload "0.2.2" :scope "test"]
                  [pandeiro/boot-http "0.3.0" :scope "test"]

                  [org.webjars/bootstrap "2.3.2"]
                  [deraen/boot-less "0.2.0" :scope "test"]

                  [cljsjs/react "0.12.2-1"]
                  [cljsjs/boot-cljsjs "0.3.0" :scope "test"]

                  [quiescent "0.1.4"]
                  [datascript "0.7.2"]
                  [keybind "1.0.0"]])

(task-options!
  pom {:project 'showkr
       :version "0.1.0-SNAPSHOT"})

(require
  '[adzerk.boot-cljs :refer [cljs]]
  '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
  '[adzerk.boot-reload :refer [reload]]
  '[pandeiro.http :refer [serve]]
  '[deraen.boot-less :refer [less]]
  '[cljsjs.app :refer [from-cljsjs]])

(deftask dev
  "Start development environment"
  []
  (comp (serve :dir "target"
               :port 3000)
        (from-cljsjs :target "public")
        (watch)
        (speak)
        (reload :on-jsload 'showkr/trigger-render)
        (cljs-repl)
        (cljs :source-map true
              :optimizations :none
              :unified-mode true)
        (less :source-map true)
        (sift :exclude #{"\\.less$"})))

(deftask prod
  "Compile production version"
  []
  (set-env! :target-path "prod")
  (comp
    (from-cljsjs :target "public"
                 :profile :production)
    (cljs :source-map false
          :optimizations :advanced)
    (less :compression true)
    (sift :exclude #{"\\.less$" "^cljsjs/"})))
