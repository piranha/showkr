(ns showkr.utils
  (:require [goog.string :as gfmt]
            [goog.string.format]))

(defn get-route []
  (let [path (.. js/window -location -hash)]
    (.slice path 1)))

(def fmt gfmt/format)
