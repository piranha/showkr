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

(defonce db (db/create-conn {:photo {:db/cardinality :db.cardinality/many}
                             :set {:db/cardinality :db.cardinality/many}
                             :comment/photo {:db/valueType :db.type/ref}
                             :set/user {:db/valueType :db.type/ref}}))

(defn by-attr [db attrmap]
  (let [q {:find '[?e] :where (mapv #(apply vector '?e %) attrmap)}
        e (ffirst (db/q q db))]
    (when e
      (db/entity db e))))

(defn transact->id! [db entity]
  (let [tempid (:db/id entity)
        tx (db/transact! db [entity])]
    (get (:tempids tx) tempid)))

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
          (db/transact! db [[:db/add db-id :showkr/state :failed]]))))))

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

(defn photo->local [set-id idx photo]
  (assoc photo
    :db/id (- -1 idx)
    :photo/order idx
    :showkr/state :fetched
    :showkr/type :photo
    :set [set-id]
    :description (-> photo :description :_content)))

(defn comment->local [photo-id idx comment]
  (assoc comment
    :db/id (- -1 idx)
    :content (:_content comment)
    :comment/order idx
    :showkr/state :fetched
    :showkr/type :comment
    :comment/photo photo-id))

;;; data->db

(defn store-set! [db db-id set]
  (let [photos (map-indexed (partial photo->local db-id) (:photo set))
        photo-tx (db/transact! db photos)]
    (db/transact! db
      [(assoc set
         :db/id db-id
         :showkr/state :fetched
         :showkr/type :set
         :photo (vals (:tempids photo-tx)))])))

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
       :photo_id (:id photo)}
      (fn [db-id data]
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

(defn watch-next [set-id photo-id]
  #_ (datascript/q
    '[:find ?id
      :in $ ?set-id ?photo-id
      :where
      [?prev :id ?photo-id]
      [?prev :photo/order ?prev-order]
      [?e :set ?set-id]
      [?e :photo/order (inc ?prev-order)]
      [?e :id ?id]]
    @showkr.data/db set-id photo-id)

  (let [photos (:photo set-id)
        next-photo (if-not photo-id
                     (first photos)
                     (second (drop-while #(not= (:id %) photo-id) photos)))]
    (when next-photo
      (set! js/location.hash (str "#" (:id set-id) "/" (:id next-photo))))))

(defn watch-prev [set-id photo-id]
  (when photo-id
    (let [photos (:photo set-id)
          prev-photo (-> (take-while #(not= (:id %) photo-id) photos)
                       last)]
      (when prev-photo
        (set! js/location.hash (str "#" (:id set-id) "/" (:id prev-photo)))))))
