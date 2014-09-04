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

(defonce db (db/create-conn {:photo {:db/cardinality :db.cardinality/many}
                             :set {:db/cardinality :db.cardinality/many}
                             :comment/photo {:db/valueType :db.type/ref}
                             :set/user {:db/valueType :db.type/ref}}))

(defn- flickr-error [payload]
  (js/console.error "Error fetching data with parameters:" payload))

(defn flickr-call [payload callback]
  (let [q (Jsonp. URL "jsoncallback")]
    (.send q (clj->js (merge OPTS payload)) callback flickr-error)))

(defn old? [data]
  (> (- (.getTime (js/Date.))
       (or (-> data meta :date) 0))
    *old-threshold*))

(defn fetched? [data]
  (= :fetched (:state (meta data))))

(defn flickr-fetch [path attr payload & [cb]]
  (let [data (get-in @world path)]
    (when (empty? data)
      ;(js/console.log (str (pr-str path) " is empty"))
      (swap! world assoc-in path
        ^{:state :waiting :date (.getTime (js/Date.))} {}))
    (when (old? data)
      ;(js/console.log (str (pr-str path) " is too old"))
      (flickr-call payload
        (fn [data]
          (let [data (js->clj data :keywordize-keys true)]
            (case (:stat data)
              "ok"
              ;; NOTE: I'm not exactly sure this is the best behavior, but for
              ;; now it worked for me. I guess I need to think of better general
              ;; layout. Probably I should put fetched data in another fetched
              ;; data, but it's too convenient to render.
              ;;
              ;; FIXME: Maybe switch to DataScript and make joins and all the
              ;; fun stuff?
              (swap! world update-in path
                #(with-meta (merge % (attr data)) {:state :fetched
                                                   :date (.getTime (js/Date.))}))

              "fail"
              (swap! world assoc-in path
                (if (>= (:code data) 100) ;; not user input fault
                  nil
                  (with-meta data
                    {:state :failed}))))
            (when cb (cb))))))))

(defn by-attr [db attrmap]
  (let [q {:find '[?e] :where (mapv #(apply vector '?e %) attrmap)}
        e (ffirst (db/q q db))]
    (when e
      (db/entity db e))))

(defn transact->id! [db entity]
  (let [tempid (:db/id entity)
        tx (db/transact! db [entity])]
    (get (:tempids tx) tempid)))

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

(defn fetch-set-db [id]
  (when-not (by-attr @db {:id id :showkr/type :set})
    (let [db-id (transact->id! db {:db/id -1 :id id
                                   :showkr/state :waiting :showkr/type :set})]
      (flickr-call {:method "flickr.photosets.getPhotos"
                    :photoset_id id
                    :extras "original_format,description,path_alias"}
        (fn [data]
          (let [data (js->clj data :keywordize-keys true)]
            (case (:stat data)
              "ok"
              (store-set! db db-id (:photoset data))

              "fail"
              (js/console.log "query failed" "fetch-set" id))))))))

(defn fetch-comments-db [photo]
  (when-not (:photo/comment-state photo)
    (db/transact! db [[:db/add (:db/id photo) :photo/comment-state :waiting]])
    (flickr-call {:method "flickr.photos.comments.getList"
                  :photo_id (:id photo)}
      (fn [data]
        (let [data (js->clj data :keywordize-keys true)]
          (case (:stat data)
            "ok"
            (store-comments! db photo (-> data :comments :comment))

            "fail"
            (js/console.log "query failed" "fetch-comments" (:id photo))))))))

(defn fetch-user-sets-db [login]
  (let [user (by-attr @db {:login login})]
    (when (= :fetched (:showkr/state user))
      (flickr-call {:method "flickr.photosets.getList"
                    :user_id (:id user)}
        (fn [data]
          (let [data (js->clj data :keywordize-keys true)]
            (case (:stat data)
              "ok"
              (store-user-sets! db user (-> data :photosets :photoset))

              "fail"
              (js/console.log "query failed" "fetch-user-sets" login))))))))

(defn fetch-user-db [login]
  (when-not (by-attr @db {:login login})
    (let [db-id (transact->id! db {:db/id -1 :login login
                                   :showkr/state :waiting :showkr/type :user})]
      (flickr-call {:method "flickr.urls.lookupUser"
                    :url (str "https://flickr.com/photos/" login)}
        (fn [data]
          (let [data (js->clj data :keywordize-keys true)]
            (case (:stat data)
              "ok"
              (do
                (store-user! db db-id (:user data))
                (fetch-user-sets-db login))

              "fail"
              (js/console.log "query failed" "fetch-user" login))))))))

;; (defn fetch-set [id]
;;   (flickr-fetch [:data :sets id] :photoset
;;     {:method "flickr.photosets.getPhotos"
;;      :photoset_id id
;;      :extras "original_format,description,path_alias"}))

;; (defn fetch-comments [set-id idx]
;;   (when (fetched? (get-in @world [:data :sets set-id]))
;;     (flickr-fetch [:data :sets set-id :photo idx :comments] :comments
;;       {:method "flickr.photos.comments.getList"
;;        :photo_id (get-in @world [:data :sets set-id :photo idx :id])})))

;; (defn fetch-user-sets [username]
;;   (let [user (get-in @world [:data :users username])]
;;     (when (fetched? user)
;;       (flickr-fetch [:data :users username :sets] :photosets
;;         {:method "flickr.photosets.getList"
;;          :user_id (:id user)}))))

;; (defn fetch-user [username]
;;   (flickr-fetch [:data :users username] :user
;;     {:method "flickr.urls.lookupUser"
;;      :url (str "https://flickr.com/photos/" username)}
;;     #(fetch-user-sets username)))

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
