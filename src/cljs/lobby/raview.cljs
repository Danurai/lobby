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
    
(defn- info-button []
  [:i.fas.fa-info.float-right.mx-2 {
    :style {:cursor "pointer"} 
    :data-target "#infomodal"
    :data-toggle "modal"
    :title "Information"}])
    
    
(defn- resourceimg [ r ]
  [:span
    [:img.resource-sm.mr-1 {:src (str "/img/ra/res-" (-> r first key name) ".png")}]
    [:b (str "x" (-> r first val))]])
    
(defn rendercard 
  ([ card size ]
    (let [imgsrc (str "/img/ra/" (:type card) "-" (:id card) ".jpg")
          scale (case size :lg 1.5 :sm 0.9  1)
          selected? (or (:selected card) (-> @ra-app :selected (= (:uid card))))]
      [:div {:style {:position "relative"}}
        [:div.d-flex.w-100.justify-content-around {
          :style {
            :position "absolute" 
            :top (str (-> @ra-app :settings :cardsize :h (* scale) (/ 2)) "px")
            :z-index "99"}}
          (for [res (:collect-resource card)]
            [:button.btn.btn-light {:key (gensym) :on-click #(comms/ra-send! {:action :collect-resource :resources res :card card})} 
              (resourceimg res)])]
        [:img.img-fluid.card.mx-1.mb-1 {
          :title (str card)
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
          }]]))
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
  
(defn rendercardback [ card size ]
  (let [imgsrc (str "/img/ra/" (:type card) "-back.jpg")
        scale (case size :lg 1.5 :sm 0.9  1)]
    [:img.img-fluid.mx-1.mb-1 {
      :title (str card)
      :key (gensym)
      :width  (* (-> @ra-app :settings :cardsize :w) scale) 
      ;:height (* (-> @ra-app :settings :cardsize :h) scale)
      :style {
        :display "inline-block"
        }
      :src imgsrc
      }]))  
    
       
(defn- ok-btn-handler [ action ]
  (comms/ra-send! {:action action :card (:selected @ra-app)})
  (swap! ra-app dissoc :selected))
    
    

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
    [:div
      (if (= (:action pdata) :selectmagicitem)
        [:div.d-flex.justify-content-center.mb-1
          [:h5.mr-2.my-auto "Choose a new Magic Item"]
          [:button.btn.btn-secondary.mt-auto {:disabled (-> @ra-app :selected nil?) :on-click #(ok-btn-handler (:action pdata))} "OK"]])
      [:div.flex-wrap.justify-content-center {:class (if (or target? (-> @ra-app :settings :showmi :active)) "d-flex" "d-none")}
        (doall (for [magicitem (-> @gm :state :magicitems)]
          (if (-> magicitem :owner some?)
              (render-taken-card magicitem)
              (rendercard (assoc magicitem :target? target?)))))]]))
            
            
(defn- canplay? [ card resources ]
  (->> (:cost card)
       (reduce-kv 
        (fn [m k v] (update m k - v)) resources) 
        vals 
        (apply min)
        (< -1)))
            
(defn- hand [ ]
  (let [resources (-> @gm :state :players (get @uname) :public :resources)]
    [:div.d-flex.w-100 {:style {:position "fixed" :bottom "-75px"}}
      (doall (for [art (map-indexed #(assoc %2 :idx %1) (-> @gm :state :players (get @uname) :private :artifacts))]
        [:div.hand {
          :key (gensym) 
          :style {:left (-> art :idx (* (-> @ra-app :settings :cardsize :w (* 1.5))) (+ 200 ))}
          :class (if (and (-> @gm :state :players (get @uname) :action (= :play)) (canplay? art resources)) "canplay")
          :on-click #(if (and (-> @gm :state :players (get @uname) :action (= :play)) (canplay? art resources)) (comms/ra-send! {:action :playcard :card art :resources (:cost art)}))
          }
          (rendercard art :lg)]))]))
  
(defn- chat [ ]
  [:div.chat
    [:div.border.rounded.p-1.chatbox   {:style {:min-height (* 45 (-> @ra-app :settings :cardsize :scale))}}
      (for [msg (-> @gm :state :chat) :let [{:keys [msg uname timestamp]} msg]]
        [:div {:key (gensym) :style {:word-wrap "break-word"}}
          ;[:span.mr-1 (model/timeformat timestamp)]
          [:b.text-primary.mr-1 uname]
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
    
    
        
(defn resource-table [ plyr ]
  (let [p1 (or (-> @gm :state :pass-to first) (-> @gm :state :plyr-to first))]
    [:table.table-hover
      [:thead [:tr
        [:td ]
        [:td ]
        [:td ]
        (for [r ["gold" "calm" "elan" "life" "death"]]
          [:th {:key (gensym)} [:img.resource-sm.mx-1.mb-1 {:src (str "/img/ra/res-" r ".png")}]])
        [:th.text-center.px-2 "VP"]]]
      [:tbody
        (doall (for [[p d] (-> @gm :state :players)]
          [:tr {:key (gensym) :class (if (= plyr p) "selected")} 
            (case (-> @gm :state :players (get p) :action)
              :selectmagicitem [:td.text-center [:small "Passing"]]
              :pass [:td.text-center [:small "Passed"]]
              [:td.text-center 
                [:div.btn-group
                  [:button.btn.btn-xs.btn-light {:on-click #(comms/ra-send! {:action :done :uname p})} "d"]
                  [:button.btn.btn-xs.btn-light {:on-click #(comms/ra-send! {:action :pass :uname p})} "p"]]])
            [:td.px-2 (if (= p p1) [:img.p1.ml-auto {:src "/img/ra/player-1.png"}])]
            [:td.px-2 [:b.mr-2 p]]
            (doall (for [r [:gold :calm :elan :life :death]]
              [:td.border.border-secondary.text-center {:key (gensym)} (-> d :public :resources r)]))
            [:td.px-2.text-center.border.border-secondary (+ (if (-> @gm :state :p1 (= p)) 1 0) (-> d :public :vp))]]))]]))
      
(defn- resource [[k v]]
  [:div.d-flex.mb-1 {:key (gensym)}
    [:img.resource-sm..my-auto.mr-2 {:src (str "/img/ra/res-" (name k) ".png")}]
    [:b.my-auto.mr-2 v]
    [:div.btn-group.btn-group-xs
      [:button.btn.btn-light {:on-click #(comms/ra-send! {:action :amendresource :resources {k -1}})} [:i.fas.fa-xs.fa-caret-left]]
      [:button.btn.btn-light {:on-click #(comms/ra-send! {:action :amendresource :resources {k  1}})} [:i.fas.fa-xs.fa-caret-right]]]
    ])

(defn- player-resources []
  [:div.d-flex.flex-column.mr-2
    (doall (for [res (-> @gm :state :players (get @uname) :public :resources)]
      (resource res)))
    [:button.btn.btn-sm.btn-secondary.w-100.my-1 {:on-click #(comms/ra-send! {:action :pass})} "Pass"]
    [:button.btn.btn-sm.btn-secondary.w-100.my-1 {:on-click #(comms/ra-send! {:action :done})} "Done"]
  ])
    
(defn- player-board [ p size ]
  (let [pdata (-> @gm :state :players (get p) :public)]
    [:div.d-flex.justify-content-left
      (if (-> pdata :mage :uid) (rendercard (:mage pdata) size))
      (if-let [mi (->> @gm :state :magicitems (filter #(= (:owner %) p)) first)] 
        (if (-> @gm :state :players (get p) :action (= :pass))
          (rendercardback mi size)
          (rendercard mi size)))
      (doall (for [art (:artifacts pdata)] (rendercard art size)))]))
    
(defn- gamestate [ pdata ]
  (let [p (->> @gm :state :players (reduce-kv #(if (= (:action %3) :play) (conj %1 %2) %1) #{}) first)]
    [:div.col-9 {:style {:position "relative"}}
      [:div.row.mb-2
        [:div.col-4 (resource-table p)]
        [:div.col-8 (player-board p :sm)]]      
      [magicitems pdata]
      [:div.d-flex.justify-content-around
        [placesofpower]
        [monuments]]
    ]))
    
    
(defn- modal []
  [:div#infomodal.modal {:tab-index "-1" :role "dialog"}
    [:div.modal-dialog {:role "document"}
      [:div.modal-content 
        [:div#infocarousel.carousel.slide {:data-ride "carousel"}
          [:ol.carousel-indicators
            [:li.active {:data-target "#infocarousel" :data-slide-to "0"}]
            [:li {:data-target "#infocarousel" :data-slide-to "1"}]
            [:li {:data-target "#infocarousel" :data-slide-to "2"}]
            [:li {:data-target "#infocarousel" :data-slide-to "3"}]]
          [:div.carousel-inner
            [:div.carousel-item.active [:img.d-block.w-100 {:src "/img/ra/ref-0.jpg"}]]
            [:div.carousel-item [:img.d-block.w-100 {:src "/img/ra/ref-1.jpg"}]]
            [:div.carousel-item[:img.d-block.w-100 {:src "/img/ra/ref-2.jpg"}]]
            [:div.carousel-item[:img.d-block.w-100 {:src "/img/ra/ref-3.jpg"}]]]
          [:a.carousel-control-prev {:href "#infocarousel" :role "button" :data-slide "prev"}
            [:span.carousel-control-prev-icon]]
          [:a.carousel-control-next {:href "#infocarousel" :role "button" :data-slide "next"}
            [:span.carousel-control-next-icon]]]]]])
    
(defn ramain [ ]
  (-> ((js* "$") "body") 
      (.css "background-image" "url(/img/ra/ra-bg.png")
      (.css "background-size" "100%")
      
      )
  ;(-> ((js* "$") "#navbar") (.attr "hidden" true))
  (let [pdata (-> @gm :state :players (get @uname))]
    [:div.container-fluid.my-2.h-100 {:style {:position "relative"} :on-mouse-move #(reset! ra-preview nil)}
      (modal)
      [:div.row (-> pdata str)]
      [:div.row
        (case (-> @gm :state :status)
          :setup [setup pdata]
          [gamestate pdata])
        [:div.col-3
          [preview]
          [settings]
          [info-button]
          [settings-button]]]
      [:div.row.w-100 {:style {:position "fixed" :bottom "5px" :height (-> @ra-app :settings :cardsize :h (* 2.5))}}
        [:div.col-9
          [:div.d-flex
            [player-resources]
            [player-board@uname :lg]]]
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