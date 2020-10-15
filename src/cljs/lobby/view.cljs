(ns lobby.view
	(:require 
    [reagent.core :as r]
		[lobby.model :as model]
		[lobby.comms :as comms]
    [lobby.raview :refer [ramain]]))
    
(defn createform []
  (let [formdata (r/atom {:game "Res Arcana" :title (str (.. js/document (getElementById "loginname") -textContent) "'s Game") :private? false})]
    (fn []
      [:form.form-inline {:on-submit #(.preventDefault %)}
        [:label.my-auto.mr-1 "Game"]
        [:select.form-control.form-control-sm.mr-2 {
          :value (:game @formdata)
          :on-change #(swap! formdata assoc :game (.. % -target -value))}
          [:option "Res Arcana"]]
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
          (for [g gms]
            [:div.list-group-item.list-group-item-action {:key (:gid g)} 
              [:div.d-flex
                [:div 
                  [:span.badge.badge-warning.mr-2 (:game g)]
                  [:span.mr-2 (:title g)]
                  [:small.mr-2 (str "Host: " (:owner g))]
                  (for [p (:plyrs g)]
                    [:i.fas.fa-user.mr-2 {:key (gensym) :title p :class (if (= p (:owner g)) "text-primary")}])]
                (if (and (-> g :status nil?) (-> g :private? false?))
                    [:button.btn.btn-sm.btn-primary.ml-auto 
                      {:on-click #(comms/joingame (:gid g))}
                      "Join"])]])]]]))
          
(defn createjoin []
  [:div.col-sm-8
    (create)
    (join)])
    
(defn gamelobby [ uname gm ]
  [:div.col-sm-8
    [:div.d-flex
      [:h4 (:title gm)
        (if (:private? gm) [:i.fas.fa-lock.text-secondary.ml-3])]
      [:h4.ml-auto [:span.badge.badge-warning (:game gm)]]]
    [:div.d-flex.mb-3
      (for [p (:plyrs gm)]
        [:div.mr-2 {:key p}
          [:div.d-flex [:i.fas.fa-user.fa-lg.mx-auto {:class (if (= uname p) "text-primary")}]]
          [:div p]])]
    [:div.d-flex
      [:button.btn.btn-sm.btn-danger {:on-click #(comms/leavegame (:gid gm))} "Leave"]
      (if (= uname (:owner gm))
          [:button.btn.btn-primary.ml-auto {:on-click #(comms/startgame (:game gm) (:gid gm))} "Start"])]
    ])
    
(defn gamehooks [ gm uname ]
  (case (:game gm)
    "Res Arcana" (ramain gm uname)
    [:div.row-fluid
      [:h5 "Game not found"]
      [:button.btn.btn-sm.btn-danger {:on-click #(comms/leavegame (:gid gm))} "Leave"]]))
          
(defn main []
  (let [uname (.. js/document (getElementById "loginname") -textContent)
        gm    (->> @model/app :games (filter #(contains? (-> % :plyrs set) uname)) first)]
    (-> ((js* "$") "body") (.removeAttr "style"))
    [:div
      (if (:state gm)
        (gamehooks gm uname)
        [:div.container.my-3
          [:div.row 
            (if gm
              (gamelobby uname gm)
              (createjoin))
            [:div.col-sm-4
              [:div.p-2.border.rounded
                [:h5 "Connected"]
                (for [conn (:user-hash @model/app)]
                  [:div {:key (gensym)} (key conn)])]]]])
      [:div [:small (str @model/app)]]  
      ;[:button.btn {:on-click #(comms/sendone)} "Send Req"]
    ]))