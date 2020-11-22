(ns lobby.daview
  (:require 
    [lobby.comms :as comms]))
    
(defn- teambox [ gid id tm plyr? ]
  [:div.col-sm.border.border-light.rounded.m-2.p-2 {
    :key (gensym)
    :on-click #(comms/ra-send! {:gid gid :action :pickteam :team id})
    }
    [:h5 {:style {:color id :text-transform "capitalize"}} id]
    
    (for [m (-> tm :members)]
      [:div {:key (str id (:id m))} (:name m)])
  ])


(defn chooseteams [ gid gm uname ]
  [:div 
    [:div.row.mb-2
      (for [p (:plyrs gm) 
        :let [teams (reduce-kv #(if (= (:cmdr %3) p) (conj %1 (hash-map %2 %3)) %1) [] (-> gm :state :teams))]]
        [:div.col {:key (gensym)}
          [:div.text-center [:h5 p]]
          [:div.row {:style {:min-height "180px"}}
            (for [n (-> gm :state :teamlimit range)
              :let [team (first (get teams n))]]
              (if (nil? team)
                [:div.col.m-2.p-2.border.border-light.rounded.text-center {:key (gensym)} "Empty Team"]
                (teambox gid (key team) (val team) true)
              ))]
          [:div
            (if (= uname (:owner gm)) ; and all teams chosen
              [:button.btn.btn-secondary.float-right {
                :disabled false
                :on-click #(comms/ra-send! {:gid gid :action :start})
                } "Enter the Hulk"])]
          ])]
    [:div.row
      (for [team (-> gm :state :teams)]
        (teambox gid (key team) (val team) false)
        )]
        
  ])

(defn damain [ gid gm uname ]
  (-> ((js* "$") "body") 
      (.css "background-color" "#222222")
      (.css "color" "grey"))
  [:div.container-fluid.my-3 
    (case (-> gm :state :status)
      :setup (chooseteams gid gm uname)
      [:div 
        [:h5 "Welcome to The Hulk"]
        [:div (str gm)]])
    [:div (-> gm :state :teams str)]
    [:div.py-3
      [:button.btn.btn-sm.btn-dark.float-right {:on-click #(if (js/confirm "Are you sure you want to Quit?") (comms/leavegame gid))} "Quit"]]])