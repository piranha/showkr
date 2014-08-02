(ns showkr.browsing
  (:require [quiescent :as q :include-macros true]
            [quiescent.dom :as d]

            [showkr.utils :refer-macros [p]]
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
  [{:keys [username user hide-title]}]
  (q/wrapper
    (condp = (-> user :sets meta :state)
      :fetched
      (apply d/div nil
        (when-not hide-title
          (d/h1 nil
            "Sets of "
            (d/a {:href (str "https://flickr.com/photos/" username)} username)))
        (for [set (-> user :sets :photoset)]
          (Set set)))

      :waiting
      (ui/spinner)

      (d/div {:className "alert alert-error"}
        "Could not fetch user "
        (d/b nil username)
        ". It could be an error or it just does not exist. Go to "
        (d/a {:href "#"} "index page.")))
    :onMount #(data/fetch-user username)))
