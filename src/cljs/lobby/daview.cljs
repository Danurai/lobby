(ns lobby.daview
  (:require 
    [reagent.core :as r]
    [lobby.comms :as comms]
    [lobby.model :as model]))
    
(defonce formation (r/atom nil))
    
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
  
(defn draw_page [ canvas f ]
  (let [ctx (.getContext canvas "2d")
        w (.-clientWidth canvas) 
        h (.-clientHeight canvas)]
    (.clearRect ctx 0 0 w h)
    
    (.drawImage ctx (.getElementById js/document "tm") 0 0 170 207)
    
    (set! (.-fillStyle ctx) "rgb(200,0,0)")
    (set! (.-strokeStyle ctx) "rgb(200,0,0)")
    ;(.fillRect ctx 0,0,100,100)
    ;(.strokeText ctx (-> f count str) 10 10 )
    ))
         
(defn canvasclass [ gid gm uname ]
  (let [dom-node (r/atom nil)]
    (r/create-class
     {:component-did-update
        (fn [ this ]
          (draw_page (.getElementById js/document "drawing") (-> gm :state :formation)))
      :component-did-mount
        (fn [ this ]
          (reset! dom-node (r/dom-node this)))
      :reagent-render
        (fn [ ]
          @model/app
          (let [zones (-> gm :state :formation count)]
            [:div ;{:style {:overflow-x "scroll"}}
              [:canvas#drawing.border (if-let [node @dom-node]  {
                :style {:width "1000px" :height "500px"}
                ;:on-click      mouseclick
                ;:on-mouse-move mousemove
                ;:on-mouse-out  #(swap! appstate dissoc :mx :my)
              })]]))})))

(defn damain [ gid gm uname ]
  (-> ((js* "$") "body") 
      (.css "background-color" "#222222")
      (.css "color" "grey"))
  [:div.container-fluid.my-3 
    [:div ;{:display "none"}
      [:img#tm {:src "/img/da/term.jpg" :width "170px" :height "207px"}]]
    (case (-> gm :state :status)
      :setup (chooseteams gid gm uname)
      [:div
        [:h5 "Welcome to The Hulk"]
        [canvasclass gid gm uname]
        [:div.my-3 (str gm)]])
    [:div (-> gm :state :teams str)]
    [:div.py-3
      [:button.btn.btn-sm.btn-dark.float-right {:on-click #(if (js/confirm "Are you sure you want to Quit?") (comms/leavegame gid))} "Quit"]]])