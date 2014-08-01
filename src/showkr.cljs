(ns showkr
  (:require [cljs.reader :refer [read-string]]

            [quiescent :as q :include-macros true]
            [quiescent.dom :as d]

            [showkr.data :as data]
            [showkr.utils :refer [get-route]]
            [showkr.root :refer [Root]]))

(def ^:private render-queued false)
(defn render []
  (js/console.log "rendering" (clj->js @data/world))
  (set! render-queued false)
  (q/render (Root @data/world)
    (.getElementById js/document (-> @data/world :opts :target))))

(defn ^:export main [id opts]
  (let [opts (js->clj opts :keywordize-keys true)]

    ;; listen for data changes
    (add-watch data/world ::render
      (fn []
        (when-not render-queued
          (set! render-queued true)
          (if (exists? js/requestAnimationFrame)
            (js/requestAnimationFrame render)
            (js/setTimeout render 16)))))

    ;; listen for path changes
    (.addEventListener js/window "hashchange"
      #(let [path (get-route)
             path (if (empty? path) (:path opts "") path)]
         (swap! data/world assoc-in [:opts :path] path)))

    ;; kick off rendering
    (swap! data/world update-in [:opts] merge
      {:target id :path (get-route)}
      (or opts {}))))
