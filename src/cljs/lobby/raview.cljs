(ns lobby.raview
  (:require
    [reagent.core :as r]
    [lobby.model :as model :refer [ uname gid gm ]]
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
        
(def ra-preview (r/atom nil))
      
(defn- togglesettings! []
  (if (-> @ra-app :settings :hide) 
      (swap! ra-app update-in [:settings] dissoc :hide) 
      (swap! ra-app assoc-in [:settings :hide] true)))
 
(defn update-card-size [ base ]
  (let [w (* base 10) h (* base 14)] 
    (swap! ra-app assoc-in [:settings :cardsize] {:scale (/ base 2) :w w :h h})
    (.setItem js/localStorage "cardscale" (/ base 2))))
    
(defn card-click-handler [ gid c ]
  (let [selected? (-> @ra-app :selected (= (:name c)))] ; Q&D
    ;(comms/ra-send! {
    ;  :gid gid
    ;  :action (->> c :type clojure.string/lower-case (str "select") keyword)
    ;  :select? (not selected?)
    ;  :card c})
    (if selected?
      (swap! ra-app dissoc :selected)
      (swap! ra-app assoc  :selected (:uid c)))))
      
  
(defn- settings []
  [:div.settings.bg-dark.rounded-left.p-1 {:style {:right (if (-> @ra-app :settings :hide) "-200px" "0px")}}
    [:div [:button.btn.close.mr-1 {:on-click #(togglesettings!)} [:i.fas.fa-times]]]
    [:div.my-1 [:b "Settings"]]
    [:div.mx-1
      [:label (str "Image Scale: " (-> @ra-app :settings :cardsize :scale))]
      [:input.custom-range {:type "range" :min 2 :max 12
        :value (-> @ra-app :settings :cardsize :scale (* 2))
        :on-change #(update-card-size (-> % .-target .-value))}]
      [:div.d-flex 
        [:div "Show Tips"]
        [:i.ml-auto.fas.fa-2x.text-light {
          :class (if (-> @ra-app :settings :tips :active) "fa-toggle-on" "fa-toggle-off")
          :style {:cursor "pointer"}
          :on-click #(if (-> @ra-app :settings :tips :active) 
                         (swap! ra-app update-in [:settings :tips] dissoc :active) 
                         (swap! ra-app assoc-in  [:settings :tips :active] true))}]]
      [:div.d-flex
        [:div "Show Magic Items"][:i.ml-auto.fas.fa-2x.text-light {
            :class (if (-> @ra-app :settings :showmi :active) "fa-toggle-on" "fa-toggle-off")
            :style {:cursor "pointer"}
            :on-click #(if (-> @ra-app :settings :showmi :active) 
                           (swap! ra-app update-in [:settings :showmi] dissoc :active) 
                           (swap! ra-app assoc-in  [:settings :showmi :active] true))}]]]
  ])
                         
(defn- settings-button []
  [:i.fas.fa-cog.fa-lg.float-right {
    :style {:cursor "pointer"} 
    :on-click #(togglesettings!) 
    :title "Settings"}])

(defn rendercard 
  ([ card size ]
    (let [imgsrc (str "/img/ra/" (:type card) "-" (:id card) ".jpg")
          scale (case size :lg 1.5 :sm 0.9  1)
          selected? (or (:selected card) (-> @ra-app :selected (= (:uid card))))]
      [:img.img-fluid.card.mx-1.mb-1 {
        ;:title (str card)
        :key (gensym)
        :width  (* (-> @ra-app :settings :cardsize :w) scale) 
          ;:height (* (-> @ra-app :settings :cardsize :h) scale)
        :style {
          :display "inline-block"
          }
        :src imgsrc
        :class (cond selected? "active" (:target? card) "target" :else nil)
        :on-click #(if (:target? card) (card-click-handler @gid card))
        :on-mouse-move (fn [e] (.stopPropagation e) (reset! ra-preview imgsrc))
        :on-mouse-out #(reset! ra-preview nil)
        }]))
  ([ card ]
    (rendercard card nil)))
    
(defn- render-taken-card 
  ([ card size ]
    (let [imgsrc (str "/img/ra/" (:type card) "-" (:id card) ".jpg")
          scale (case size :lg 1.5 :sm 0.9  1)]
      [:img.img-fluid.card.mx-1.mb-1 {
        ;:title (str card)
        :key (gensym)
        :width  (* (-> @ra-app :settings :cardsize :w) scale) 
        :style {
          :border "4px dashed goldenrod"
          :opacity "0.6"
          :display "inline-block"
          ;:height (* (-> @ra-app :settings :cardsize :h) scale)
        }
        :src imgsrc}]
      ))
  ([ card ]
    (render-taken-card card nil)))
  
;(defn rendercardback [ type ]
;  (let [scale (case type "pop" 1.5 "magicitem" 0.9  1)]
;    [:img.img-fluid.mr-2 {
;      :width  (* (-> @ra-app :settings :cardsize :w) scale)
;      :height (* (-> @ra-app :settings :cardsize :h) scale)
;      :src (str "/img/ra/" type "-back.jpg")}]))  
    

; Section Elements


(defn- preview [ ]
  (if-let [prv @ra-preview]
    [:img.img-fluid.preview.card {:src prv}])) ;(* (-> @ra-app :settings :cardsize :w) 3)}}])

  
(defn- placesofpower []
  [:div.d-flex.flex-wrap.justify-content-center
    (doall (for [pop (-> @gm :state :pops)]
      (rendercard pop :lg)))])
        
(defn- monuments []
  [:div.d-flex.flex-wrap.justify-content-center
    (doall (for [monument (-> @gm :state :monuments :public)]
      (rendercard monument :lg)))])
       
(defn- magicitems [ pdata ]
  (let [target? (contains? #{:selectstartitem :selectmagicitem} (:action pdata))]
    [:div.flex-wrap.justify-content-center {:class (if (or target? (-> @ra-app :settings :showmi :active)) "d-flex" "d-none")}
      (doall (for [magicitem (-> @gm :state :magicitems)]
        (if (-> magicitem :owner some?)
            (render-taken-card magicitem)
            (rendercard (assoc magicitem :target? target?)))))]))
            
(defn- hand [ ]
  [:div.d-flex.w-100 {:style {:position "fixed" :bottom "-75px"}}
    (doall (for [art (map-indexed #(assoc %2 :idx %1) (-> @gm :state :players (get @uname) :private :artifacts))]
      [:div.hand {:key (gensym) :style {:left (-> art :idx (* 100))}}
        (rendercard art :lg)]))])
  
(defn- chat [ ]
  [:div.chat
    [:div.border.rounded.p-1.chatbox   {:style {:min-height (* 45 (-> @ra-app :settings :cardsize :scale))}}
      (for [msg (-> @gm :state :chat) :let [{:keys [msg uname timestamp]} msg]]
        [:div {:key (gensym) :style {:word-wrap "break-word"}}
          [:span.mr-1 (model/timeformat timestamp)]
          [:b.text-primary.mr-1 (str uname ":")]
          [:span msg]])
          ]
    [:form {:on-submit (fn [e] (.preventDefault e) (comms/sendmsg! (:msg @ra-app) @gid ) (swap! ra-app assoc :msg ""))}
      [:div.input-group
        [:input.form-control.form-control-sm.bg-light {
          :type "text" :placeholder "Type to chat"
          :value (:msg @ra-app)
          :on-change #(swap! ra-app assoc :msg (-> % .-target .-value))}]
        [:span.input-group-append [:button.btn.btn-sm.btn-outline-secondary {:type "btn"} [:i.fas.fa-arrow-right]]]]]])
       
; SETUP
       
(defn- ok-btn-handler [ action ]
  (comms/ra-send! {:action action :card (:selected @ra-app)})
  (swap! ra-app dissoc :selected))
    
(defn- setup [ pdata ]
  [:div.col-9
    [:div.d-flex.justify-content-around
      [:div
        [:div 
          [:h5.text-center "Mage"]
          (case (:action pdata)
            :selectmage [:div.d-flex.flex-wrap.justify-content-center (doall (for [mg (-> pdata :private :mages)] (rendercard mg)))]
            (rendercard (->> pdata :private :mages (filter #(= (:uid %) (-> pdata :public :mage))) first)))]]
      [:div
        [:h5.text-center "Artifacts"]
        [:div.d-flex.flex-wrap.justify-content-center (doall (for [a (-> pdata :private :artifacts)] (rendercard a :sm)))]]]
    [:div.d-flex.border.border-dark.rounded.bg-dark.p-2.my-3
      [:h4.text-light.my-auto.mr-3 (case (:action pdata) :selectmage [:span [:i.fas.fa-arrow-up.mr-2] "Select Mage"] :selectstartitem [:span "Select Starting Item" [:i.fas.fa-arrow-down.ml-2]] "Waiting for other Players")]
      [:button.btn.btn-light {:disabled (-> @ra-app :selected nil?) :on-click #(ok-btn-handler (:action pdata))} [:b "OK"]]]
    [:div.row
      [:div.col
        [:div.d-flex.justify-content-center
          [magicitems pdata]]
        [:div.d-flex.justify-content-around
          [placesofpower]
          [monuments]]
        ]]])
    
    
        
(defn player-resource [ plyr ]  
  [:table.table-hover
    [:thead [:tr 
      [:td ]
      [:td ]
      (for [r ["gold" "calm" "elan" "life" "death"]]
        [:th {:key (gensym)} [:img.resource-sm.mx-1.mb-1 {:src (str "/img/ra/res-" r ".png")}]])
      [:th.text-center.px-2 "VP"]]]
    [:tbody
      (doall (for [p (-> @gm :state :turnorder) :let [d (-> @gm :state :players (get p))]]
        [:tr {:key (gensym) :class (if (= plyr p) "bg-light")} 
          [:td.px-2 (if (-> @gm :state :turnorder first (= p)) [:img.p1.ml-auto {:src "/img/ra/player-1.png"}])]
          [:td.px-2 [:b.mr-2 p]]
          (doall (for [r [:gold :calm :elan :life :death]]
            [:td.border.border-secondary.text-center {:key (gensym)} (-> d :public :resources r)]))
          [:td.px-2.text-center.border.border-secondary (+ (if (-> @gm :state :p1 (= p)) 1 0) (-> d :public :vp))]]))]])
    
(defn- player-board [ p size ]
  (let [pdata (-> @gm :state :players (get p) :public)]
    [:div.d-flex.justify-content-left
      (rendercard (:mage pdata) size)
      (rendercard (->> @gm :state :magicitems (filter #(= (:owner %) p)) first) size)
      (doall (for [art (:artifacts pdata)] (rendercard art size)))]))
    
(defn- gamestate [ ]
  (let [p (->> @gm :state :players (reduce-kv #(if (= (:action %3) :play) (conj %1 %2) %1) #{}) first)]
    [:div.col-9 {:style {:position "relative"}}
      [:div.row.mb-2
        [:div.col-3 (player-resource p)]
        [:div.col-9 (player-board p :sm)]]      
      [magicitems (-> @gm :state :players (get @uname) :action)]
      [:div.d-flex.justify-content-around
        [placesofpower]
        [monuments]]
    ]))
    
(defn ramain [ ]
  (-> ((js* "$") "body") 
      (.css "background-image" "url(/img/ra/ra-bg.png")
      (.css "background-size" "100%"))
  ;(-> ((js* "$") "#navbar") (.attr "hidden" true))
  (let [pdata (-> @gm :state :players (get @uname))]
    [:div.container-fluid.my-2.h-100 {:style {:position "relative"} :on-mouse-move #(reset! ra-preview nil)}
      [:div.row
        (case (-> @gm :state :status)
          :setup [setup pdata]
          [gamestate])
        [:div.col-3
          [preview]
          [settings]
          [settings-button]]]
      [:div.row.w-100 {:style {:position "fixed" :bottom "5px" :height (-> @ra-app :settings :cardsize :h (* 2.5))}}
        [:div.col-9
          [player-board@uname :lg]]
        [:div.col-3
          [chat]]]
      [hand]
      [:div.d-flex
        [:button.btn.btn-sm.btn-danger.ml-auto.mt-auto {:on-click #(if (js/confirm "Are you sure you want to Quit?") (comms/leavegame @gid))} "Quit"]]]))
      
;; Player Data
;;;  (def playerdata {
;;;    :public {
;;;      :mage nil
;;;      :artifacts nil
;;;      :monuments nil
;;;      :pops nil
;;;      :resources {
;;;        :gold 1
;;;        :calm 1
;;;        :elan 1
;;;        :life 1
;;;        :death 1
;;;      }
;;;    }
;;;    :private { ; player knows
;;;      :mages nil
;;;      :artifacts nil
;;;    }
;;;    :secret { ; no-one knows
;;;      :discard nil
;;;    }})
      
      
      
      
      
      
      
      
      
      
      
      
      
      
      
      
      
      
      
      
      
      
      
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

;  
;(defn showdata [ gm ]
;  [:div 
;    [:div.mb-2 (str @ra-app)]
;    [:div (str gm)]])
;    
;  
;  
;        
;        
;(defn tips [ tipsettings ]
;  [:div.tip.pt-2
;    (if (:active tipsettings)
;      [:div.text-center
;        [:i.fas.fa-question-circle.mr-2]
;        [:span (:tip tipsettings)]])])
;        
;        
;

;            
;            
;          
;; |X|   | |
;(defn player-hand [ gid gm uname ]
;  (let [pub (-> gm :state :players (get uname) :public)
;        pri (-> gm :state :players (get uname) :private)]
;    [:div.h-100.p-1.border.rounded {:style {:background "rgba(50,50,50,0.5)"}}
;      (case (-> gm :state :status) 
;        :setup (if (= (-> gm :state :players (get uname) :action) :selectmage)
;                [:div
;                  [:div.h5.text-center "Choose Your Mage"]
;                  [:div.d-flex.justify-content-center
;                    (doall (for [c (:mages pri)]
;                      (rendercard gid "mage" c)))]
;                  [:div.d-flex [:button.btn.btn-dark.ml-auto.btn-sm {:disabled (-> @ra-app :selected nil?) :on-click #(select-start-mage! gid (:selected @ra-app))} "OK"]]]
;                [:div 
;                  [:div.h5.text-center "Mage:"]
;                  [:div.d-flex.justify-content-center (rendercard gid "mage" (->> pri :mages (filter :selected) first))]])
;        [:div 
;          [:div "Player Hand, First Player Token"]
;          [:div
;            (for [a (-> pri :artifacts)]
;              (doall (rendercard gid "artifact" a)))]])]))
;      
;; | | XX | |
;(defn player-board [ gid gm uname ]
;  (let [pub (-> gm :state :players (get uname) :public)
;        pri (-> gm :state :players (get uname) :private)]
;    (if (= (-> gm :state :status) :setup)
;        [:div.row-fluid {:style {:height (* 2 (-> @ra-app :settings :cardsize :h))}}
;          [:div.h5.text-center "Artifact Deck:"]
;          [:div.d-flex.justify-content-center
;            (doall (for [c (-> gm :state :players (get uname) :private :artifacts)]
;              (rendercard gid "artifact" c)))]]
;        [:div "Places of Power, Artifacts and Magic Item"
;          
;            (rendercard gid "mage" (-> pub :mage))
;            (rendercard gid "magicitem" (->> gm :state :magicitems (filter #(= (:owner %) uname)) first))
;              ])))
;          
;          
;          
;(defn ramainx [ gid gm uname ]
;  (-> ((js* "$") "body") 
;      (.css "background-image" "url(/img/ra/ra-bg.png")
;      (.css "background-size" "100%"))
;  ;(-> ((js* "$") "#navbar") (.attr "hidden" true))
;  [:div.container-fluid.my-2 {:on-mouse-move #(swap! ra-app dissoc :preview)}
;    (settings)
;    ;[:div (-> gm :state :players (get uname) str)]
;    [:div.row
;      [:div.col-9
;        (players gid gm uname)
;        [:div.row.justify-content-around
;          (magicitems gid gm uname)
;          (placesofpower gid gm)
;          (monuments gid gm)]]
;      [:div.col-3
;        [:div.row.d-flex.px-2.h-100
;          [:div.col
;            [:i.fas.fa-cog.fa-lg.ml-auto {:style {:cursor "pointer"} :on-click #(togglesettings!) :title "Settings"}]]
;          ;(tips (-> @ra-app :settings :tips))
;          [:button.btn.btn-sm.btn-danger.ml-auto.mt-auto {:on-click #(if (js/confirm "Are you sure you want to Quit?") (comms/leavegame gid))} "Quit"]]
;        (let [preview (:preview @ra-app)] 
;          [:img.img-fluid.preview.card {:hidden (nil? preview) :src preview}])]]
;    [:div.row ;{:style {:position "fixed" :bottom "15px" :width "100%"}}
;      [:div.col-3
;        (player-hand gid gm uname)]
;      [:div.col-6
;        (player-board gid gm uname)]
;      [:div.col-3 (chat gid (:chat gm))]]
;  ])