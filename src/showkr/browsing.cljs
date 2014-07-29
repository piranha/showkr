(ns showkr.browsing
  (:require [quiescent :as q :include-macros true]
            [quiescent.dom :as d]

            [showkr.data :as data]
            [showkr.ui :as ui]
            [showkr.viewing :refer [photo-url]]))


(q/defcomponent Set
  [{:keys [id title date_create photos description] :as set}]
  (d/div {:className "row"}
    (d/div {:className "span6"}
      (d/h3 nil
        (d/a {:href (str "#" id)} (:_content title)))
      (d/small nil (:_content description))
      (d/dl nil
        (d/dt nil "Created at")
        (d/dd nil (ui/date date_create))
        (d/dt nil "In total")
        (d/dd nil photos)))
    (d/div {:className "span6"}
      (d/a {:href (str "#" id)}
        (d/img {:src (photo-url (assoc set :id (:primary set)) :medium)})))))

(q/defcomponent User
  [{:keys [username user]}]
  (q/wrapper
    (apply d/div nil
      (d/h1 nil
        "Sets of "
        (d/a {:href (str "https://flickr.com/photos/" username)} username))
      (when (= :fetched (-> user :sets meta :state))
        (for [set (-> user :sets :photoset)]
          (Set set))))
    :onMount #(data/fetch-user username)))
