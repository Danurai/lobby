(ns lobby.bbview
  (:require
    [reagent.core :as r]
    [lobby.model :as model :refer [ uname gid gm ]]
    [lobby.comms :as comms]))
    
; Atom and Atom Functions

(def bb-app (r/atom {}))

(defn- setbigview [ url & args ] (if (nil? url) (swap! bb-app dissoc :bigview) (swap! bb-app assoc :bigview url)))
(defn- showbigview [] (swap! bb-app assoc :showbigview? true))

(defn- chat [ ]
  [:div.chat
    [:div.border.rounded.p-1.chatbox   ;{:style {:min-height (* 45 (-> @ra-app :settings :cardsize :scale))}}
      (for [msg (-> @gm :state :chat) :let [{:keys [msg uname timestamp]} msg]]
        [:div {:key (gensym) :style {:word-wrap "break-word"}}
          ;[:span.mr-1 (model/timeformat timestamp)]
          [:b.text-primary.mr-1 uname]
          [:span msg]])]
    [:form {:on-submit (fn [e] (.preventDefault e) (comms/sendmsg! (:msg @bb-app) @gid ) (swap! bb-app assoc :msg ""))}
      [:div.input-group
        [:input.form-control.form-control-sm.bg-light {
          :type "text" :placeholder "Type to chat"
          :value (:msg @bb-app)
          :on-change #(swap! bb-app assoc :msg (-> % .-target .-value))}]
        [:span.input-group-append [:button.btn.btn-sm.btn-outline-secondary {:type "btn"} [:i.fas.fa-arrow-right]]]]]])


(defn plyrname [ plyr ] (clojure.string/join " " (-> plyr (select-keys [:race :position]) vals)))

; HIGHLIGHTS

(defn get-team-zone-map [ hl ]
  (reduce-kv
    (fn [m k v]
      (let [tm (-> v first :team)]
        (if tm (assoc m tm k) m)))
    {} (:zone hl)))

(defn- player-bar [ hl zone ]
  (let [tzones (get-team-zone-map  hl) pteam (-> @gm :state :players (get @uname) :team :team)]
    [:div.col-4 
      (if (or (empty? tzones)
              (-> tzones (get pteam) (= zone)) 
              (and (-> tzones (get pteam) nil?) (-> hl :zone zone empty?) )) 
        (if (-> @bb-app :matchupcommit? some?)
            (if (and (-> @bb-app :matchupcommit? :id (= (:id hl))) (-> @bb-app :matchupcommit? :zone (= zone))) [:button.btn.btn.sm.w-100.btn-warning "Select a player to commit (Cancel)"])
            [:button.btn.btn-sm.w-100.btn-light {:on-click (fn [e] (.stopPropagation e) (swap! bb-app assoc :matchupcommit? {:id (:id hl) :zone zone}))} "Commit"]))
      (for [ p (-> hl :zone zone) ]
        (let [url (str "/img/bb/images/" (:team p) " - " (:position p) ".png")]
          [:div.bar.bgimg.d-flex {
              :key (gensym) 
              :class (if (:prone? p) "prone") 
              :style {:background-image (str "url('" url "')" ) }
              :on-mouse-move #(setbigview url) :on-mouse-out #(setbigview nil) :on-click #(showbigview)} 
            [:img.team-marker {:src (str "/img/bb/images/Marker " (:team p) "1.png")}]
            [:span.label (:position p)]
            [:span.spp (-> p :spp (get (if (:prone? p) 1 0)))]
            ]))]))

(defn- highlights [ state ]
  [:div.row.mb-2
    (doall (for [ hl (-> state :highlights :public) :let [src (str "img/bb/images/Highlight-" (:id hl) ".png")]]
      [:div.col {:key (gensym)}
        [:div.row 
          (player-bar hl :a)
          [:div.col-4.highlight
            [:div.d-flex.justify-content-around.scoreboard 
              [:div (->> hl :zone :a (map #(+ (if (:hasball? %) 2 0) (if (:prone? %) (-> % :spp last) (-> % :spp first)))) (reduce +) )]
              [:div (->> hl :zone :b (map #(+ (if (:hasball? %) 2 0) (if (:prone? %) (-> % :spp last) (-> % :spp first)))) (reduce +) )]]
            [:img.img-fluid {:on-mouse-over #(swap! bb-app assoc :bigview src) :on-click #(swap! bb-app assoc :showbigview? true) :src src } ]
            [:img.football {:src "img/bb/images/Football.png"}]]
          (player-bar hl :b)]]))])


(defn- teamplyrcard [ plyr ]
  (let [ src (str "/img/bb/images/" (:team plyr) " - " (:position plyr) ".png" ) matchup (-> @bb-app :matchupcommit?)]
    [:div.playercard {
        :key (gensym) 
        :on-mouse-move #(setbigview src) 
        :on-mouse-out #(setbigview nil) 
        :on-click #(if (some? matchup) 
                        (comms/ra-send! {:action :commitplayer :plid (:id plyr) :hlid (:id matchup) :zone (:zone matchup)}) 
                        (showbigview)) 
        :class (if (some? matchup) "selectable")}
      [:img.img-fluid.mx-1 {:src src}]
      (if (-> plyr :qty some?) [:span.playerqty "x" (:qty plyr)])]))

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
          (doall (for [p (->> team :private (sort-by :id)) :let [hlid (-> state :highlights :public first :id)]]
            [:div {:key (gensym)}
              (teamplyrcard p)]))]]
      [:div.col-2 (chat)]]))

(defn gameview [ state ]
  [:div.col
    (highlights state)
    (hand state @uname)]
  )

(defn- teamcard [ team ]
  )


(defn setupview [ state ]
  [:div "Setup View"]
  (let [ choose?  (= uname (get (-> state :turnorder repeat) (:turn state)))
         chosenteams (->> @gm :state :players vals (map :team) set)
         team (:team @bb-app) ]
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
        (for [ tq (->> state :teams (filter #(= (:team %) team)) (sort-by :id) (map :position) frequencies) ]
          [:div.p-2 
            {:key (gensym)}
            (teamplyrcard {:position (first tq) :qty (last tq) :team team })
            ])]]))



(defn bbmain [ ]
  (-> ((js* "$") "body") 
      (.css "background-image" "url(/img/bb/images/bbpitch.png")
      (.css "background-size" "100%")
      
      )
  (let [state (:state @gm)]
    [:div.container-fluid.my-2 {:class (if (-> @bb-app :matchupcommit? some?) "select" ) :on-click #(swap! bb-app dissoc :matchupcommit?)}
      
      (if (:showbigview? @bb-app)
        [:div.bigview {:on-click #(swap! bb-app dissoc :showbigview?)}
          [:div.bigviewcontent
            [:img.bigviewimg {:src (:bigview @bb-app)}]]])
      [:div.d-flex.mb-2
        [:h4.mr-2 "BBTM " (:status state)]
        [:button.btn.btn-sm.btn-danger {:on-click #(if (js/confirm "Are you sure you want to Quit?") (comms/leavegame @gid))} "Quit"]]
      (case (:status state)
        :setup (setupview state)
        (gameview state))

      [:div (str @bb-app)]
      [:div.bg-light (-> state (dissoc :players :chat :highlights) str)]
      [:div.bg-light (-> state :highlights :public first str)]
      [:div.bg-light (-> state :highlights :public second str)]
      [:div.bg-light (-> state :players first first str)]
      [:div.bg-light (-> state :players first second str)]
      [:div.bg-light (-> state :players second first str)]
      [:div.bg-light (-> state :players second second str)]
      
      ]))