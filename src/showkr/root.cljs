(ns showkr.root
  (:require [quiescent :as q :include-macros true]
            [quiescent.dom :as d]
            [datascript :as db]

            [showkr.data :as data]
            [showkr.form :refer [Form]]
            [showkr.viewing :refer [Set]]
            [showkr.browsing :refer [User]]))

(defn simple-dict [db]
  (let [eid (data/transact->eid! db {:db/id -1})
        getter #(into {} (db/entity @db eid))]
    [getter
     (fn setter
       ([v]
        (let [data (dissoc (getter) :db/id)]
          (db/transact! db (for [[k v] data]
                             [:db/retract eid k v]))))
       ([k v] (db/transact! db [[:db/add eid k v]])))]))

(let [[getter setter] (simple-dict data/db)]
  (q/defcomponent Root
    [{db :db, {:keys [path hide-title debug]} :opts}
     toggle-debug]
    (d/div {:className "container"}
      (when-not hide-title
        (d/header nil
          (d/h1 nil (d/a {:href "#"} "Showkr"))))

      (d/div {:className "row"}
        (d/div {:className "span12"}
          (cond
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

      (when-not hide-title
        (d/footer nil
          (d/p {:className "pull-right"}
            (when debug
              (str "Stats: db size - "
                (count (:eavt db)) " datoms, "
                (count (pr-str db)) "b"))
            (d/a {:href "#"
                  :onClick #(do (.preventDefault %)
                                (toggle-debug))}
              " Toggle debug"))
          (d/p nil
            "© 2012-2014 "
            (d/a {:href "http://solovyov.net"} "Alexander Solovyov")))))))
