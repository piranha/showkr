(ns showkr.figwheel
  (:require [figwheel.client :as fw :include-macros true]
            [showkr.main :refer [render]]))

(fw/watch-and-reload :jsload-callback render)


