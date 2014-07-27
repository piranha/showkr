(ns showkr.form
  (:require [quiescent :as q :include-macros true]
            [quiescent.dom :as d]

            [showkr.ui :as ui]))

(let [count (atom 0)]
  (defn unique-id []
    (str "c" (swap! count inc))))

(q/defcomponent FormGroup
  [{:keys [label icon input]}]
  (let [id (unique-id)]
    (d/div {:className "control-group"}
      (d/label {:htmlFor id} label)
      (d/div {:className "controls"}
        (d/div {:className "input-prepend"}
          (if icon
            (d/span {:className "add-on"}
              (ui/icon icon)))
          (d/input (assoc input :id id)))))))

(q/defcomponent Form
  []
  (d/div {:className "row"}
    (d/fieldset {:className "span6"}
      (d/legend nil "Go directly to photoset")

      (FormGroup {:label "Photoset URL or id"
                  :icon "edit"
                  :input {:type "text"
                          :placeholder "Photoset URL or id"}})
      
      (d/div {:className "control-group"}
        (if false 

          (d/div {:className "controls"}
            "Or select something from your history: "
            (apply d/ul nil
              (for [[key name] []]
                (d/li nil
                  (d/a {:href (str "#" key)} name)))))

          (d/div {:className "controls"}
            "Or watch an "
            (d/a {:href "#72157627590185596"} "example")))))

    (d/fieldset {:className "span6"}
      (d/legend nil "Browse user photos")

      (FormGroup {:label "User id or username"
                  :icon "user"
                  :input {:type "text"
                          :placeholder "User id or username"}}))

    (d/div {:className "form-actions"}
      (d/input {:type "submit" :className "btn btn-primary"}))))
