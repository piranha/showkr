(ns showkr.root
  (:require [quiescent :as q :include-macros true]
            [quiescent.dom :as d]
            [datascript :as db]

            [showkr.data :as data]
            [showkr.form :refer [Form]]
            [showkr.viewing :refer [Set]]
            [showkr.browsing :refer [User]]))

(defn simple-dict [db attr]
  (let [eid (-> (db/transact! db [{:db/id -1 attr {}}])
              :tempids
              (get -1))
        getter (fn []
                 (attr (db/entity @db eid)))]
    [getter
     (fn setter
       ([v] (db/transact! db [[:db/add eid attr v]]))
       ([k v] (setter (assoc (getter) k v))))]))

(let [[getter setter] (simple-dict data/db :form/data)]
  (q/defcomponent Root
    [{db :db, {:keys [path hide-title]} :opts}]
    (cond
      (= path "about")
      (d/div nil "about")

      (re-matches #"^user/.*" path)
      (let [login (.slice path 5)]
        (User {:db db
               :login login
               :hide-title hide-title}))

      (re-matches #"^\d+(/(\d+)?)?$" path)
      (let [[set-id scroll-id] (.split path "/")]
        (Set {:db db
              :id set-id
              :scroll-id scroll-id}))

      :else
      (Form {:form (getter) :db db} setter))))
