(ns showkr.utils)

(defn get-route []
  (let [path (.. js/window -location -hash)]
    (.slice path 1)))
