(ns showkr.ui
  (:require [quiescent :as q :include-macros true]
            [quiescent.dom :as d]))

(def MONTHS ["Jan" "Feb" "Mar" "Apr" "May" "Jun"
              "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"])

(defn icon [name]
  (d/i {:className (str "icon-" name)}))

(defn date [date]
  (d/time {:dateTime (.toISOString date)}
    (str (.getDate date) " " (get MONTHS (.getMonth date)) " " (.getFullYear date))))

(defn spinner []
  (d/img {:src "spinner.gif"}))
