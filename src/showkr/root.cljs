(ns showkr.root
  (:require [quiescent :as q :include-macros true]
            [quiescent.dom :as d]

            [showkr.data :as data]
            [showkr.form :refer [Form]]
            [showkr.viewing :refer [Set]]
            [showkr.browsing :refer [User]]))

(q/defcomponent Root
  [{:keys [data db opts]}]
  (let [{:keys [path hide-title]} opts]
    (cond
      (= path "about")
      (d/div nil "about")

      (re-matches #"^user/.*" path)
      (let [username (.slice path 5)]
        (User {:username username
               :user (get-in data [:users username])
               :hide-title hide-title}))

      (re-matches #"^\d+(/(\d+)?)?$" path)
      (let [[set-id scroll-id] (.split path "/")]
        (Set {:db db
              :id set-id
              :scroll-id scroll-id}))

      :else
      (Form data
        (fn
          ([v] (swap! data/world assoc-in [:data :form] v))
          ([k v] (swap! data/world assoc-in [:data :form k] v)))))))
