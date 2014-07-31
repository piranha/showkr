(ns showkr.figwheel
  (:require [figwheel.client :as fw :include-macros true]
            [showkr.data :as data]
            [showkr :refer [render]]))

(fw/watch-and-reload :jsload-callback (fn [] (swap! data/world
                                               #(assoc % :fake-val (not (:fake-val %))))))


