(ns showkr.viewing
  (:require [quiescent :as q :include-macros true]
            [quiescent.dom :as d]

            [showkr.data :as data]
            [showkr.ui :as ui]))


;; s  small square 75x75
;; t  thumbnail, 100 on longest side
;; m  small, 240 on longest side
;; -  medium, 500 on longest side
;; z  medium 640, 640 on longest side
;; b  large, 1024 on longest side*
(let [sizes {:small "m" :medium "z" :big "b"}]
  (defn photo-url [{:keys [farm server id secret] :as photo} size-name]
    (let [size (size-name sizes)]
      (str "http://farm" farm ".staticflickr.com/" server
           "/" id "_" secret "_" size ".jpg"))))

(defn flickr-url [{:keys [id set-id pathalias owner] :as photo}]
  (str "http://www.flickr.com/photos/" (or pathalias owner)
       "/" id "/in/set-" set-id "/"))

(defn flickr-avatar [{:keys [author iconfarm iconserver] :as comment}]
  (if (= iconserver "0")
    "http://www.flickr.com/images/buddyicon.jpg"
    (str "http://farm" iconfarm ".static.flickr.com/" iconserver
         "/buddyicons/" author ".jpg")))

(q/defcomponent Comment
  [{:keys [author authorname permalink datecreate _content] :as comment}]
  (d/li {:className "comment"}
    (d/div {:className "avatar"}
      (d/img {:src (flickr-avatar comment)}))

    (d/div nil
      (d/a {:href (str "http://flickr.com/photos/" author)} authorname)
      (d/a {:href permalink :className "anchor"} (ui/date datecreate)))
    _content))

(q/defcomponent CommentList
  [comments]
  (condp = (:state comments)
    nil
    (d/div {:className "span4 comments"})

    :waiting
    (d/div {:className "span4 comments"})

    :fetched
    (d/div {:className "span4 comments"}
      (apply d/ul {:className "comments"}
        (for [comment (-> comments :data :comment)]
          (Comment comment)))
      (d/ul {:className "pager"}))))

(q/defcomponent Photo
  [{:keys [idx id set-id title description] :as photo}]
  (q/wrapper
    (d/div nil
      (d/h3 nil (str (inc idx) ". " title " ")
        #_ (d/a {:className "anchor" :href (str "#" set-id "/" id)} "#"))

      (d/small {:rel "description"} (:_content description))
      (d/div {:className "row"}
        (d/div {:className "span8"}
          (d/a {:href (flickr-url photo)}
            (d/img {:src (photo-url photo :medium)})))
        (CommentList (:comments photo))))
    :onMount (fn []
               (data/fetch-comments set-id idx))))

(q/defcomponent Set
  [{:keys [id set scroll-id]}]
  (q/wrapper
    (condp = (:state set)
      nil
      (d/div nil id)

      :waiting
      (d/div nil id)

      :fetched
      (apply d/div nil
        (if (-> set :data :title)
          (d/h1 nil
            (d/span {:rel "title"} (-> set :data :title))))
        (d/small {:rel "description"} (-> set :data :description))

        (map-indexed
          #(Photo (assoc %2 :idx %1
                            :set-id id
                            :owner (-> set :data :owner)))
          (-> set :data :photo))))

    :onMount (fn [node]
               (data/fetch-set id))
    :onUpdate (fn [node]
                (data/fetch-set id))))
