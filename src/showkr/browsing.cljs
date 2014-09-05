(ns showkr.browsing
  (:require [quiescent :as q :include-macros true]
            [quiescent.dom :as d]
            [datascript :as db]

            [showkr.utils :refer-macros [p]]
            [showkr.data :as data]
            [showkr.ui :as ui]
            [showkr.viewing :refer [photo-url]]))

(q/defcomponent Set
  [set]
  (d/div {:className "row"}
    (d/div {:className "span6"}
      (d/h3 nil
        (d/a {:href (str "#" (:userset/id set))} (:title set)))
      (d/small nil (:description set))
      (d/dl nil
        (d/dt nil "Created at")
        (d/dd nil (ui/date (:date/create set)))
        (d/dt nil "In total")
        (d/dd nil (:set/total set))))
    (d/div {:className "span6"}
      (d/a {:href (str "#" (:userset/id set))}
        (d/img {:src (photo-url set :medium)})))))

(q/defcomponent User
  [{:keys [db login hide-title]}]
  (let [user (data/by-attr db {:user/login login})]
    (q/wrapper
      (case (:showkr/state user)
        :fetched
        (apply d/div nil
          (when-not hide-title
            (d/h1 nil
              "Sets of "
              (d/a {:href (str "https://flickr.com/photos/" login)} (:user/name user))))
          (for [set (:userset/_user user)]
            (Set set)))

        :waiting
        (ui/spinner)

        :not-exists
        (d/div {:className "alert alert-error"}
          "Could not fetch user "
          (d/b nil login)
          ". It could be an error or it just does not exist. Go to "
          (d/a {:href "#"} "index page."))

        (d/noscript))
      :onMount #(data/fetch-user login))))
