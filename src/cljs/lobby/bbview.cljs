(ns lobby.bbview
  (:require
    [reagent.core :as r]
    [lobby.model :as model :refer [ uname gid gm ]]
    [lobby.comms :as comms]))
    
; Atom and Atom Functions

(def bb-app (r/atom {}))

(defn- chat [ ]
  [:div.chat
    [:div.border.rounded.p-1.chatbox   ;{:style {:min-height (* 45 (-> @ra-app :settings :cardsize :scale))}}
      (for [msg (-> @gm :state :chat) :let [{:keys [msg uname timestamp]} msg]]
        [:div {:key (gensym) :style {:word-wrap "break-word"}}
          ;[:span.mr-1 (model/timeformat timestamp)]
          [:b.text-primary.mr-1 uname]
          [:span msg]])
          ]
    [:form {:on-submit (fn [e] (.preventDefault e) (comms/sendmsg! (:msg @bb-app) @gid ) (swap! bb-app assoc :msg ""))}
      [:div.input-group
        [:input.form-control.form-control-sm.bg-light {
          :type "text" :placeholder "Type to chat"
          :value (:msg @bb-app)
          :on-change #(swap! bb-app assoc :msg (-> % .-target .-value))}]
        [:span.input-group-append [:button.btn.btn-sm.btn-outline-secondary {:type "btn"} [:i.fas.fa-arrow-right]]]]]])

(defn- teamplyrcard [ plyr ]
  (let [ src (str "/img/bb/images/" (:team plyr) " - " (:position plyr) ".png" )]
    [:div {:key (gensym) :on-mouse-move #(swap! bb-app assoc :bigview src) :on-mouse-out #(swap! bb-app dissoc :bigview) :on-click #(swap! bb-app assoc :showbigview? true)}
      [:img.bbcard.mx-1 {:src src}]]
    ))

(defn- highlights [ state ]
  [:div.d-flex.justify-content-around.mb-2
    (for [ hl (-> state :highlights :public) :let [src (str "img/bb/images/Highlight-" (:id hl) ".png")]]
      [:div.highlight {:key (gensym)}
        [:img.hlcard {:on-mouse-over #(swap! bb-app assoc :bigview src) :on-click #(swap! bb-app assoc :showbigview? true) :src src } ]
        [:img.football {:src "img/bb/images/Football.png"}]
        ])])

(defn- hand [ state uname ]
  (let [team (-> state :players (get uname) :team)]
    [:div.row
      [:div.col-2
        [:div.d-flex 
          [:div 
            [:img.bbcard.draw {:src (str "img/bb/images/" (:alliance team) " Back.png")}]
            [:div.imgnumber (-> team :secret count)]]
          [:div   
            [:img.bbcard.discard {:src (str "img/bb/images/" (:alliance team) " Back.png")}]
            [:div.imgnumber (-> team :discard count)]]]]
      [:div.col-8
        [:div.d-flex
          (for [p (->> team :private (sort-by :id))]
            (teamplyrcard p))]]
      [:div.col-2 (chat)]]))

(defn gameview [ state ]
  [:div.col
    (highlights state)
    (hand state @uname)]
  )

(defn plyrname [ plyr ]
  (clojure.string/join " " (-> plyr (select-keys [:race :position]) vals)))
(defn- teamcard [ team ]
  )


(defn setupview [ state ]
  [:div "Setup View"]
  (let [ choose?  (= uname (get (-> state :turnorder repeat) (:turn state)))
         chosenteams (->> @gm :state :players vals (map :team) set) ]
    [:div.col
      [:div.row
        (for [ p (:turnorder state) ]
          [:div.col-3 
            {:key (gensym)}
            [:div.p-3.border.round
              [:h5.text-center p]
              [:div.text-center (str "Team: " (-> state :players (get p) :team))]
          ]])]
      [:div.d-flex.justify-content-around
        (for [ t (->> state :teams (map :team) distinct) :let [chosen? (contains? chosenteams t)]]
          [:img.choicebox {
              :key (gensym)
              :class (if chosen? "disabled" "enabled")
              :on-mouse-move (fn [e] (.stopPropagation e) (swap! bb-app assoc :team t))
              :on-click #(if chosen? nil (comms/ra-send! {:action :chooseteam :team t}))
              :src (str "img/bb/images/Marker " t ".png")}])]
      [:div.d-flex.justify-content-center
        (for [ t (->> state :teams (filter #(= (:team %) (:team @bb-app))) (sort-by :id))]
          [:div.p-2 
            {:key (gensym)}
            (teamplyrcard t)])
        ]]
  ))



(defn bbmain [ ]
  (-> ((js* "$") "body") 
      (.css "background-image" "url(/img/bb/images/bbpitch.png")
      (.css "background-size" "100%")
      
      )
  (let [state (:state @gm)]
    [:div.container-fluid.my-2
      
      (if (:showbigview? @bb-app)
        [:div.bigview {:on-click #(swap! bb-app dissoc :showbigview?)}
          [:div.bigviewcontent
            [:img.bigviewimg {:src (:bigview @bb-app)}]]])
      [:h4 "BBTM " (:status state)]
      (case (:status state)
        :setup (setupview state)
        (gameview state))

      [:div (str @bb-app)]
      [:div (str state)]]))