(ns showkr.utils)

(defmacro p
  [body]
  `(let [x# ~body]
     (js/console.log (cljs.core/pr-str '~body) "=" (cljs.core/js->clj x#))
     x#))
