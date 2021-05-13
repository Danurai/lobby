(ns lobby.view
	(:require 
    [reagent.core :as r]
    [lobby.raview :refer [ramain]]
    [lobby.daview :refer [damain]]
    [lobby.bbview :refer [bbmain]]
		[lobby.comms :as comms]
		[lobby.model :as model :refer [uname gid gm]]
  ))
    
(defn createform []
  (let [formdata (r/atom {
          :game "Res Arcana" ;(if-let [g1 (-> @model/app :gamelist first)] (key g1) "") 
          :title (str (.. js/document (getElementById "loginname") -textContent) "'s Game") 
          :private? false})]
    (fn []
      [:form.form-inline {:on-submit #(.preventDefault %)}
        [:label.my-auto.mr-1 "Game"]
        [:select.form-control.form-control-sm.mr-2 {
          :value (:game @formdata)
          :on-change #(swap! formdata assoc :game (.. % -target -value))}
          [:option "BBTM"]
          [:option "Res Arcana"]
          [:option "Death Angel"]
          ;(for [g (:gamelist @model/app)] [:option {:key (gensym)} (key g)])
        ]
        [:label.my-auto.mr-1 "Title"]
        [:input.form-control.form-control-sm.mr-2 {
          :data-lpignore true
          :type "text"
          :value (:title @formdata)
          :on-change #(swap! formdata assoc :title (.. % -target -value))}]
        [:button.btn.btn-sm.btn-primary.my-auto {:on-click #(comms/creategame @formdata)}  "Create"]])))
    
(defn create []
  [:div.row.mb-2
    [:div.col
      [:div.mb-1
        [:h5 "Create"]]
      [:div#createform
        [createform]]]])
      
(defn join []
  (let [gms (:games @model/app)]
    [:div.row
      [:div.col 
        [:div [:span.h5.mr-2 "Join"][:span (str "(" (count gms) ")")]]
        [:div.list-group
          (for [[k g] gms :let [full? (= (-> g :plyrs count) (-> g :maxp))]]
            [:div.list-group-item.list-group-item-action {:key k}
              [:div.d-flex
                [:div 
                  [:span.badge.badge-warning.mr-2 (:game g)]
                  [:span.mr-2 (:title g)]
                  [:small.mr-2 (str "Host: " (:owner g))]
                  (for [p (:plyrs g)]
                    [:i.fas.fa-user.mr-2 {:key (gensym) :title p :class (if (= p (:owner g)) "text-primary")}])]
                [:div.ml-auto
                  (cond
                    (:started g) "In Progress"    ; Watch?
                    (:private? g) "Private Game"  ; Enter Code
                    full? "Game full"
                    :default [:button.btn.btn-sm.btn-primary {:on-click #(comms/joingame k)} "Join"])]]])]]]))
          
(defn createjoin []
  [:div.col-sm-8
    (create)
    (join)])
    
(defn isAI [ p ]
  (-> @model/app :user-hash (get p) nil?))
    
(defn gamelobby [ gid gm uname ]
  [:div.col-sm-8
    [:div.d-flex
      [:h4 (:title gm)
        (if (:private? gm) [:i.fas.fa-lock.text-secondary.ml-3])]
      [:h4.ml-auto [:span.badge.badge-warning (:game gm)]]]
    [:div.d-flex.mb-3
      (for [p (:plyrs gm)]
        [:div.mr-2 {:key p}
          [:div.d-flex [:i.fas.fa-user.fa-lg.mx-auto {:class (if (= (:owner gm) p) "text-primary")}]]
          [:div p]])
      (if (-> (->> @model/app :plyrs (map #(isAI %)) frequencies) (get true) nil?) 
        [:button.btn.btn-sm.btn-success.ml-auto {:disabled (= (-> gm :plyrs count) (-> gm :maxp)) :on-click #(comms/addai gid)} [:i.fas.fa-plus.mr-1] "Add AI"])]
    [:div.d-flex
      [:button.btn.btn-sm.btn-danger {:on-click #(comms/leavegame gid)} "Leave"]
      (if (= uname (:owner gm))
          [:button.btn.btn-primary.ml-auto {:disabled (< (-> gm :plyrs count) (-> gm :minp)) :on-click #(comms/startgame gid)} "Start"])]])
    
(defn gamehooks [ ]
  (case (:game @gm)
    "Res Arcana"  (ramain)
    "Death Angel" (damain)
    "BBTM"        (bbmain)
    [:div.row-fluid
      [:h5 "Game not found"]
      [:button.btn.btn-sm.btn-danger {:on-click #(comms/leavegame @gid)} "Leave"]]))
    
(defn main []
  (let [msg (r/atom "")]
    (fn []
      (-> ((js* "$") "body") (.removeAttr "style"))
      [:div
        (if (:state @gm)
          (gamehooks)
          [:div.container.my-3 {:style {:min-height "400px"}}
            [:div.row
              (if @gm
                (gamelobby @gid @gm @uname)
                (createjoin))    
              [:div.col-sm-4.h-100
                [:div.p-2.border.rounded.mb-2 {:style {:height "50%"}}
                  [:h5 "Connected"]
                  (for [conn (:user-hash @model/app)]
                    [:div {:key (gensym)} (key conn)])]
                [:div
                  [:div.border.rounded.mb-1.p-1 {:style {:height "200px" :font-size "0.8rem" :overflow-y "scroll" :display "flex" :flex-direction "column-reverse"}}
                    (for [msg (:chat @model/app) :let [{:keys [msg uname timestamp]} msg]]
                      [:div {:key (gensym) :style {:word-wrap "break-word"}}
                        [:span.mr-1 (model/timeformat timestamp)]
                        [:b.text-primary.mr-1 (str uname ":")]
                        [:span msg]])]
                  [:form {:on-submit (fn [e] (.preventDefault e) (comms/sendmsg! @msg) (reset! msg ""))}
                    [:div.input-group
                      [:input.form-control.bg-light {
                        :type "text" :placeholder "Type to chat"
                        :value @msg
                        :on-change #(reset! msg (-> % .-target .-value))}]
                      [:span.input-group-append [:button.btn.btn-outline-secondary {:type "btn"} [:i.fas.fa-arrow-right]]]]]]]]
            [:div.row                      
              [:div [:small (str (dissoc @model/app :chat))]]
              [:button.btn.btn-sm.btn-danger {:on-click #(comms/reset)} "Reset"]]])])))