(ns lobby.raview
  (:require
    [reagent.core :as r]
    [lobby.model :as model :refer [ uname gid gm ]]
    [lobby.comms :as comms]))
    
;;;;; ATOMS ;;;;;

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

;;;;; FUNCTIONS ;;;;;

(def resource-clr {
  :gold "goldenrod"
  :calm "darkcyan"
  :life "green"
  :elan "red"
  :death "black"})

(defn- checkmark [ state? ] [:i.fas {:class (if state? "fa-check text-success" "fa-times text-danger")}])

(defn- imgsrc 
  ([ card back? ]
    (str "/img/ra/" (:type card) "-" (if back? "back" (:id card)) ".jpg"))
  ([ card ] (imgsrc card false)))

(defn- select-btn-handler [ action card ]
  (comms/ra-send! {:action action :card (:uid card)}))

(defn- card-click-handler [ card ]
  (swap! ra-app assoc :bigview card)
  (swap! ra-app assoc :showbigview? true))

(defn- select-card-keys [ c ]
  (select-keys c [:name :type :id]))

(defn- parse-msg [ msg all-cards ]
  (let [card-names (->> all-cards (map :name) (clojure.string/join "|"))
        split-patt (re-pattern (str "(?i)" card-names "|\\w+|."))
        match-patt (re-pattern (str "(?i)" card-names))
        grp        (re-seq split-patt msg)]
    (if grp
        [:span
          (for [m grp]
            (if (re-matches match-patt m)
                (let [c (->> all-cards (filter #(re-matches (re-pattern (str "(?i)" m)) (:name %))) first)]
                  [:span.card-link {:key (gensym) :href m :on-click #(card-click-handler c)} m]) 
                [:span {:key (gensym)} m]))]
        [:span msg])))

(defn- chat [ ]
  (let [gs (:state @gm)]
    [:div.chat2 
      [:div.chatbox.p-1
        (for [msg (-> @gm :state :chat) :let [{:keys [msg uname timestamp]} msg]]
          [:div {:key (gensym) :style {:word-wrap "break-word"} :class (if (nil? uname) "bg-secondary text-light")}
            ;[:span.mr-1 (model/timeformat timestamp)]
            [:b.text-primary.me-1 uname]
            [:span (parse-msg msg (:allcards gs))]])]
    [:form {:on-submit (fn [e] (.preventDefault e) (comms/sendmsg! (:msg @ra-app) @gid ) (swap! ra-app assoc :msg ""))}
      [:div.input-group
        [:input.form-control.form-control-sm.bg-light {
          :type "text" :placeholder "Type to chat"
          :value (:msg @ra-app)
          :on-change #(swap! ra-app assoc :msg (-> % .-target .-value))}]
        [:span.input-group-append [:button.btn.btn-sm.btn-outline-secondary.bg-light {:type "btn"} [:i.fas.fa-arrow-right]]]]]]))


(defn- getmagicitem [ items uname ]
  (->> items  
        (filter #(-> % :owner (= uname))) 
        first))

(defn- hovercard []
  (if-let [bigview (-> @ra-app :bigview)]
    [:div.row
      [:img.img-fluid {:src (imgsrc bigview)}]]))

(defn resource-icon [ type n ]
  (let [color (case type (:elan :death) "white" "black")
        scale 1]
    [:div.text-center.mx-1 {
        :key (gensym)
        :style {
          :background-image (str "url(img/ra/res-" (name type) ".png")
          :background-size "contain"
          :background-repeat "no-repeat"
          :background-position-y "50%"
          :color color
          :font-size (str scale "em")
          :width     (str (* scale 1.1) "em")
        }
      }
      n]))

(defn- render-card-simple 
  ([ card size back? ]
    (let [scale (case size :lg 1.3 :sm 0.7 1)]
      [:img.card.clickable.me-1 {
        :key (gensym)
        :title (if back? (:type card) (str card))
        :width  (* (-> @ra-app :settings :cardsize :w) scale) 
        :class (if (:disabled? card) "disabled")
        :src (imgsrc card back?)
        :on-click #(card-click-handler card)
        :on-mouse-move (fn [e] (.stopPropagation e) (swap! ra-app assoc :bigview card))
      }])
    )
  ([ card size ]  (render-card-simple card size false))
  ([ card ]       (render-card-simple card nil false)))

(defn render-card
  ([ card size ]
    (let [scale (case size :lg 1.3 :sm 0.7  1)
          selected? nil]  ;(or (:selected card) (-> @ra-app :selected (= (:uid card))))]
      [:div.clickable {:style {:position "relative"} :key (gensym)}
        [:div.d-flex.w-100.justify-content-around {
            :style {
              :position "absolute" 
              :top (str (-> @ra-app :settings :cardsize :h (* scale) (/ 2)) "px")
              :z-index "50"}}
            (for [res (:collect-resource card)]
              [:button.btn.btn-light {:key (gensym) :on-click #(comms/ra-send! {:action :collect-resource :resources res :card card})} 
                (str res)])
          ]
        [:img.img-fluid.card.mx-1.mb-1 {
          :title (str card)
          :width  (* (-> @ra-app :settings :cardsize :w) scale) 
          ;:height (* (-> @ra-app :settings :cardsize :h) scale)
          :style {
            :display "inline-block"
            }
          :src (imgsrc card)
          :class (cond selected? "active" (:target? card) "target" (:disabled? card) "disabled" :else nil)
          :on-touch-start #(card-click-handler card)
          :on-click       #(card-click-handler card)
          :on-mouse-move (fn [e] (.stopPropagation e) (swap! ra-app assoc :bigview card))
          ;:on-mouse-out #(swap! ra-app dissoc :bigview)
          }]]))
  ([ card ]
    (render-card card nil)))

(defn- render-cards 
  ([ cards size simple?]
    [:div.d-flex.justify-content-center {:class (if (-> cards first :type (= "magicitem")) "flex-wrap")}
      (doall (for [ c cards ] (if simple? (render-card-simple c size) (render-card c size))))])
  ([ cards size ] (render-cards cards size false))
  ([ cards ] (render-cards cards nil false)))
        
(defn- bigview [ pdata ] ; Content also driven via @ra-app
  [:div.bigview {:hidden (-> @ra-app :showbigview? nil?) :on-click #(swap! ra-app dissoc :showbigview?)}
    [:div.bigviewcontent
      (if-let [card (-> @ra-app :bigview)]
        [:div.d-flex.justify-content-center.h-100
          [:img.h-100.p-1 {:src (imgsrc card) }]
          (when (:target? card)
            [:div 
              [:div [:button.btn.btn-light.mb-1.w-100 {:on-click #(select-btn-handler (:action pdata) card)} "Select"]]
              [:div [:button.btn.btn-light.mb-1.w-100 "Cancel"]]
            ])])]])

(defn- pop-mon-mi-row [ selectmagicitem? ] 
  [:div.row
    [:div.col-6
      [:div.text-center [:b "Places of Power"]] 
      (-> @gm :state :pops (render-cards :lg))]
    [:div.col-3 
      [:div.text-center [:b "Monuments"]] 
      ;(render-cards (->> @gm :state :monuments :public (apply conj [{:type "monument" :id "back"}])))
      (-> @gm :state :monuments :public (render-cards :lg))
      ]
    (when (not= :setup (-> @gm :state :status) )
      [:div.col-3
        [:div.text-center [:b "Magic Items"]]
        (render-cards (->> @gm 
                          :state 
                          :magicitems 
                          (map 
                            #(if (-> % :owner some?) 
                                 (assoc % :disabled? true) 
                                (if selectmagicitem? 
                                    (assoc % :target? true) 
                                    %)))) :sm true)])])

(defn- opponentdisplay [ gs uname ]
  (let [magicitems (:magicitems gs)]
    [:div.d-flex
      (doall (for [k (:display-to gs) 
                    :let [ v       (get (:players gs) k) 
                           passed? (= :pass (:action v))
                           active? (contains? #{:selectmagicitem :play} (:action v))
                           focus?  (= k (:focus @ra-app))]]
        [:div.border.border-dark.rounded.m-1 {
            :key (gensym) 
            :style {:flex-grow (cond (= uname k) 0 focus? 99 :else 1)} 
            :class (if active? "focus")
            :on-click #(if (= k (:focus @ra-app)) (swap! ra-app dissoc :focus) (swap! ra-app assoc :focus k))
          }
          [:div.d-flex.justify-content-around.mx-1
            [:div.text-center (str k (if-let [mage (-> v :public :mage :name)] (str " - " mage)))]
            (if (not= k uname) 
                (if (= :collect (:phase gs))
                    [:div.ms-auto [:i.fas {:class (if (:collected? v) "fa-check text-success" "fa-times text-danger")}]]))
            ]
          [:div.d-flex.mb-1.ms-1
            (if (-> v :public :mage map?) (-> v :public :mage (render-card-simple :sm)))
            (if-let [item (getmagicitem magicitems k)] (render-card-simple item :sm passed?))
            ]
          (if (not= k uname) [:div.d-flex.justify-content-center (for [ [r n] (-> v :public :resources)] (resource-icon r n) )])
        ]))
    ]))

(defn- playerdisplay [ gs uname pdata ]
  [:div.d-flex
    [:div.border.border-dark.rounded.m-1.px-2 {:style {:flex-grow 1 } :class (case (:action pdata) :pass "passed" :play "focus" "")}
      [:div.d-flex.justify-content-around
        [:h3 (str uname (if-let [mage (-> pdata :public :mage :name)] (str " - " mage)))]
        (cond
          (= :collect (:phase gs))  (let [collected? (:collected? pdata)]
                                      [:div
                                        [:span.me-1 "Collect Resources"]
                                        [:button.btn.btn-outline-secondary.mt-1 {
                                            :class (if collected? "active") 
                                            :on-click #(comms/ra-send! {:action :collected})} 
                                          [:span.me-1 "Ready?"] ]
                                        ])
          (= :selectmagicitem (:action pdata)) [:h3 "Select a new Magic Item"]
          :else [:div ]
        )]
      [:div.d-flex
        [:div.btn-group-vertical
          [:button.btn.btn-sm.btn-outline-secondary {:title "Pass" :on-click #(comms/ra-send! {:action :pass})} [:i.fas.fa-sign-out-alt.text-primary]]
          [:button.btn.btn-sm.btn-outline-secondary {:title "View Discard Pile"} [:i.fas.fa-trash-alt.text-danger]]
          ]
        (-> pdata :public :mage render-card)
        (-> gs :magicitems (getmagicitem uname) render-card)
      ]
      [:div.d-flex.justify-content-between {:style {:position "relative"}}
        [:div "Hand"]
        [:div.d-flex.justify-content-center (for [ [r n] (-> pdata :public :resources)] (resource-icon r n) )]
        [:div ]
      
        [:div.d-flex.hand 
          (doall (for [c (-> pdata :private :artifacts)] (render-card c :lg) ))]
    ]]
  ])


;;;;; STATES Setup / Play ;;;;;;

(defn- setup [ state uname pdata ]
  [:div.col-12.ra-main
    [opponentdisplay (->> state :players (remove #(-> % key (= uname))))]
    [:div.row
      [:div.col-2
        [:div.text-center "Mage"]
        [:div.d-flex.justify-content-center
          [:img.img-fluid {:src (if-let [mgid (-> pdata :public :mage)]
                                  (->> pdata :private :mages (filter #(= (:uid %) mgid)) first imgsrc)
                                  "/img/ra/mage-back.jpg")}]]]
      [:div.col-8
        (case (-> pdata :action)
          :selectmage 
            [:div.row.focus [:h3.text-center "Select Mage"] [render-cards (-> pdata :private :mages)]]
          :selectstartitem
            [:div.row.focus [:h3.text-center "Select Starting Magic Item"] [render-cards (map #(assoc % :target? true) (->> @gm :state :magicitems (remove #(-> % :owner some?) )))]]
          [:div.h4.text-center "Waiting for other players to make choices"])
        [:div.h4.text-center "Artifact Deck"]
        [render-cards (-> pdata :private :artifacts)]]
      [:div.col-2 [hovercard]]
    ]
    (pop-mon-mi-row false)])

(defn- gamestate [ gs uname pdata ]
  [:div.row.ra-main
    (opponentdisplay gs uname)
    (pop-mon-mi-row (= :selectmagicitem (:action pdata)))
    (playerdisplay gs uname pdata)
  ])

;;;;; MAIN ;;;;;

(defn ramain [ ]
  (-> js/document .-body .-style .-backgroundImage (set! "url(/img/ra/ra-bg.png)"))
  (-> js/document .-body .-style .-backgroundSize (set! "100%"))
  ;(-> ((js* "$") "#navbar") (.attr "hidden" true))
  (let [gs    (:state @gm)
        pdata (-> gs :players (get @uname))]
    [:div.container-fluid
      (bigview pdata)
      [chat]
      [:div.d-flex.justify-content-between.ra-main.m-1
        [:div ]
        [:h2.mt-auto.mb-0 "Res Arcana - " (if (= (:status gs) :setup) "Setup" (-> gs :phase name clojure.string/capitalize (str " phase")))]
        [:button.btn.btn-sm.btn-danger {:on-click #(if (js/confirm "Are you sure you want to Quit?") (comms/leavegame @gid))} [:i.fas.fa-lg.fa-times-circle]]]
      (case (-> @gm :state :status)
        :setup [setup gs @uname pdata]
        [gamestate gs @uname pdata])
      ]))
      
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