(ns showkr.form
  (:require [quiescent :as q :include-macros true]
            [quiescent.dom :as d]
            [datascript :as db]

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

(defn handle-submit [form setter]
  (when (or (:set form) (:user form))
    (set! js/window.location
      (cond
        (re-matches #"^\d+$" (:set form ""))
        (str "#" (:set form))

        (re-matches #"/sets/(\d+)/" (:set form ""))
        (str "#"
          (first (re-matches #"/sets/(\d+)/" (:set form))))

        (:user form)
        (str "#user/" (:user form))

        (:set form)
        (do
          (js/alert "Something wrong with your input")
          "#")

        :else
        "#"))
    (setter {})))

(q/defcomponent Form
  [{:keys [form db]} setter]
  (d/form {:className "form-horizontal"
           :onSubmit (fn [e]
                       (.preventDefault e)
                       (handle-submit form setter))}
    (d/div {:className "row"}
      (d/fieldset {:className "span6"}
        (d/legend nil "Go directly to album")

        (FormGroup {:label "Album URL or id"
                    :icon "edit"
                    :input {:type "text"
                            :placeholder "Album (set) URL or id"
                            :tabIndex 1
                            :value (:set form "")
                            :onChange #(setter :set (.. % -target -value))}})

        (d/div {:className "control-group"}
          (let [sets (db/q '[:find ?id ?title
                             :where [?e :set/id ?id]
                                    [?e :title ?title]] db)]
            (if-not (empty? sets)

              (d/div {:className "controls"}
                "Or select something from your history: "
                (apply d/ul nil
                  (for [[id title] sets]
                    (d/li nil
                      (d/a {:href (str "#" id)} title)))))

              (d/div {:className "controls"}
                "Or watch an "
                (d/a {:href "#72157629517765623"} "example"))))))

      (d/fieldset {:className "span6"}
        (d/legend nil "Browse user albums")

        (FormGroup {:label "User id or username"
                    :icon "user"
                    :input {:type "text"
                            :placeholder "User id or username"
                            :tabIndex 2
                            :value (:user form "")
                            :onChange #(setter :user (.. % -target -value))}})

        (d/div {:className "control-group"}
          (let [users (db/q '[:find ?id ?name (count ?set)
                              :where [?e :user/id ?id]
                                     [?e :user/name ?name]
                                     [?set :userset/user ?e]] db)]
            (if-not (empty? users)
              (d/div {:className "controls"}
                "Or browse someone from your history: "
                (apply d/ul nil
                  (for [[id name set-count] users]
                    (when (pos? set-count)
                      (d/li nil
                        (d/a {:href (str "#user/" id)} name)))))))))))

    (d/div {:className "form-actions"}
      (d/input {:type "submit"
                :className "btn btn-primary"
                :tabIndex 3}))))
