(ns lobby.raview
  (:require
    [reagent.core :as r]
    [lobby.model :as model]
    [lobby.comms :as comms]))
    
; Atom and Atom Functions

(def ra-app (let [scale (or (.getItem js/localStorage "cardscale") 4)]
  (r/atom {
    :settings {
      :msg ""
      :hide true
      :cardsize {
        :scale scale
        :w (* scale 20)
        :h (* scale 28)}
      :tips {
        :active true
        :tip "Do you have dragons, creatures, or ways to make gold? This may suggest Places of Power that will work well for you or if you can buy several monuments."}}})))
      
(defn- showsettings! [ hide? ]
  (if (-> @ra-app :settings :hide) 
      (swap! ra-app update-in [:settings] dissoc :hide) 
      (swap! ra-app assoc-in [:settings :hide] true)))
 
(defn update-card-size [ base ]
  (let [w (* base 20) h (* base 28)] 
    (swap! ra-app assoc-in [:settings :cardsize] {:scale base :w w :h h})
    (.setItem js/localStorage "cardscale" base)))

; Element functions 


;(defn resource-icon [ res v ]
;  [:div.mr-1 {
;    :key (gensym) 
;    :style {
;      :background-image (str "url(/img/ra/res-" (name res) ".jpg)")
;      :background-size "2rem 2rem"
;      :background-repeat "no-repeat"
;      :width "32px" 
;      :height "2rem"}}
;    [:div.text-center {:class (if (= res :death) "text-white")} v]])

(defn rendercard 
  ([ type card size ]
    (let [ext ".jpg"
          imgsrc (str "/img/ra/" type "-" (:id card) ext)
          scale (case size :lg 1.5 :sm 0.9  1)]
      [:img.img-fluid.card.mr-2 {
        :key (gensym)
        :style {:display "inline-block"}
        :width  (* (-> @ra-app :settings :cardsize :w) scale)
        :height (* (-> @ra-app :settings :cardsize :h) scale)
        :src imgsrc
        :class (if (= (str type (:id card)) (:selected @ra-app)) "active")
        ;:on-click #(let [sel (str type (:id card))] 
        ;            (if (= sel (:selected @ra-app))
        ;              (swap! ra-app dissoc :selected)
        ;              (swap! ra-app assoc :selected sel)))
        :on-mouse-move (fn [e] (.stopPropagation e) (swap! ra-app assoc :preview imgsrc))
        :on-mouse-out #(swap! ra-app assoc :preview nil)}]))
  ([ type card ]
    (rendercard type card nil)))
  
(defn rendercardback [ type ]
  (let [scale (case type "pop" 1.5 "magicitem" 0.9  1)]
    [:img.img-fluid.mr-2 {
      :width  (* (-> @ra-app :settings :cardsize :w) scale)
      :height (* (-> @ra-app :settings :cardsize :h) scale)
      :src (str "/img/ra/" type "-back.jpg")}]))  
  
(defn showdata [gm]
  [:div 
    [:div.mb-2 (str @ra-app)]
    [:div (str gm)]])
    
(defn tips [ tipsettings ]
  [:div.tip.pt-2
    (if (:active tipsettings)
      [:div.text-center
        [:i.fas.fa-question-circle.mr-2]
        [:span (:tip tipsettings)]])])
    
(defn settingscog [] 
  [:i.fas.fa-cog.fa-lg.m-2.float-right {:style {:cursor "pointer"} :on-click #(showsettings! true) :title "Settings"}])
  
  
; Section Elements
  
(defn placesofpower [ gm ]
  [:div
    (doall (for [pop (-> gm :state :pops)]
      (rendercard "pop" pop :lg)))])
        
(defn monuments [ gm ]
  [:div
    (doall (for [monument (-> gm :state :monuments :public)]
      (rendercard "monument" monument :lg)))])
       
(defn magicitems [ gm ]
  [:div.mb-2
    [:div.d-flex.justify-content-center
      (doall (for [magicitem (-> gm :state :magicitems)]
        (rendercard "magicitem" magicitem)))]])
      
(defn settings []
  [:div.settings.bg-dark.rounded-right.p-1 {:style {:left (if (-> @ra-app :settings :hide) "-200px" "0px")}}
    [:div [:button.btn.close.mr-1 {:on-click #(showsettings! true)} [:i.fas.fa-times]]]
    [:div.my-1 [:b "Settings"]]
    [:div.mx-1
      [:label (str "Image Size: " (-> @ra-app :settings :cardsize :scale))]
      [:input.custom-range {
        :type "range" 
        :min 2 :max 6
        :value (-> @ra-app :settings :cardsize :scale)
        :on-change #(update-card-size (-> % .-target .-value))
        }]
      [:div.d-flex 
        [:div "Show Tips"]
        [:i.ml-auto.fas.fa-2x.text-light {
          :class (if (-> @ra-app :settings :tips :active) "fa-toggle-on" "fa-toggle-off")
          :style {:cursor "pointer"}
          :on-click #(if (-> @ra-app :settings :tips :active) 
                         (swap! ra-app update-in [:settings :tips] dissoc :active) 
                         (swap! ra-app assoc-in [:settings :tips :active] true))}]]
      ]])
        
(defn chat [ gid chat ]
  [:div.chat
    [:div.border.rounded.p-1.chatbox ;  {:style {:min-height (* 45 (-> @ra-app :settings :cardsize :scale))}}
      (for [msg chat :let [{:keys [msg uname timestamp]} msg]]
        [:div {:key (gensym) :style {:word-wrap "break-word"}}
          [:span.mr-1 (model/timeformat timestamp)]
          [:b.text-primary.mr-1 (str uname ":")]
          [:span msg]])
          ]
    [:form {:on-submit (fn [e] (.preventDefault e) (comms/sendmsg! (:msg @ra-app) gid ) (swap! ra-app assoc :msg ""))}
      [:div.input-group
        [:input.form-control.form-control-sm.bg-light {
          :type "text" :placeholder "Type to chat"
          :value (:msg @ra-app)
          :on-change #(swap! ra-app assoc :msg (-> % .-target .-value))}]
        [:span.input-group-append [:button.btn.btn-sm.btn-outline-secondary {:type "btn"} [:i.fas.fa-arrow-right]]]]]])
        
        
; Player Data
;;  (def playerdata {
;;    :public {
;;      :mage nil
;;      :artifacts nil
;;      :monuments nil
;;      :pops nil
;;      :resources {
;;        :gold 1
;;        :calm 1
;;        :elan 1
;;        :life 1
;;        :death 1
;;      }
;;    }
;;    :private { ; player knows
;;      :mages nil
;;      :artifacts nil
;;    }
;;    :secret { ; no-one knows
;;      :discard nil
;;    }})
        
(defn players [ gid gm uname ]
  (let [plyrs (:plyrs gm)]
    [:div.row.mb-2
      [:div.col-12
        [:table.table-hover
          [:thead [:tr 
            [:td ]
            (for [r ["gold" "calm" "elan" "life" "death"]]
              [:th {:key (gensym)} [:img.resource-sm.mx-1.mb-1 {:src (str "/img/ra/res-" r ".png")}]])
            [:th.text-center.px-2 "VP"]]]
          [:tbody
            (for [p (-> gm :state :turnorder) :let [d (-> gm :state :players (get p))]]
              [:tr {:key (gensym)} 
                [:td.px-2.border.border-secondary.d-flex [:b.mr-2 p] (if (-> gm :state :p1 (= p)) [:img.p1.ml-auto {:src "/img/ra/player-1.png"}])]
                (for [r [:gold :calm :elan :life :death]]
                  [:td.border.border-secondary.text-center {:key (gensym)} (-> d :public :resources r)])
                [:td.px-2.text-center.border.border-secondary (+ (if (-> gm :state :p1 (= p)) 1 0) (-> d :public :vp))]])]]
        [:div]]]))
    
(defn playerresource [ gid gm uname ]
  [:div.h-100.p-1.border.rounded {:style {:background "rgba(50,50,50,0.5)"}}
    [:button.btn.btn-danger {:on-click #(comms/leavegame gid)} "Quit"]
    (settingscog)
    (if (= (-> gm :state :status) :setup)
        [:div.row-fluid
          [:div.h5.text-center "Choose Your Mage"]
          [:div.d-flex.justify-content-center
            (doall (for [c (-> gm :state :players (get uname) :private :mages)]
              (rendercard "mage" c)))]]
        [:div (str gm) "Player Hand, First Player Token"])])
    
(defn playercards [ gid gm uname ]
  (if (= (-> gm :state :status) :setup)
      [:div.row-fluid
        [:div.h5.text-center "Artifact Deck:"]
        [:div.d-flex.justify-content-center
          (doall (for [c (-> gm :state :players (get uname) :private :artifacts)]
            (rendercard "artifact" c)))]
        (tips (-> @ra-app :settings :tips))]
      [:div (str gm) "Places of Power, Artifacts and Magic Item"]))
          
          
(defn ramain [ gid gm uname ]
  (-> ((js* "$") "body") 
      (.css "background-image" "url(/img/ra/ra-bg.png")
      (.css "background-size" "100%"))
  ;(-> ((js* "$") "#navbar") (.attr "hidden" true))
  [:div.container-fluid.my-2 {:on-mouse-move #(swap! ra-app dissoc :preview)}
    (settings)
    [:div.row
      [:div.col-10
        (players gid gm uname)
        [:div.row.justify-content-around
          (placesofpower gm)
          (monuments gm)]]
      [:div.col-2
        (let [preview (:preview @ra-app)] 
          [:img.img-fluid.preview.card {:hidden (nil? preview) :src preview}])]]
    [:div.row {:style {:position "fixed" :bottom "15px" :width "100%"}}
      [:div.col-3
        (playerresource gid gm uname)]
      [:div.col-7
        (playercards gid gm uname)]
      [:div.col-2 (chat gid (:chat gm))]]
  ])