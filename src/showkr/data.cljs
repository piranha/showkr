(ns showkr.data
  (:import [goog.net Jsonp]))

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

(defn- flickr-error [payload]
  (js/console.error "Error fetching data with parameters:" payload))

(defn flickr-call [payload callback]
  (let [q (Jsonp. URL "jsoncallback")]
    (.send q (clj->js (merge OPTS payload)) callback flickr-error)))

(defn old? [data]
  (> (- (.getTime (js/Date.))
       (or (-> data meta :date) 0))
    *old-threshold*))

(defn flickr-fetch [path attr payload & [cb]]
  (let [data (get-in @world path)]
    (when (empty? data)
      (swap! world assoc-in path
        ^{:state :waiting :date (.getDate (js/Date.))} {}))
    (when (old? data)
      (flickr-call payload
        (fn [data]
          (let [data (js->clj data :keywordize-keys true)]
            (swap! world assoc-in path
              (condp = (:stat data)
                "ok"
                (with-meta (attr data)
                  {:state :fetched
                   :date (.getTime (js/Date.))})

                "fail"
                (if (>= (:code data) 100) ;; not user input fault
                  nil
                  (with-meta data
                    {:state :failed}))))
            (when cb (cb))))))))

(defn fetched? [data]
  (= :fetched (:state (meta data))))

;;; A-la flux or something, call it and data will appear

(defn fetch-set [id]
  (flickr-fetch [:data :sets id] :photoset
    {:method "flickr.photosets.getPhotos"
     :photoset_id id
     :extras "original_format,description,path_alias"}))

(defn fetch-comments [set-id idx]
  (when (fetched? (get-in @world [:data :sets set-id]))
    (flickr-fetch [:data :sets set-id :photo idx :comments] :comments
      {:method "flickr.photos.comments.getList"
       :photo_id (get-in @world [:data :sets set-id :photo idx :id])})))

(defn fetch-user-sets [username]
  (let [user (get-in @world [:data :users username])]
    (when (fetched? user)
      (flickr-fetch [:data :users username :sets] :photosets
        {:method "flickr.photosets.getList"
         :user_id (:id user)}))))

(defn fetch-user [username]
  (flickr-fetch [:data :users username] :user
    {:method "flickr.urls.lookupUser"
     :url (str "https://flickr.com/photos/" username)}
    #(fetch-user-sets username)))

;;; not sure this is the right place

(defn watch-next [set photo-id]
  (let [photos (:photo set)
        next-photo (if-not photo-id
                     (first photos)
                     (second (drop-while #(not= (:id %) photo-id) photos)))]
    (when next-photo
      (set! js/location.hash (str "#" (:id set) "/" (:id next-photo))))))

(defn watch-prev [set photo-id]
  (when photo-id
    (let [photos (:photo set)
          prev-photo (-> (take-while #(not= (:id %) photo-id) photos)
                       last)]
      (when prev-photo
        (set! js/location.hash (str "#" (:id set) "/" (:id prev-photo)))))))
