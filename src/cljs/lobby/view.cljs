(ns lobby.view
	(:require 
    [reagent.core :as r]
    [lobby.raview :refer [ramain]]
    [lobby.daview :refer [damain]]
    [lobby.bbview :refer [bbmain]]
		[lobby.comms :as comms]
		[lobby.model :as model :refer [uname gid gm]]
  ))

(defn- isAI? [ p ]  ;; Doesn't work for test scripts
  (-> @model/app :user-hash (get p) nil?))
       
(defn lobby [ gid gm uname ]
  (let [owner? (= uname (:owner gm))]
    [:div.col-7
      [:div.border.rounded.p-2.bg-light
        [:h4.d-flex.mb-3
          [:span.badge.bg-secondary.me-2 (:game gm)]
          [:div.mt-auto (:title gm)]
          (if (:private? gm) [:i.fas.fa-lock.text-secondary.ms-3])]
        [:div.d-flex.justify-content-between
          [:div.d-flex.mb-3.mx-auto
            (doall (for [p (:plyrs gm)]
              [:div.mx-2 {:key p :class (if (and owner? (isAI? p)) "clickable") :on-click #(if (and owner? (isAI? p)) (comms/removeai gid p))}
                [:div.text-center 
                  [:i.fas.fa-user.fa-lg.mx-auto {:class (cond  (= (:owner gm) p) "text-primary" (isAI? p) "text-warning")}]]
                [:div p]]))]
          (if (and owner? (-> (->> @model/app :plyrs (map #(isAI? %)) frequencies) (get true) nil?))
            [:div [:button.btn.btn-success {:disabled (= (-> gm :plyrs count) (-> gm :maxp)) :on-click #(comms/addai gid)} [:i.fas.fa-plus.me-1] "Add AI"]])]
        [:div.d-flex
          [:button.btn.btn-sm.btn-danger {:on-click #(comms/leavegame gid)} "Leave"]
          (if owner? [:button.btn.btn-primary.ms-auto {:disabled (< (-> gm :plyrs count) (-> gm :minp)) :on-click #(comms/startgame gid)} "Start"])]]]))
   

(defn- createform []
  (let [formdata (r/atom {
          :game "Res Arcana" ;(if-let [g1 (-> @model/app :gamelist first)] (key g1) "") 
          :title (str (.. js/document (getElementById "loginname") -textContent) "'s Game") 
          :private? false})]
    (fn []
      [:form {:on-submit #(.preventDefault %)}
        [:div.d-flex
          [:div.me-1
            [:label.my-auto.me-1 "Game"]
            [:select.form-control.form-control-sm.me-2 {
              :value (:game @formdata)
              :on-change #(swap! formdata assoc :game (.. % -target -value))}
              [:option "Res Arcana"]
              ;[:option "BBTM"]
              ;[:option "Death Angel"]
              ;(for [g (:gamelist @model/app)] [:option {:key (gensym)} (key g)])
            ]]
          [:div.me-1 {:style {:flex-grow 1}}
            [:label.my-auto.me-1 "Title"]
            [:input.form-control.form-control-sm.me-2 {
              :data-lpignore true
              :type "text"
              :value (:title @formdata)
              :on-change #(swap! formdata assoc :title (.. % -target -value))}]]
          [:div.d-flex
            [:button.btn.btn-sm.btn-primary.mt-auto {:on-click #(comms/creategame @formdata)}  "Create"]]]])))
    
(defn- create []
  [:div.border.rounded.p-2.mb-2.bg-light
    [:h5 "Create a new Game"]
    [createform]])
      
(defn join []
  (let [gms (:games @model/app)]
    [:div.border.rounded.p-2.mb-2.bg-light
      [:div [:span.h5.me-2 "Join"][:span (str "(" (count gms) ")")]]
      [:div.list-group
        (for [[k g] gms :let [full? (= (-> g :plyrs count) (-> g :maxp))]]
          [:div.list-group-item.list-group-item-action {:key k}
            [:div.d-flex
              [:div 
                [:span.badge.badge-warning.me-2 (:game g)]
                [:span.me-2 (:title g)]
                [:small.me-2 (str "Host: " (:owner g))]
                (for [p (:plyrs g)]
                  [:i.fas.fa-user.me-2 {:key (gensym) :title p :class (if (= p (:owner g)) "text-primary")}])]
              [:div.ms-auto
                (cond
                  (:started g)  "In Progress"    ; Watch?
                  (:private? g) "Private Game"  ; Enter Code
                  full?         "Game full"
                  :default      [:button.btn.btn-sm.btn-primary {:on-click #(comms/joingame k)} "Join"])]]])]]))
        
(defn createjoin []
  [:div.col-7
    (create)
    (join)])
    
(defn gamehooks []
  (case (:game @gm)
    "Res Arcana"  (ramain)
    "Death Angel" (damain)
    "BBTM"        (bbmain)
    [:div.row-fluid
      [:h5 "Game not found"]
      [:button.btn.btn-sm.btn-danger {:on-click #(comms/leavegame @gid)} "Leave"]]))

(defn- chat []
  (let [msg (r/atom "")]
    (fn []
      [:div.border.rounded.bg-light.p-2.chat
        [:div.chatbox ;{:style {:height "200px" :font-size "0.8rem" :overflow-y "scroll" :display "flex" :flex-direction "column-reverse"}}
          (for [msg (:chat @model/app) :let [{:keys [msg uname timestamp]} msg]]
            [:div {:key (gensym) :style {:word-wrap "break-word"}}
              [:span.me-1 (model/timeformat timestamp)]
              [:b.text-primary.me-1 (str uname ":")]
              [:span msg]])]
        [:form {:on-submit (fn [e] (.preventDefault e) (comms/sendmsg! @msg) (reset! msg ""))}
          [:div.input-group
            [:input.form-control.bg-light {
              :type "text" :placeholder "Type to chat"
              :value @msg
              :on-change #(reset! msg (-> % .-target .-value))}]
            [:button.btn.btn-secondary {:type "btn"} [:i.fas.fa-arrow-right]]]]])))

(defn- gamelobby [] 
  [:div.container.my-2 
    ;[:div.row [:div [:small (str (dissoc @model/app :chat))]] [:button.btn.btn-sm.btn-danger {:on-click #(comms/reset)} "Reset"]]
    [:div.row
      (if @gm
        (lobby @gid @gm @uname)
        (createjoin))    
      [:div.col
        [:div.border.rounded.p-2.mb-2.bg-light
          [:h5 "Online:"]
          (for [conn (:user-hash @model/app)]
            [:div {:key (gensym)} (key conn)])]
        [chat]]]])
      
(defn main []
  (-> js/document .-body (.removeAttribute "style"))
  (-> js/document .-body .-style .-backgroundColor (set! "slategrey"))
  (if (:state @gm)
      (gamehooks)
      (gamelobby)))