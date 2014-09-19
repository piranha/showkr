(ns showkr.data
  (:import [goog.net Jsonp])

  (:require [datascript :as db]))

(def URL "https://api.flickr.com/services/rest/")
(def OPTS {:api_key "1606ff0ad63a3b5efeaa89443fe80704"
           :format "json"})

(def ^:dynamic *old-threshold* (* 1000 60 60 2)) ;; 2 hours

(defonce opts
  (atom {:target nil
         :path nil}))

(let [count (atom 0)]
  (defn temp-id []
    (swap! count dec)))

;; datascript stuff

(defonce db (db/create-conn
  {:photo/set     {:db/cardinality :db.cardinality/many
                   :db/valueType :db.type/ref}
   :comment/photo {:db/valueType :db.type/ref}
   :set/user      {:db/valueType :db.type/ref}
   :userset/user  {:db/valueType :db.type/ref}}))

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

(defn eid-or-temp [db attrmap]
  (or (:db/id (by-attr db attrmap)) (temp-id)))

(defn transact->eid! [db entity]
  (let [eid (:db/id entity)
        tx (db/transact! db [entity])]
    (if (pos? eid)
      eid
      (get (:tempids tx) eid))))

;;; helpers

(defn old? [entity]
  (> (- (.getTime (js/Date.))
       (or (:showkr/date entity) 0))
    *old-threshold*))

(defn parse-date [d]
  (-> d (js/parseInt 10) (* 1000) (js/Date.)))

(defn now []
  (.getTime (js/Date.)))

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
  (let [entity (by-attr @db attrmap)]
    (when (or (nil? entity) (old? entity))
      (let [db-id (transact->eid! db (assoc attrmap
                                      :showkr/state :waiting
                                      :db/id (or (:db/id entity) -1)))]
        (-flickr-fetch db db-id payload cb)))))

;;; converters

(defn set->local [set]
  {:showkr/state :fetched
   :showkr/date (now)
   :set/id (set :id)

   :set/pages (set :pages)
   :set/page (set :page)
   :set/per-page (set :per_page)
   :set/total (set :total)

   :set/primary (set :primary)
   :set/owner (set :owner)

   :title (set :title)
   :description (set :description)})

(defn photo->local [photo idx]
  {:showkr/state :fetched
   :showkr/date (now)
   :photo/order idx
   :photo/id (photo :id)

   :flickr/farm (photo :farm)
   :flickr/server (photo :server)
   :photo/secret (photo :secret)
   :photo/original-secret (photo :originalsecret)
   :photo/original-format (photo :originalformat)
   :photo/path-alias (photo :pathalias)

   :title (photo :title)
   :description (-> photo :description :_content)})

(defn comment->local [comment idx]
  {:showkr/state :fetched
   :showkr/date (now)
   :comment/order idx
   :comment/id (comment :id)

   :flickr/farm (comment :iconfarm)
   :flickr/server (comment :iconserver)
   :date/create (-> comment :datecreate parse-date)
   :comment/author (comment :author)
   :comment/author-name (comment :authorname)
   :comment/real-name (comment :realname)
   :comment/path-alias (comment :path_alias)
   :comment/link (comment :permalink)

   :content (:_content comment)})

(defn user->local [user]
  {:showkr/state :fetched
   :showkr/date (now)
   :user/id (user :id)

   :user/name (-> user :username :_content)})

(defn user-set->local [set]
  {:showkr/state :fetched
   :showkr/date (now)
   :userset/id (set :id)

   :flickr/farm (set :farm)
   :flickr/server (set :server)
   :photo/secret (set :secret)
   :date/update (-> set :date_update parse-date)
   :date/create (-> set :date_create parse-date)

   :set/total (-> set :photos (js/parseInt 10)) ; + (set :videos)?
   :set/primary (set :primary)

   :title (-> set :title :_content)
   :description (-> set :description :_content)})

;;; data->db

(defn store-set! [db db-id set]
  (db/transact! db
    (for [[photo idx] (map vector (:photo set) (range))]
      (-> photo
        (photo->local idx)
        (assoc :photo/set [db-id]
               :db/id     (eid-or-temp @db {:photo/id (:id photo)})))))
  (db/transact! db [(assoc (set->local set) :db/id db-id)]))

(defn store-comments! [db photo comments]
  (db/transact! db [[:db/add (:db/id photo) :photo/comment-state :fetched]])
  (when comments
    (db/transact! db
      (for [[comment idx] (map vector comments (range))]
        (-> comment
          (comment->local idx)
          (assoc :comment/photo (:db/id photo)
                 :db/id         (eid-or-temp @db {:comment/id (:id comment)})))))))

(defn store-user! [db db-id user]
  (db/transact! db [(assoc (user->local user) :db/id db-id)]))

(defn store-user-sets! [db user sets]
  (db/transact! db
    (for [set sets]
      (-> set
        user-set->local
        (assoc :userset/user (:db/id user)
               :db/id        (eid-or-temp @db {:userset/id (:id set)}))))))

;;; A-la flux or something, call it and data will appear

(defn fetch-set [id]
  (flickr-fetch db {:set/id id}
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
  (let [user (by-attr @db {:user/login login})]
    (when (= :fetched (:showkr/state user))
      (-flickr-fetch db nil
        {:method "flickr.photosets.getList"
         :user_id (:user/id user)}
        (fn [db-id data]
          (store-user-sets! db user (-> data :photosets :photoset)))))))

(defn fetch-user [login]
  (flickr-fetch db {:user/login login}
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
    [?set :set/id ?set-id]
    [?e :photo/id ?photo-id]
    [?e :photo/set ?set]
    [?e :photo/order ?order]])

(def photo-by-order-q
  '[:find ?e
    :in $ ?set-id ?order
    :where
    [?set :set/id ?set-id]
    [?e :photo/set ?set]
    [?e :photo/order ?order]])

(defn subseq-photo [dir-fn set-id photo-id]
  (let [order (dir-fn (ffirst (db/q photo-order-q @db set-id photo-id)))
        photo (qe photo-by-order-q @db set-id order)]
    (when photo
      (set! js/location.hash (str "#" set-id "/" (:photo/id photo))))))

(def watch-next (partial subseq-photo (fnil inc -1)))
(def watch-prev (partial subseq-photo dec))
