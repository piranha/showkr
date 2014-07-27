(ns showkr.main
  (:require [quiescent :as q :include-macros true]
            [quiescent.dom :as d]

            [showkr.data :as data]
            [showkr.utils :refer [get-route]]
            [showkr.form :refer [Form]]
            [showkr.viewing :refer [Set]]
            [showkr.browsing :refer [User]]))


(q/defcomponent Root
  [{:keys [path sets users] :as data}]
  (cond
    (= path "about")
    (d/div nil "about")

    (re-matches #"^user/.*" path)
    (let [username (.slice path 5)]
      (User {:username username :user (get users username)}))

    (re-matches #"^\d+$" path)
    (Set {:id path :set (get sets path)})

    (re-matches #"^\d+/\d+$" path)
    (let [[set-id scroll-id] (.split path "/")]
      (Set {:id set-id :set (get sets set-id)
            :scroll-id scroll-id}))

    :else
    (Form (:form data)
      (fn
        ([v] (swap! data/world assoc :form v))
        ([k v] (swap! data/world assoc-in [:form k] v))))))

(defn render []
  (js/console.log "rendering" (clj->js @data/world))
  (q/render (Root @data/world)
    (.getElementById js/document (:target @data/world))))

(defn ^:export main [id]
  (add-watch data/world ::main render)
  (swap! data/world assoc
    :target id
    :path (get-route))
  (.addEventListener js/window "hashchange"
    #(swap! data/world assoc :path (get-route))))
