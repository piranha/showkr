(ns showkr
  (:require [cljs.reader :refer [read-string]]

            [quiescent :as q :include-macros true]
            [quiescent.dom :as d]

            [showkr.data :as data]
            [showkr.utils :refer [get-route]]
            [showkr.root :refer [Root]]))

(def ^:private render-queued false)
(defn ^:private actually-render []
  (set! render-queued false)
  (q/render (Root {:opts @data/opts :db @data/db})
    (.getElementById js/document (:target @data/opts))))

(defn render []
  (when-not render-queued
    (set! render-queued true)
    (if (exists? js/requestAnimationFrame)
      (js/requestAnimationFrame actually-render)
      (js/setTimeout actually-render 16))))

(defn ^:export main [id opts]
  (let [opts (js->clj opts :keywordize-keys true)]

    ;; listen for data changes
    (add-watch data/opts ::render render)
    (add-watch data/db ::render render)

    #_(add-watch data/world ::store
      (fn []
        (binding [*print-meta* true]
          (.setItem js/window.localStorage "data" (pr-str (:data @data/world))))))

    ;; listen for path changes
    (.addEventListener js/window "hashchange"
      #(let [path (get-route)
             path (if (empty? path) (:path opts "") path)]
         (swap! data/opts assoc :path path)))

    ;; kick off rendering
    (let [stored (.getItem js/window.localStorage "data")]
      (swap! data/opts merge
        {:target id :path (get-route)}
        (or opts {})))))
