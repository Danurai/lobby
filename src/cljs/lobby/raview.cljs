(ns lobby.raview
  (:require
    [reagent.core :as r]
    [lobby.model :as model]
    [lobby.comms :as comms]))
    
(def ra-app (r/atom {
  :settings {
    :msg ""
    :hide true
    :tips {
      :active true
      :tip "Welcome to Res Arcana!"}
    :cardsize {
      :base 4 
      :w 80 
      :h 112}}}))

  
(defn rendercard [ type card ext ]
  (let [imgsrc (str "/img/ra/" type "-" (:id card) ext)
        scale (case type "pop" 1.5 "magicitem" 0.9  1)]
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
  
(defn rendercardback [ type ]
  (let [scale (case type "pop" 1.5 "magicitem" 0.9  1)]
    [:img.img-fluid.mr-2 {
      :width  (* (-> @ra-app :settings :cardsize :w) scale)
      :height (* (-> @ra-app :settings :cardsize :h) scale)
      :src (str "/img/ra/" type "-back.png")}]))  
  
(defn placesofpower [ gm ]
  [:div.mb-2
    [:div.d-flex.justify-content-center
      (doall (for [pop (-> gm :state :pops)]
        (rendercard "pop" pop ".png")))]])
        
(defn magicitems [ gm ]
  [:div.mb-2
    [:div.d-flex.justify-content-center
      (doall (for [magicitem (-> gm :state :magicitems)]
        (rendercard "magicitem" magicitem ".png")))]])
    
(defn monuments [ gm ]
  [:div.mb-2
    ;[:div.h5.text-center "Monuments"]
    [:div.d-flex.justify-content-center
      (rendercardback "monument")
      (doall (for [monument (-> gm :state :monuments :public)]
        (rendercard "monument" monument ".jpg")))]])
    
    
(defn setup [ gid gm uname ]
  (let [mydata (get-in gm [:state :players uname])]
    [:div.col.mx-2
      [:div.row.mb-3
        (placesofpower gm)
        (monuments gm)]
      [:div.row ;mage choice
        [:div.col
          [:div.h5.text-center "Setup: Choose your Mage"]
          [:div.d-flex
            (doall (for [mage (-> mydata :private :mages)]
              (rendercard "mage" mage ".jpg")))]]
        [:div.col 
          [:div.h5 "Selected"]
          ;(if (some? selectedmage)
          ;  (rendercard "mage" {:id selectedmage}  ".jpg"))
          ]
        [:div.col
          [:div.d-flex.justify-content-center
            (for [plyr (-> gm :plyrs)]
              [:div.mr-3 {:key plyr}
                [:div [:i.fa-user.fa-2x {:class (if (contains? (-> gm :state :ready) plyr) "fas" "far")}]]
                [:div plyr]])]
          [:div.d-flex 
            [:button.btn.btn-primary.mx-auto {
              :on-click #(comms/ra-send {:gid gid :action :toggleready})
              ;:disabled (nil? selectedmage)
              } 
              (if (contains? (-> gm :state :ready) uname) "Cancel" "Ready")
              ]]]]
      [:div.row ;artifacts
        (doall (for [artifact (-> mydata :private :artifacts) ]
          (rendercard "artifact" artifact ".jpg")))]
      [:div.row.mb-2.tip "Tip: Do you have dragons, creatures, or ways to make gold? This may suggest Places of Power that will work well for you or if you can buy several monuments."]]))
          

(defn showsettings! [ hide? ]
  (if (-> @ra-app :settings :hide) 
      (swap! ra-app update-in [:settings] dissoc :hide) 
      (swap! ra-app assoc-in [:settings :hide] true)))
      
(defn settings []
  [:div.settings.bg-dark.rounded-right.p-1 {:style {:left (if (-> @ra-app :settings :hide) "-200px" "0px")}}
    [:div [:button.btn.close.mr-1 {:on-click #(showsettings! true)} [:i.fas.fa-times]]]
    [:div.my-1 [:b "Settings"]]
    [:div.mx-1
      [:label (str "Image Size: " (-> @ra-app :settings :cardsize :base))]
      [:input.custom-range {
        :type "range" 
        :min 2 :max 6
        :value (-> @ra-app :settings :cardsize :base)
        :on-change #(let [base (-> % .-target .-value)] (swap! ra-app assoc-in [:settings :cardsize] {:base base :w (* base 20) :h (* base 28)})) 
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
    [:div.border.rounded.p-1.chatbox  {:style {:height (* 40 (-> @ra-app :settings :cardsize :base))}}
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
        
(defn resource-icon [ res v ]
  [:div.mr-1 {
    :key (gensym) 
    :style {
      :background-image (str "url(/img/ra/res-" (name res) ".png)")
      :background-size "2rem 2rem"
      :background-repeat "no-repeat"
      :width "32px" 
      :height "2rem"}}
    [:div.text-center {:class (if (= res :death) "text-white")} v]])
        
(defn players [ gid gm uname ]
  (let [plyrs (:plyrs gm)]
    [:div.row.mb-2
      [:div.col-12
        [:table.table-hover
          [:thead [:tr 
            [:td ]
            (for [r ["gold" "calm" "elan" "life" "death"]]
              [:th {:key (gensym)} [:img.resource-sm.mx-1.mb-1 {:src (str "/img/ra/res-" r ".png")}]])]]
          [:tbody
            (for [[p d] (-> gm :state :players)]
              [:tr {:key (gensym)} 
                [:td.px-2.border.border-secondary [:b p]]
                (for [r [:gold :calm :elan :life :death]]
                  [:td.border.border-secondary.text-center {:key (gensym)} (-> d :public :resources r)])])]]
        [:div]]]))

(defn tableau [ gid gm uname ]
  [:div.row
    [:h5 "Tableau"]]
)

(defn showdata [gm]
  [:div 
    [:div (str @ra-app)]
    [:div (str gm)]])
    
(defn tips [ tipsettings ]
  [:div.tip.text-center
    (if (:active tipsettings)
      [:div 
        [:i.fas.fa-question-circle.mr-2]
        [:span (:tip tipsettings)]])])
    
(defn settingscog [] 
  [:i.fas.fa-cog.fa-lg.m-2 {:style {:cursor "pointer"} :on-click #(showsettings! true)}])
    
(defn ramain [ gid gm uname ]
  (-> ((js* "$") "body") 
      (.css "background-image" "url(/img/ra/ra-bg.png")
      (.css "background-size" "100%"))
  ;(-> ((js* "$") "#navbar") (.attr "hidden" true))
  [:div.container-fluid.my-2 {:on-mouse-move #(swap! ra-app dissoc :preview)}
    (settings)
    [:div.row
      [:div.col-9
        (players gid gm uname)
        [:div.row
          [:div.col-8
            (placesofpower gm)
            (magicitems gm)]
          [:div.col-4
            (monuments gm)
            (tips (-> @ra-app :settings :tips))]]]
      [:div.col-3
        (showdata gm)
        (let [preview (:preview @ra-app)] 
          [:img.preview {:hidden (nil? preview) :src preview}])]]
    [:div.row {:style {:position "fixed" :bottom "15px" :width "100%"}}
      [:div.col-3 
        [:b "private"]
        (settingscog)]
      [:div.col-7
        [:span "cards"]]
      [:div.col-2 (chat gid (:chat gm))]]
  ])
;    (settings)
;    [:div.row.border
;      [:div.col-xl-3]
;      [:div.col-xl-6
;        (tableau gid gm uname)]
;      [:div.col-xl-3)]]
;    [:div.row.border  (chat gid gm)]
;    [:small (str @ra-app)]])