(ns lobby.daview
  (:require 
    [reagent.core :as r]
    [lobby.model :as model :refer [uname gid gm]]
    [lobby.comms :as comms]
    [lobby.dagfx :as dagfx]))
    
    
(def da-app (r/atom {
  :style {
    :cursor "auto" :font-family "Orbitron"}}))
    
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
 
(defn draw_page [ canvas hitmap img ]
  (let [ctx (.getContext canvas "2d")
        ctx2 (.getContext hitmap "2d")
        w   (.-clientWidth  canvas) 
        h   (.-clientHeight canvas)
        formation (-> @gm :state :formation)
        ]
        
    (.clearRect ctx 0 0 w h)
    
    (dagfx/blips ctx (-> @gm :state :blips) (-> formation count (* 150) (+ 75)))
    
    ;(.setTransform ctx 1 0 0 1 0 0)
    (doseq [f formation]
      (let [x (-> (count formation) (- (:zone f)) (* 150))
            hl (if (-> @da-app :highlight :zone (= (:zone f))) (:highlight @da-app))]
            
            
        (dagfx/marine ctx img hl (:marine f) x)
        
        (doseq [t (map-indexed #(assoc %2 :id %1) (:terrain f))]
          (dagfx/threat ctx x (if (-> t :facing (= :top)) 75 205) t))
        
        (dagfx/swarm ctx (filter #(= (:facing %) :top) (:swarm f)) x 10)  
        (dagfx/swarm ctx (filter #(= (:facing %) :bot) (:swarm f)) x 280)  
        
        ; Hitmap - marines
        (.beginPath ctx2)
        ;rgba(zone, type, id,255)
        (set! (.-fillStyle ctx2) (str "rgba(" (:zone f) ",1," (-> f :marine :id) ",255)"))
        (.arc ctx2 (+ x 75) 150 50 0 (* 2 Math.PI))
        (.fill ctx2)
    ))
  ))
  
(def mouse (r/atom nil))

(defn mousemove [ evt ]
  (let [ctx2 (.getContext (.getElementById js/document "hitmap") "2d")
        ele  (.-target evt)
        x    (- (.-pageX evt) (.-offsetLeft ele))
        y    (- (.-pageY evt) (.-offsetTop ele))
        rgba (zipmap [:zone :type :id :show] (map #(int %) (clojure.string/split (.-data (.getImageData ctx2 x y 1 1)) #",")))]
    (swap! mouse assoc :x x 
                       :y y
                       :rgba rgba)
    (swap! da-app assoc-in [:style :cursor] (if (= 255 (:show rgba)) "pointer" "auto"))
    (swap! da-app assoc :highlight rgba)
    ))
  
(defn canvas [ ]
  (let [dom-node (r/atom nil)]
    (r/create-class
     {:component-did-update
        (fn [ this ]
          (draw_page (.getElementById js/document "drawing") (.getElementById js/document "hitmap") (.getElementById js/document "img")))
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
              :hidden true
            })]])})))
  
(def order-icon {
  :support "fa-plus-square"
  :move    "fa-arrows-alt"
  :attack  "fa-crosshairs"
})
  
(defn convert
"Converts single element to hiccup or reagent"
[pre txt]
  (if-let [symbol (re-matches #"\[(\w+)\]" txt)]
    [:i {:class (str pre (second symbol))}]
    txt))

(defn makespan [res] 
  (apply conj [:span] 
    (reduce 
      #(if (string? %2) 
        (if (string? (last %1)) 
          (conj (-> %1 drop-last vec) (str (last %1) %2)) 
          (conj (vec %1) %2)) 
        (conj (vec %1) %2)) [""] res)))
        
(defn- markdown [ txt ]
  (->> txt
      (re-seq #"\[\w+\]|\w+|." )
      (map #(convert "lotr-type-" %))
      makespan))
(defn- orders [ ]
  [:div.d-flex.justify-content-around
    (for [[k team] (-> @gm :state :teams)]
      (if (= (:cmdr team) @uname)
        (doall (for [order (:orders team)]
          [:div {:key (->> order :id (str "order_")) :style {:width "18rem" :color k}}
            [:div.card-title.text-center 
              [:span (:name order)]]
            [:div.card-title.text-center
              [:i.fas.me-2 {:class (-> order :type order-icon)}]
              [:span.me-2 (->> order :id )]]
            [:div.card-body.text-secondary   (:text order)]])))) ])
  
(defn- hulk [ ]
  (let [ui8ca (:id @mouse)]
    [:div {:style (:style @da-app)}
      [:div.d-flex
        [:h5 "Welcome to The Hulk"]]
      [canvas]
      [orders]
      [:div (str @gm)]]))
      
(defn damain [ ]
  (-> js/document .-body .-style .-backgroundColor (set! "#222222") )
  (-> js/document .-body .-style .-color (set! "grey") )

  [:div.container-fluid.my-3 
    [:img#img {:hidden true :src "/img/da/out.png"}]
    (case (-> @gm :state :status)
      :setup (chooseteams @gid @gm)
      [hulk]
      )
    [:div (-> @gm :state :teams str)]
    [:div.py-3
      [:button.btn.btn-sm.btn-dark.float-right {:on-click #(if (js/confirm "Are you sure you want to Quit?") (comms/leavegame @gid))} "Quit"]]])
      
   
;(defn- canvas2 [ ]
;  (let [dom-node (r/atom nil)]
;    (r/create-class
;      {:component-did-update
;        (fn [ this ]
;          (let [canvas (.getElementById js/document "drawing")
;                img    (.getElementById js/document "img")
;                ctx    (.getContext canvas "2d")
;                w      (.-clientWidth  canvas) 
;                h      (.-clientHeight canvas)]
;            (set! (.-src img) "img/da/out.png")
;            (.clearRect ctx 0 0 w h)
;            (.drawImage ctx img 0 0 96 96 0 0 96  96)
;            (swap! mouse assoc :id (clojure.string/split (.-data (.getImageData ctx 50 50 1 1)) #"," ))
;          ))
;      :component-did-mount
;        (fn [ this ]
;          (reset! dom-node (r/dom-node this)))
;      :reagent-render
;        (fn [ ]
;          @gm
;          [:div
;            [:canvas#drawing.border (if-let [node @dom-node] {:width "1000px" :height "300px"})]
;            [:img#img {:hidden true :src ""}]])})))
  
  
     
      
;(defn damain [ ]
;  [:div.container-fluid.my-3
;    [:div (-> @mouse str)]
;    [canvas2]])