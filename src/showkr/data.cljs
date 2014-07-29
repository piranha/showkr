(ns showkr.data
  (:import [goog.net Jsonp]))

(def URL "https://api.flickr.com/services/rest/")
(def OPTS {:api_key "1606ff0ad63a3b5efeaa89443fe80704"
           :format "json"})

(defonce world
  (atom {:target nil
         :path nil
         :form {}
         :sets {}
         :users {}}))

(defn- flickr-error [payload]
  (js/console.error "Error fetching data with parameters:" payload))

(defn call-flickr [payload callback]
  (let [q (Jsonp. URL "jsoncallback")]
    (.send q (clj->js (merge OPTS payload)) callback flickr-error)))

(defn fetched? [data]
  (= :fetched (:state (meta data))))

;;; A-la flux or something, call it and data will appear

(defn fetch-set [id]
  (when-not (get-in @world [:sets id])
    (swap! world assoc-in [:sets id]
      ^{:state :waiting} {})
    (call-flickr {:method "flickr.photosets.getPhotos"
                  :photoset_id id
                  :extras "original_format,description,path_alias"}
      (fn [data]
        (swap! world assoc-in [:sets id]
          (with-meta (js->clj (.-photoset data) :keywordize-keys true)
            {:state :fetched}))))))

(defn fetch-comments [set-id idx]
  (let [photo (get-in @world [:sets set-id :photo idx])]
    (when (and photo (not (:comments photo)))
      (swap! world assoc-in [:sets set-id :photo idx :comments]
        ^{:state :waiting} {})
      (call-flickr {:method "flickr.photos.comments.getList"
                    :photo_id (:id photo)}
        (fn [data]
          (swap! world assoc-in [:sets set-id :photo idx :comments]
            (with-meta (js->clj (.-comments data) :keywordize-keys true)
              {:state :fetched})))))))

(defn fetch-user-sets [username]
  (let [user (get-in @world [:users username])]
    (when (and (fetched? user) (not (:sets user)))
      (swap! world assoc-in [:users username :sets]
        ^{:state :waiting} {})
      (call-flickr {:method "flickr.photosets.getList"
                    :user_id (:id user)}
        (fn [data]
          (swap! world assoc-in [:users username :sets]
            (with-meta (js->clj (.-photosets data) :keywordize-keys true)
              {:state :fetched})))))))

(defn fetch-user [username]
  (when-not (get-in @world [:users username])
    (swap! world assoc-in [:users username]
      ^{:state :waiting} {})
    (call-flickr {:method "flickr.urls.lookupUser"
                  :url (str "https://flickr.com/photos/" username)}
      (fn [data]
        (swap! world assoc-in [:users username]
          (with-meta (js->clj (.-user data) :keywordize-keys true)
            {:state :fetched}))
        (fetch-user-sets username)
        #_ (fetch-user-info username)))))

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
