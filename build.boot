#!/usr/bin/env boot

(set-env!
  :source-paths #{"src"}
  :resource-paths #{"resources/public"}
  :dependencies '[[adzerk/boot-cljs "0.0-2411-5" :scope "test"]
                  [adzerk/boot-cljs-repl "0.1.7" :scope "test"]
                  [adzerk/boot-reload "0.2.0" :scope "test"]
                  [pandeiro/boot-http "0.3.0" :scope "test"]
                  [com.facebook/react "0.11.1"]
                  [quiescent "0.1.4"]
                  [datascript "0.7.1"]
                  [keybind "0.1.0"]])

(task-options!
  pom {:project 'showkr
       :version "0.1.0-SNAPSHOT"})

(require
  '[adzerk.boot-cljs :refer [cljs]]
  '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
  '[adzerk.boot-reload :refer [reload]]
  '[pandeiro.http :refer [serve]])

;; (deftask dev
;;   "Development environment"
;;   []
;;   (comp (watch)
;;         (cljs-repl)
;;         (cljs :source-map true
;;               :optimizations :none
;;               :unified true)
;;         (reload :on-jsload 'showkr/trigger-render)))

;; (deftask prod
;;   "Production version"
;;   []
;;   (comp
;;     (cljs :source-map false
;;           :optimizations :advanced)))
