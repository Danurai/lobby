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
      
(defn- togglesettings! []
  (if (-> @ra-app :settings :hide) 
      (swap! ra-app update-in [:settings] dissoc :hide) 
      (swap! ra-app assoc-in [:settings :hide] true)))
 
(defn update-card-size [ base ]
  (let [w (* base 20) h (* base 28)] 
    (swap! ra-app assoc-in [:settings :cardsize] {:scale base :w w :h h})
    (.setItem js/localStorage "cardscale" base)))
    
(defn card-click-handler [ gid c ]
  (let [selected? (-> @ra-app :selected (= (:name c)))] ; Q&D
    ;(comms/ra-send! {
    ;  :gid gid
    ;  :action (->> c :type clojure.string/lower-case (str "select") keyword)
    ;  :select? (not selected?)
    ;  :card c})
    (if selected?
      (swap! ra-app dissoc :selected)
      (swap! ra-app assoc  :selected (:name c)))))
      
      
      
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
  ([ gid type card size ]
    (let [ext ".jpg"
          imgsrc (str "/img/ra/" type "-" (:id card) ext)
          scale (case size :lg 1.5 :sm 0.9  1)
          selected? (or (:selected card) (-> @ra-app :selected (= (:name card))))]
      [:img.img-fluid.card.mr-2 {
        :key (gensym)
        :style {
          :display "inline-block"
          :width  (* (-> @ra-app :settings :cardsize :w) scale) 
          :height (* (-> @ra-app :settings :cardsize :h) scale)}
        :src imgsrc
        :class (cond selected? "active" (:target? card) "target" :else nil)
        :on-click #(if (:target? card) (card-click-handler gid card))
        :on-mouse-move (fn [e] (.stopPropagation e) (swap! ra-app assoc :preview imgsrc))
        :on-mouse-out #(swap! ra-app assoc :preview nil)}]))
  ([ gid type card ]
    (rendercard gid type card nil)))
  
(defn rendercardback [ type ]
  (let [scale (case type "pop" 1.5 "magicitem" 0.9  1)]
    [:img.img-fluid.mr-2 {
      :width  (* (-> @ra-app :settings :cardsize :w) scale)
      :height (* (-> @ra-app :settings :cardsize :h) scale)
      :src (str "/img/ra/" type "-back.jpg")}]))  
  
(defn showdata [ gm ]
  [:div 
    [:div.mb-2 (str @ra-app)]
    [:div (str gm)]])
    
  
  
; Section Elements
  
(defn placesofpower [ gid gm ]
  [:div
    (doall (for [pop (-> gm :state :pops)]
      (rendercard gid "pop" pop :lg)))])
        
(defn monuments [ gid gm ]
  [:div
    (doall (for [monument (-> gm :state :monuments :public)]
      (rendercard gid "monument" monument :lg)))])
       
(defn magicitems [ gid gm uname ]
  (let [select? (-> gm :state :players (get uname) :action (= :selectstartitem))]
    [:div.mb-2 {:hidden (not select?)}; show based on select? or global setting
      [:h5.text-center {:hidden (nil? select?)} "Select a Magic Item" 
        [:div (:selected @ra-app)]
        [:button.btn.btn-sm.btn-secondary.float-right {
          :disabled (nil? (:selected @ra-app))
          :on-click #(comms/ra-send! {:gid gid :action :selectstartitem :card (-> @ra-app :selected str)})}
          "OK"]]
      [:div.d-flex
        (doall (for [magicitem (-> gm :state :magicitems)]
          (rendercard gid "magicitem" (assoc magicitem :target? select?))))]]))
        
(defn settings []
  [:div.settings.bg-dark.rounded-left.p-1 {:style {:right (if (-> @ra-app :settings :hide) "-200px" "0px")}}
    [:div [:button.btn.close.mr-1 {:on-click #(togglesettings!)} [:i.fas.fa-times]]]
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
        
(defn tips [ tipsettings ]
  [:div.tip.pt-2
    (if (:active tipsettings)
      [:div.text-center
        [:i.fas.fa-question-circle.mr-2]
        [:span (:tip tipsettings)]])])
        
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

;; TOP ROW ;;

(defn player-resource [ gm ]  
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
          [:td.px-2.text-center.border.border-secondary (+ (if (-> gm :state :p1 (= p)) 1 0) (-> d :public :vp))]])]])

(defn- setupsummary [ gid gm ]
  [:div.p-2
    [:div.h5.text-center "Players making choices..."]
    [:div.d-flex.justify-content-around
      (for [p (:plyrs gm)]
        [:div.mr-2 {:key p}
          [:b.mr-2 {:title (-> gm :state :players (get p) str)} p] 
          (case (-> gm :state :players (get p) :public :mage)
                0 [:span.mr-2 [:span.mr-1 "Mage"][:i.fas.fa-times-circle.text-danger]]
                1 [:span.mr-2 [:span.mr-1 "Mage"][:i.fas.fa-check-circle.text-success]]
                (rendercard gid "mage" (-> gm :state :players (get p) :public :mage) :sm))
          (if-let [mi (-> gm :state :players (get p) :public :magicitem)]
            (rendercard gid "magicitem" mi :sm)
            [:span.mr-2 [:span.mr-1 "Magic Item"][:i.fas.fa-times-circle.text-danger]])
          ])]
    [:div.d-flex [:button.ml-auto.btn.btn-dark.btn-sm {
      :disabled (> (-> gm :plyrs count) (->> gm :state :players (reduce-kv #(update %1 :count + (-> %3 :public :mage)) {:count 0}) :count))
      }
      "Start"]]])
      
(defn player-data [ gid gm ]
  [:div.border.border-secondary.w-100.rounded-right 
    (case (-> gm :state :status)
      :setup (setupsummary gid gm)
      [:div "Active/Selected Player Public Data"]
    )])
          
(defn players [ gid gm uname ]
  [:div.row.mb-2
    [:div.col-12
      [:div.d-flex
        (player-resource gm)
        (player-data gid gm)]]])
            
            
(defn select-start-mage! [ gid card ]
  (swap! ra-app dissoc :selected)
  (comms/ra-send! {:gid gid :action :selectstartmage :card card}))
          
; |X|   | |
(defn player-hand [ gid gm uname ]
  (let [pub (-> gm :state :players (get uname) :public)
        pri (-> gm :state :players (get uname) :private)]
    [:div.h-100.p-1.border.rounded {:style {:background "rgba(50,50,50,0.5)"}}
      (case (-> gm :state :status) 
        :setup (if (= (-> gm :state :players (get uname) :action) :selectmage)
                [:div
                  [:div.h5.text-center "Choose Your Mage"]
                  [:div.d-flex.justify-content-center
                    (doall (for [c (:mages pri)]
                      (rendercard gid "mage" c)))]
                  [:div.d-flex [:button.btn.btn-dark.ml-auto.btn-sm {:disabled (-> @ra-app :selected nil?) :on-click #(select-start-mage! gid (:selected @ra-app))} "OK"]]]
                [:div 
                  [:div.h5.text-center "Mage:"]
                  [:div.d-flex.justify-content-center (rendercard gid "mage" (->> pri :mages (filter :selected) first))]])
        [:div 
          [:div "Player Hand, First Player Token"]
          [:div
            (for [a (-> pri :artifacts)]
              (doall (rendercard gid "artifact" a)))]])]))
      
; | | XX | |
(defn player-board [ gid gm uname ]
  (let [pub (-> gm :state :players (get uname) :public)
        pri (-> gm :state :players (get uname) :private)]
    (if (= (-> gm :state :status) :setup)
        [:div.row-fluid {:style {:height (* 2 (-> @ra-app :settings :cardsize :h))}}
          [:div.h5.text-center "Artifact Deck:"]
          [:div.d-flex.justify-content-center
            (doall (for [c (-> gm :state :players (get uname) :private :artifacts)]
              (rendercard gid "artifact" c)))]]
        [:div "Places of Power, Artifacts and Magic Item"
          
            (rendercard gid "mage" (-> pub :mage))
            (rendercard gid "magicitem" (->> gm :state :magicitems (filter #(= (:owner %) uname)) first))
              ])))
          
(defn ramain [ gid gm uname ]
  (-> ((js* "$") "body") 
      (.css "background-image" "url(/img/ra/ra-bg.png")
      (.css "background-size" "100%"))
  ;(-> ((js* "$") "#navbar") (.attr "hidden" true))
  [:div.container-fluid.my-2 {:on-mouse-move #(swap! ra-app dissoc :preview)}
    (settings)
    ;[:div (-> gm :state :players (get uname) str)]
    [:div.row
      [:div.col-9
        (players gid gm uname)
        [:div.row.justify-content-around
          (magicitems gid gm uname)
          (placesofpower gid gm)
          (monuments gid gm)]]
      [:div.col-3
        [:div.row.d-flex.px-2.h-100
          [:div.col
            [:i.fas.fa-cog.fa-lg.ml-auto {:style {:cursor "pointer"} :on-click #(togglesettings!) :title "Settings"}]]
          ;(tips (-> @ra-app :settings :tips))
          [:button.btn.btn-sm.btn-danger.ml-auto.mt-auto {:on-click #(if (js/confirm "Are you sure you want to Quit?") (comms/leavegame gid))} "Quit"]]
        (let [preview (:preview @ra-app)] 
          [:img.img-fluid.preview.card {:hidden (nil? preview) :src preview}])]]
    [:div.row ;{:style {:position "fixed" :bottom "15px" :width "100%"}}
      [:div.col-3
        (player-hand gid gm uname)]
      [:div.col-6
        (player-board gid gm uname)]
      [:div.col-3 (chat gid (:chat gm))]]
  ])