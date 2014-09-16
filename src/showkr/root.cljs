(ns showkr.root
  (:require [quiescent :as q :include-macros true]
            [quiescent.dom :as d]
            [datascript :as db]

            [showkr.data :as data]
            [showkr.form :refer [Form]]
            [showkr.viewing :refer [Set]]
            [showkr.browsing :refer [User]]))

(defn get-form [db name]
  (let [form-id (-> (db/transact! db [{:db/id -1 :form/data {} :form/name name}])
                  :tempids
                  (get -1))
        getter (fn []
                 (:form/data (db/entity @db form-id)))]
    [getter
     (fn setter
       ([v] (db/transact! db [[:db/add form-id :form/data v]]))
       ([k v] (setter (assoc (getter) k v))))]))

(let [[getter setter] (get-form data/db :main-form)]
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
