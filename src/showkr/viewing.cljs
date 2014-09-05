(ns showkr.viewing
  (:require [goog.style :as style]

            [quiescent :as q :include-macros true]
            [quiescent.dom :as d]
            [keybind :as key]
            [datascript :as db]

            [showkr.utils :as u :refer-macros [p]]
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
  (defn photo-url [photo size-name]
    (apply u/fmt "http://farm%s.staticflickr.com/%s/%s_%s_%s.jpg"
      (-> ((juxt :photo/farm :photo/server :photo/id :photo/secret) photo)
        (conj (size-name sizes))))))

(defn flickr-url [photo set-id]
  (u/fmt "http://www.flickr.com/photos/%s/%s/in/set-%s/"
    (or (:photo/path-alias photo) (-> photo :photo/set first :owner))
    (:photo/id photo)
    set-id))

(defn comment-avatar [comment]
  (if (= (:icon/server comment) "0")
    "http://www.flickr.com/images/buddyicon.jpg"
    (apply u/fmt "http://farm%s.static.flickr.com/%s/buddyicons/%s.jpg"
      ((juxt :icon/farm :icon/server :comment/author) comment))))


(q/defcomponent Comment
  [comment]
  (d/li {:className "comment"}
    (d/div {:className "avatar"}
      (d/img {:src (comment-avatar comment)}))

    (d/div nil
      (d/a {:href (str "http://flickr.com/photos/"
                    (or (:comment/path-alias comment) (:comment/author comment)))}
        (:comment/author-name comment))
      (d/a {:href (:comment/link comment) :className "anchor"}
        (ui/date (:comment/date comment)))
      (d/div {:className "content"
              :dangerouslySetInnerHTML (js-obj "__html" (:content comment))}))))

(q/defcomponent CommentList
  [{:keys [state comments]}]
  (case state
    nil
    (d/div {:className "span4 comments"})

    :waiting
    (d/div {:className "span4 comments"})

    :fetched
    (d/div {:className "span4 comments"}
      (apply d/ul {:className "comments"}
        (for [comment (sort-by :comment/order comments)]
          (Comment comment)))
      (d/ul {:className "pager"}))))

(q/defcomponent Photo
  [{:keys [db photo scroll-id set-id]}]
  (let [comments (:comment/_photo photo)
        upd (fn [node]
              (data/fetch-comments photo)
              (if (= (:photo/id photo) scroll-id)
                (scroll-to node)))]
    (q/wrapper
      (d/div nil
        (d/h3 nil (str (inc (:photo/order photo)) ". " (:title photo) " ")
          (d/a {:className "anchor"
                :href (u/fmt "#%s/%s" set-id (:photo/id photo))} "#"))

        (d/small {:rel "description"} (:description photo))

        (d/div {:className "row"}
          (d/div {:className "span8"}
            (d/a {:href (flickr-url photo set-id)}
              (d/img {:src (photo-url photo :medium)})))
          (when comments
            (CommentList {:state (:photo/comment-state photo)
                          :comments comments}))))
      :onMount upd
      :onUpdate upd)))

(defn bind-controls! [set-id current-id]
  (key/bind! "j" ::next #(data/watch-next set-id current-id))
  (key/bind! "down" ::next #(data/watch-next set-id current-id))
  (key/bind! "space" ::next #(data/watch-next set-id current-id))
  (key/bind! "k" ::prev #(data/watch-prev set-id current-id))
  (key/bind! "up" ::prev #(data/watch-prev set-id current-id))
  (key/bind! "shift-space" ::prev #(data/watch-prev set-id current-id)))

(defn unbind-controls! []
  (key/unbind! "j" ::next)
  (key/unbind! "down" ::next)
  (key/unbind! "space" ::next)
  (key/unbind! "k" ::prev)
  (key/unbind! "up" ::prev)
  (key/unbind! "shift-space" ::prev))

(q/defcomponent Set
  [{:keys [db id scroll-id]}]
  (let [set (data/by-attr db {:set/id id})
        upd (fn []
              (data/fetch-set id)
              (bind-controls! id scroll-id))]

    (q/wrapper
      (case (:showkr/state set)
        :fetched
        (apply d/div nil
          (if (:title set)
            (d/h1 nil
              (d/span {:rel "title"} (:title set))))
          (d/small {:rel "description"} (:description set))

          (map
            #(Photo {:photo %
                     :db db
                     :set-id id
                     :scroll-id scroll-id})
            (sort-by :photo/order (:photo/_set set))))

        :waiting
        (ui/spinner)

        (d/div {:className "alert alert-error"}
          "It seems that set "
          (d/b nil id)
          " does not exist. Go to "
          (d/a {:href "#"} "index page.")))

      :onMount upd
      :onUpdate upd
      :onWillUnmount unbind-controls!)))
