(ns showkr.viewing
  (:require [goog.style :as style]

            [quiescent :as q :include-macros true]
            [quiescent.dom :as d]
            [keybind :as key]
            [datascript :as db]

            [showkr.utils :refer-macros [p]]
            [showkr.data :as data]
            [showkr.ui :as ui]))


(defn scroll-to [el]
  (js/scroll 0 (style/getPageOffsetTop el)))

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
      (d/a {:href permalink :className "anchor"} (ui/date datecreate))
      (d/div {:className "content"
              :dangerouslySetInnerHTML (js-obj "__html" _content)}))))

(q/defcomponent CommentList
  [comments]
  (condp = (:state (meta comments))
    nil
    (d/div {:className "span4 comments"})

    :waiting
    (d/div {:className "span4 comments"})

    :fetched
    (d/div {:className "span4 comments"}
      (apply d/ul {:className "comments"}
        (for [comment (:comment comments)]
          (Comment comment)))
      (d/ul {:className "pager"}))))

(q/defcomponent Photo
  [{:keys [db photo-id idx scroll-id set-id owner] :as photo}]
  (let [photo (db/entity db photo-id)
        upd (fn [node]
              #_(data/fetch-comments set-id idx)
              (if (= (:id photo) scroll-id)
                (scroll-to node)))]
    (q/wrapper
      (d/div nil
        (d/h3 nil (str (inc idx) ". " (:title photo) " ")
          (d/a {:className "anchor" :href (str "#" set-id "/" photo-id)} "#"))

        (d/small {:rel "description"} (:description photo))
        (d/div {:className "row"}
          (d/div {:className "span8"}
            (d/a {:href (flickr-url (assoc photo :owner owner))}
              (d/img {:src (photo-url photo :medium)})))
          #_ (CommentList (:comments photo))))
      :onMount upd
      :onUpdate upd)))

(defn bind-controls! [set current]
  (key/bind! "j" ::next #(data/watch-next set current))
  (key/bind! "down" ::next #(data/watch-next set current))
  (key/bind! "space" ::next #(data/watch-next set current))
  (key/bind! "k" ::prev #(data/watch-prev set current))
  (key/bind! "up" ::prev #(data/watch-prev set current))
  (key/bind! "shift-space" ::prev #(data/watch-prev set current)))

(defn unbind-controls! []
  (key/unbind! "j" ::next)
  (key/unbind! "down" ::next)
  (key/unbind! "space" ::next)
  (key/unbind! "k" ::prev)
  (key/unbind! "up" ::prev)
  (key/unbind! "shift-space" ::prev))

(q/defcomponent Set
  [{:keys [db id scroll-id]}]
  (let [set (data/by-id db id)]
    (q/wrapper
      (condp = (:showkr/state set)
        :fetched
        (apply d/div nil
          (if (:title set)
            (d/h1 nil
              (d/span {:rel "title"} (:title set))))
          (d/small {:rel "description"} (:description set))

          (map-indexed
            #(Photo {:db db
                     :idx %1
                     :photo-id %2
                     :set-id id
                     :scroll-id scroll-id
                     :owner (:owner set)})
            (:photo set)))

        :waiting
        (ui/spinner)

        (d/div {:className "alert alert-error"}
          "It seems that set "
          (d/b nil id)
          " does not exist. Go to "
          (d/a {:href "#"} "index page.")))

      :onMount (fn [node]
                 (data/fetch-set-db id)
                 (bind-controls! set scroll-id))
      :onUpdate (fn [node]
                  (data/fetch-set-db id)
                  (bind-controls! set scroll-id))
      :onWillUnmount (fn []
                       (unbind-controls!)))))
