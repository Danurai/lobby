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
      (for [msg (-> @gm :state :chat reverse) :let [{:keys [msg uname timestamp]} msg]]
        [:div {:key (gensym) :style {:word-wrap "break-word"}}
          ;[:span.me-1 (model/timeformat timestamp)]
          [:b.text-primary.me-1 uname]
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

(defn- highlight-zone [ hl zone ]
  (let [tzones (get-team-zone-map  hl) pteam (-> @gm :state :players (get @uname) :team :team)]
    [:div.col
      (if (and (= :matchup (-> @gm :state :players (get @uname) :state))
               (= (-> @gm :state :activeplyr) @uname)
               (->> @gm :state :players (get @uname) :passed? ((complement true?))))
        (if (or (empty? tzones)
                (-> tzones (get pteam) (= zone)) 
                (and (-> tzones (get pteam) nil?) (-> hl :zone zone empty?) ))
          (if (-> @bb-app :matchupcommit? some?)
              (if (and (-> @bb-app :matchupcommit? :id (= (:id hl))) (-> @bb-app :matchupcommit? :zone (= zone))) [:button.btn.btn.sm.w-100.btn-warning "Select a player to commit (Cancel)"])
              [:button.btn.btn-sm.w-100.btn-light {:on-click (fn [e] (.stopPropagation e) (swap! bb-app assoc :matchupcommit? {:id (:id hl) :zone zone}))} "Commit"])))
      (for [ p (-> hl :zone zone) ]
        (let [url (str "/img/bb/images/" (:team p) " - " (:position p) ".png")]
          [:div.bar.bgimg.d-flex {
              :key (gensym) 
              :class (if (:prone? p) "prone") 
              :style {:background-image (str "url('" url "')" ) }
              :on-mouse-move #(setbigview url) :on-mouse-out #(setbigview nil) :on-click #(showbigview)} 
            [:img.team-marker {:src (str "/img/bb/images/Marker " (:team p) ".png")}]
            [:span.label 
              [:span.me-2 (:position p)]
              (if (-> hl :ballcarrier (= {:zone zone :id (:id p)})) [:b "Ball"])]
            [:span.spp (-> p :spp (get (if (:prone? p) 1 0)))]
            ]))]))

(defn- highlights [ state ]
  [:div.row.mb-2
    (doall (for [ hl (-> state :highlights :public) :let [src (str "img/bb/images/Highlight-" (:id hl) ".png")]]
      [:div.col {:key (gensym)}
        [:div.row 
          (highlight-zone hl :a)
          [:div.col-2
            [:div.row.highlight
              [:div.d-flex.justify-content-around.scoreboard 
                [:div (->> hl :zone :a (map #(+ (if (= {:zone :a :id (:id %)} (:ballcarrier hl)) 2 0) (if (:prone? %) (-> % :spp last) (-> % :spp first)))) (reduce +) )]
                [:div (->> hl :zone :b (map #(+ (if (= {:zone :b :id (:id %)} (:ballcarrier hl)) 2 0) (if (:prone? %) (-> % :spp last) (-> % :spp first)))) (reduce +) )]]
              [:img.img-fluid {:on-mouse-over #(swap! bb-app assoc :bigview src) :on-click #(swap! bb-app assoc :showbigview? true) :src src } ]
              (if (-> hl :ballcarrier nil?) [:img.football {:src "img/bb/images/Football.png"}])]]
          (highlight-zone hl :b)]]))])


(defn- teamplyrcard [ plyr ]
  (let [ src (str "/img/bb/images/" (:team plyr) " - " (:position plyr) ".png" ) matchup (-> @bb-app :matchupcommit?)]
    [:div.playercard {
        :key (gensym) 
        :on-mouse-move #(setbigview src) 
        :on-mouse-out #(setbigview nil) 
        :on-click #(if (some? matchup) 
                        (comms/ra-send! {:action :commitplayer :plid (:id plyr) :hlid (:id matchup) :zone (:zone matchup)}) 
                        (showbigview)) 
        :class (if (some? matchup) "selectable")
        :style {:filter (str "grayscale(" (if (:disabled? plyr) "100%" "0%") ")")}}
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

(defn response [ state resp s ]
  (case (:id resp)
    :discard 
      [:div
        [:h5.text-center "Discard One" ]
        [:div.d-flex.justify-content-center
          (for [p (-> state :players (get @uname) :team :private)]
            [:div.playercard.selectable {:key (gensym)}
              [:img.img-fluid {:src (str "/img/bb/images/" (:team p) " - " (:position p) ".png") :on-click #(comms/ra-send! {:action :response :id (:id p)})}]])]]
    :tackle-target 
      (let [committed (-> state :players (get @uname) :srcplayer) hlid (:hlid state) zone (if (= :a (:srczone state)) :b :a)]
        [:div 
          [:h5.text-center (:msg resp)]
          (for [p (->> state :highlights :public (filter #(= (:id %) hlid)) first :zone zone)]
            [:div.playercard.selectable {:key (gensym)}
            [:img.img-fluid {:src (str "/img/bb/images/" (:team p) " - " (:position p) ".png") :on-click #(comms/ra-send! {:action :response :id (:id p)})}]])
        ])
    [:div [:button.btn.btn-success {:on-click #(comms/ra-send! {:action :response})} (:msg resp)]]))

(defn abilities [ state ]
  (let [plyr   (:srcplayer state)
        src    (str "/img/bb/images/" (:team plyr) " - " (:position plyr) ".png") 
        active (-> plyr :skills (get (:activeskill state)))]
    [:div.abilities.d-flex
      [:div.playercard 
        [:img.img-fluid {
          :src src 
          :on-mouse-move #(setbigview src) 
          :on-mouse-out #(setbigview nil) 
          :on-click #(showbigview) }]]
      [:div.d-flex
        (doall (map-indexed 
          (fn [id s] 
            [:div {:key (gensym)}
              (if (not= id (:activeskill state))
                [:div.h4.mx-2 {:class (if (<= id (:activeskill state)) "text-light") } s]
                (if-let [resp (-> state :players (get @uname) :response)]
                  (response state resp s)
                  [:div
                    [:div.h2.text-center active]
                    [:div 
                      (if (not= :cheat active)
                        [:button.btn.btn-warning.me-2 {:on-click #(if (js/confirm "Are you sure you want to skip using this skill?") (comms/ra-send! {:action :skill-pass}))} "Skip"])
                      [:button.btn.btn-success {:on-click #(comms/ra-send! {:action :skill-use :skill active})} s]]]))])
          (:skills plyr)))]
      [:div.ms-auto (chat)]
    ]))

(defn gameview [ state ]
  [:div.col
    (highlights state)
    (if (-> state :players (get @uname) :state (= :skills))
      (abilities state)
      (hand state @uname))]
  )

(defn- teamcard [ team ]
  )

; SETUP

(defn setupview [ state ]
  (let [ ap           (get (:turnorder state) (:turn state))
         isap?        (= @uname (get (:turnorder state) (:turn state)))
         chosenteams  (->> state :players vals (map :team) set)
         team         (or (:team @bb-app) (->> state :teams (map :team) distinct (remove chosenteams) sort first )) ]
    [:div.col
      [:div.row.mb-2
        (for [ p (:turnorder state) :let [pteam (-> state :players (get p) :team)]]
          [:div.col
            {:key (gensym)}
            [:div.p-3.border.round
              (if (some? pteam) [:img.img-fluid {:src (str "/img/bb/images/Marker " pteam ".png") :style {:position "absolute" :width "4rem"}}])
              [:h5.text-center (str "Coach " p)] 
              [:div.text-center [:b (if (some? pteam) pteam (if (and isap? (= ap p)) "Choosing Team" "Waiting for Coach Choices"))]]]])]
      [:div.row-fluid [:div.h5.text-center "Starting Rosters"]]
      [:div.row-fluid
        [:div.d-flex.mb-2.justify-content-center
            (for [ t (->> state :teams (map :team) distinct sort)
                    :let [active? (= t team)
                          alliance (->> state :teams (filter #(= (:team %) t)) first :alliance)]]
              [:button.btn.alliance {:key (gensym) :class (if active? "active") :on-click #(swap! bb-app assoc :team t)} [:img.img-fluid {:src (str "/img/bb/images/Marker " t ".png")}]])]
        (if (and isap? (->> team (contains? chosenteams) false?))
            [:div.d-flex.justify-content-center.mb-2 [:button.btn.btn-light {:on-click #(comms/ra-send! {:action :chooseteam :team team})} "Field " team " team."]])  ;[:img.ms-1 {:src (str "/img/bb/images/Marker " team ".png")}]
        [:div.d-flex.justify-content-center
          (doall (for [ tq (->> state :teams (filter #(= (:team %) team)) (sort-by :id) (map :position) frequencies) ]
            [:div.p-2 {:key (gensym)}
              (teamplyrcard {:position (first tq) :qty (last tq) :team team :disabled? (contains? chosenteams team)})]))]]]))

; MAIN

(defn bbmain [ ]
  (-> ((js* "$") "body") 
      (.css "background-image" "url(/img/bb/images/bbpitch.png")
      (.css "background-size" "100%"))
  (let [state (:state @gm)]
    [:div.container-fluid.my-2 {:class (if (-> @bb-app :matchupcommit? some?) "select" ) :on-click #(swap! bb-app dissoc :matchupcommit?)}
      
      (if (:showbigview? @bb-app)
        [:div.bigview {:on-click #(swap! bb-app dissoc :showbigview?)}
          [:div.bigviewcontent
            [:img.bigviewimg {:src (:bigview @bb-app)}]]])
      [:div.d-flex.justify-content-center.mb-2
        [:h4.me-2 "Blood Bowl: Team Manager - " (:status state)]
        [:button.btn.btn-sm.btn-danger {:on-click #(if (js/confirm "Are you sure you want to Quit?") (comms/leavegame @gid))} "Quit"]]
      (case (:status state)
        :setup (setupview state)
        (gameview state))
      (if (true? true)
        [:div.bg-light
          [:div (-> state :players (get @uname) :response str)]
          [:div (str @bb-app)]
          [:div (-> state (dissoc :players :chat :highlights) str)]
          [:div (-> state :highlights :public first str)]
          [:div (-> state :highlights :public second str)]
          [:div (-> state :players first first str)]
          [:div (-> state :players first second str)]
          [:div (-> state :players second first str)]
          [:div (-> state :players second second str)]]
      )
    ]))