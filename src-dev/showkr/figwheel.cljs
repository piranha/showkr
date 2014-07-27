(ns showkr.figwheel
  (:require [figwheel.client :as fw :include-macros true]
            [showkr.data :as data]
            [showkr.main :refer [render]]))

(fw/watch-and-reload :jsload-callback #(swap! data/world identity))


