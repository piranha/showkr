(ns showkr
  (:require [quiescent :as q :include-macros true]
            [quiescent.dom :as d]

            [showkr.data :as data]
            [showkr.utils :refer [get-route]]
            [showkr.form :refer [Form]]
            [showkr.viewing :refer [Set]]
            [showkr.browsing :refer [User]]))


(q/defcomponent Root
  [{:keys [path hide-title sets users] :as data}]
  (cond
    (= path "about")
    (d/div nil "about")

    (re-matches #"^user/.*" path)
    (let [username (.slice path 5)]
      (User {:username username :user (get users username) :hide-title hide-title}))

    (re-matches #"^\d+(/(\d+)?)?$" path)
    (let [[set-id scroll-id] (.split path "/")]
      (Set {:id set-id
            :set (get sets set-id)
            :scroll-id scroll-id}))

    :else
    (Form (:form data)
      (fn
        ([v] (swap! data/world assoc :form v))
        ([k v] (swap! data/world assoc-in [:form k] v))))))

(def ^:private render-queued false)
(defn render []
  (js/console.log "rendering" (clj->js @data/world))
  (set! render-queued false)
  (q/render (Root @data/world)
    (.getElementById js/document (:target @data/world))))

(defn ^:export main [id initial]
  (let [initial (js->clj initial :keywordize-keys true)]
    ;; listen for data changes
    (add-watch data/world ::main
      (fn []
        (when-not render-queued
          (set! render-queued true)
          (if (exists? js/requestAnimationFrame)
            (js/requestAnimationFrame render)
            (js/setTimeout render 16)))))

    ;; listen for path changes
    (.addEventListener js/window "hashchange"
      #(let [path (get-route)
             path (if (empty? path) (:path initial "") path)]
         (swap! data/world assoc :path path)))

    ;; kick off rendering
    (swap! data/world merge
      {:target id :path (get-route)}
      (or initial {}))))
