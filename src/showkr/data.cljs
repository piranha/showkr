(ns showkr.data
  (:import [goog.net Jsonp])

  (:require [datascript :as db]
            [showkr.utils :refer-macros [p]]))

(def URL "https://api.flickr.com/services/rest/")
(def OPTS {:api_key "1606ff0ad63a3b5efeaa89443fe80704"
           :format "json"})

(def ^:dynamic *old-threshold* (* 1000 60 60 2)) ;; 2 hours

(defonce world
  (atom {:opts {:target nil
                :path nil}
         :data {:form {}
                :sets {}
                :users {}}}))

;; datascript stuff

(defonce db (db/create-conn
  {:photo/set     {:db/cardinality :db.cardinality/many
                   :db/valueType :db.type/ref}
   :comment/photo {:db/valueType :db.type/ref}
   :set/user      {:db/valueType :db.type/ref}}))

;; (defonce prr (atom false))
;; (db/listen! db :test #(when @prr (js/console.log (pr-str (:tx-data %)))))

(defn only
  "Return the only item from a query result"
  [query-result]
  (assert (>= 1 (count query-result)))
  (assert (>= 1 (count (first query-result))))
  (ffirst query-result))

(defn qe
  "Returns the single entity returned by a query."
  [q db & args]
  (when-let [id (only (apply db/q q db args))]
    (db/entity db id)))

(defn by-attr [db attrmap]
  (let [q {:find '[?e] :where (mapv #(apply vector '?e %) attrmap)}]
    (qe q db)))

(defn transact->id! [db entity]
  (let [tempid (:db/id entity)
        tx (db/transact! db [entity])]
    (get (:tempids tx) tempid)))

;;; api utils

(defn- flickr-error [payload]
  (js/console.error "Error fetching data with parameters:" payload))

(defn flickr-call [payload callback]
  (let [q (Jsonp. URL "jsoncallback")]
    (.send q (clj->js (merge OPTS payload)) callback flickr-error)))

(defn -flickr-fetch [db db-id payload cb]
  (flickr-call payload
    (fn [data]
      (let [data (js->clj data :keywordize-keys true)]
        (case (:stat data)
          "ok"
          (cb db-id data)

          "fail"
          (when db-id
            (db/transact! db [[:db/add db-id :showkr/state :failed]])))))))

(defn flickr-fetch [db attrmap payload cb]
  (when-not (by-attr @db attrmap)
    (let [db-id (transact->id! db
                  (assoc attrmap :db/id -1 :showkr/state :waiting))]
      (-flickr-fetch db db-id payload cb))))

;;; helpers

(defn old? [data]
  (> (- (.getTime (js/Date.))
       (or (-> data meta :date) 0))
    *old-threshold*))

;;; converters

(defn set->local [db-id set]
  {:db/id db-id
   :showkr/state :fetched
   :set/id (set :id)

   :set/pages (set :pages)
   :set/page (set :page)
   :set/per-page (set :per_page)
   :set/total (set :total)

   :set/primary (set :primary)
   :set/owner (set :owner)

   :title (set :title)
   :description (set :description)})

(defn photo->local [set-id idx photo]
  {:db/id (- -1 idx)
   :showkr/state :fetched
   :photo/order idx
   :photo/id (photo :id)
   :photo/set [set-id]

   :photo/farm (photo :farm)
   :photo/server (photo :server)
   :photo/secret (photo :secret)
   :photo/original-secret (photo :originalsecret)
   :photo/original-format (photo :originalformat)
   :photo/path-alias (photo :pathalias)

   :title (photo :title)
   :description (-> photo :description :_content)})

(defn comment->local [photo-id idx comment]
  {:db/id (- -1 idx)
   :showkr/state :fetched
   :comment/order idx
   :comment/id (comment :id)
   :comment/photo photo-id

   :icon/farm (comment :iconfarm)
   :icon/server (comment :iconserver)
   :comment/date (-> comment :datecreate (js/parseInt 10) (* 1000) (js/Date.))
   :comment/author (comment :author)
   :comment/author-name (comment :authorname)
   :comment/real-name (comment :realname)
   :comment/path-alias (comment :path_alias)
   :comment/link (comment :permalink)

   :content (:_content comment)})

;;; data->db

(defn store-set! [db db-id set]
  (db/transact! db (map-indexed (partial photo->local db-id) (:photo set)))
  (db/transact! db [(set->local db-id set)]))

(defn store-comments! [db photo comments]
  (db/transact! db [[:db/add (:db/id photo) :photo/comment-state :fetched]])
  (when comments
    (let [comments (map-indexed (partial comment->local (:db/id photo)) comments)]
      (db/transact! db comments))))

(defn store-user! [db db-id user]
  (db/transact! db
    [(assoc user
       :db/id db-id
       :username (-> user :username :_content)
       :showkr/state :fetched
       :showkr/type :user)]))

(defn store-user-sets! [db user sets]
  (db/transact! db
    (map-indexed #(assoc %2
                    :description (-> %2 :description :_content)
                    :title (-> %2 :title :_content)
                    :db/id (- -1 %1)
                    :set/user (:db/id user)
                    :showkr/state :fetched
                    :showkr/type :user-set)
      sets)))

;;; A-la flux or something, call it and data will appear

(defn fetch-set [id]
  (flickr-fetch db {:id id :showkr/type :set}
    {:method "flickr.photosets.getPhotos"
     :photoset_id id
     :extras "original_format,description,path_alias"}
    (fn [db-id data]
      (store-set! db db-id (:photoset data)))))

(defn fetch-comments [photo]
  (when-not (:photo/comment-state photo)
    (db/transact! db [[:db/add (:db/id photo) :photo/comment-state :waiting]])
    (-flickr-fetch db nil
      {:method "flickr.photos.comments.getList"
       :photo_id (:photo/id photo)}
      (fn [_ data]
        (store-comments! db photo (-> data :comments :comment))))))

(defn fetch-user-sets [login]
  (let [user (by-attr @db {:login login})]
    (when (= :fetched (:showkr/state user))
      (-flickr-fetch db nil
        {:method "flickr.photosets.getList"
         :user_id (:id user)}
        (fn [db-id data]
          (store-user-sets! db user (-> data :photosets :photoset)))))))

(defn fetch-user [login]
  (flickr-fetch db {:login login}
    {:method "flickr.urls.lookupUser"
     :url (str "https://flickr.com/photos/" login)}
    (fn [db-id data]
      (store-user! db db-id (:user data))
      (fetch-user-sets login))))

;;; not sure this is the right place

(def photo-order-q
  '[:find ?order
    :in $ ?set-id ?photo-id
    :where
    [?set :id ?set-id]
    [?e :photo/id ?photo-id]
    [?e :photo/set ?set]
    [?e :photo/order ?order]])

(def photo-by-order-q
  '[:find ?e
    :in $ ?set-id ?order
    :where
    [?set :id ?set-id]
    [?e :photo/set ?set]
    [?e :photo/order ?order]])

(defn subseq-photo [dir-fn set-id photo-id]
  (let [order (dir-fn (ffirst (db/q photo-order-q @db set-id photo-id)))
        photo (qe photo-by-order-q @db set-id order)]
    (when photo
      (set! js/location.hash (str "#" set-id "/" (:photo/id photo))))))

(def watch-next (partial subseq-photo (fnil inc -1)))
(def watch-prev (partial subseq-photo dec))
