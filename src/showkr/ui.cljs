(ns showkr.ui
  (:require [quiescent :as q :include-macros true]
            [quiescent.dom :as d]))

(def MONTHS ["Jan" "Feb" "Mar" "Apr" "May" "Jun"
              "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"])

(defn icon [name]
  (d/i {:className (str "icon-" name)}))

(defn date [date]
  (let [d (js/Date. (* (js/parseInt date 10) 1000))]
    (d/time {:dateTime (.toISOString d)}
      (str (.getDate d) " " (get MONTHS (.getMonth d)) " " (.getFullYear d)))))

(defn spinner []
  (d/img {:src "spinner.gif"}))
