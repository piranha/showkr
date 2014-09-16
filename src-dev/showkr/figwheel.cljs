(ns showkr.figwheel
  (:require [figwheel.client :as fw :include-macros true]
            [medley.core :refer [update]]
            [showkr.data :as data]
            [showkr :refer [render]]))

(fw/watch-and-reload :jsload-callback
  (fn [] (swap! data/opts update :dev-reload not)))
