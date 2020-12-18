(ns lobby.daview
  (:require 
    [reagent.core :as r]
    [lobby.model :as model :refer [uname gid gm]]
    [lobby.comms :as comms]))
    
    
(def da-app (r/atom {
  :style {
    :cursor "auto"}}))
    
(defn- teambox [ gid id tm plyr? ]
  [:div.col-sm.border.border-light.rounded.m-2.p-2 {
    :key (gensym)
    :on-click #(comms/ra-send! {:gid gid :action :pickteam :team id})
    }
    [:h5 {:style {:color id :text-transform "capitalize"}} id]
    
    (for [m (-> tm :members)]
      [:div {:key (str id (:id m))} (:name m)])
  ])


(defn chooseteams [ gid gm  ]
  (let [owner? (= @uname (:owner gm))]
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
              (if owner? ; and all teams chosen
                [:button.btn.btn-secondary.float-right {
                  :disabled false
                  :on-click #(comms/ra-send! {:gid gid :action :start})
                  } "Enter the Hulk"])]
            ])]
      [:div.row
        (for [team (-> gm :state :teams)]
          (teambox gid (key team) (val team) false)
          )]]))
 
(defn draw_page [ canvas hitmap ]
  (let [ctx (.getContext canvas "2d")
        w   (.-clientWidth  canvas) 
        h   (.-clientHeight canvas)
        formation (-> @gm :state :formation)
        img (.createElement js/document "img")
        ctx2 (.getContext hitmap "2d")
        ]
        
    (.clearRect ctx 0 0 w h)
    (set! (.-src img) "img/da/out.png")
    
    ;(.setTransform ctx 1 0 0 1 0 0)
    (doseq [f formation]
      (.beginPath ctx)
      (.drawImage ctx img 0 0 96 96 (-> f :zone dec (* 150) (+ 27)) 102 96  96)
      
      (set! (.-strokeStyle ctx)  (-> f :marine :squad name str))
      (set! (.-lineWidth ctx)  6)
      (.arc ctx (-> (:zone f) dec (* 150) (+ 75)) 150 46 0 (* 2 Math.PI))
      
      (.stroke ctx)
      
      (.beginPath ctx2)
      (set! (.-fillStyle ctx2)  (-> f :marine :squad name str))
      (.arc ctx2 (-> (:zone f) dec (* 150) (+ 75)) 150 50 0 (* 2 Math.PI))
      (.fill ctx2)
    )
    
    ;(prn (.-getImageData hitmap));
    
    ;(.setTransform ctx -1 0 0 1 0 0)
    ;(.drawImage ctx img 0 0 220 250 -440 0 220 250)
    ;(.restore ctx)
  ))
  
(def mouse (r/atom nil))

(defn mousemove [ evt ]
  (let [ctx2 (.getContext (.getElementById js/document "hitmap") "2d")
        ele (.-target evt)
        x (- (.-pageX evt) (.-offsetLeft ele))
        y (- (.-pageY evt) (.-offsetTop ele))]
    (swap! mouse assoc :x x 
                       :y y
                       :id (.-data (.getImageData ctx2 x y 1 1)))))
  
(defn canvas [ ]
  (let [dom-node (r/atom nil)]
    (r/create-class
     {:component-did-update
        (fn [ this ]
          (draw_page (.getElementById js/document "drawing") (.getElementById js/document "hitmap")))
      :component-did-mount
        (fn [ this ]
          (reset! dom-node (r/dom-node this)))
      :reagent-render
        (fn [ ]
          @gm
          @mouse
          [:div
            [:canvas#drawing.border (if-let [node @dom-node] {
              :width "1000px" 
              :height "300px"
              :on-mouse-move mousemove
              ;:on-click      mouseclick
              ;:on-mouse-out  #(swap! appstate dissoc :mx :my)
              })]
            [:canvas#hitmap.border (if-let [node @dom-node] {
              :width "1000px" 
              :height "300px"
            })]])})))
  
(defn- hulk [ ]
  [:div {:style (:style @da-app)}
    [:div.d-flex
      [:h5 "Welcome to The Hulk"]
      [:div.ml-2 (str @mouse) (get (:id @mouse) 0)]]
    [:div
      [canvas]]
    [:div (str @gm)]])

(defn damain [ ]
  (-> ((js* "$") "body") 
      (.css "background-color" "#222222")
      (.css "color" "grey"))
  [:div.container-fluid.my-3 
    (case (-> @gm :state :status)
      :setup (chooseteams @gid @gm)
      [hulk]
      )
    [:div (-> gm :state :teams str)]
    [:div.py-3
      [:button.btn.btn-sm.btn-dark.float-right {:on-click #(if (js/confirm "Are you sure you want to Quit?") (comms/leavegame @gid))} "Quit"]]])