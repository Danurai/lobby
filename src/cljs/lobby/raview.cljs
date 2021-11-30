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

;;;;; FUNCTIONS ;;;;;
(defonce resource-list [:gold :calm :life :elan :death])
(def resource-clr {
  :gold "goldenrod"
  :calm "darkcyan"
  :life "green"
  :elan "red"
  :death "black"})

(defn- hidemodal []
  (swap! ra-app dissoc :modal))

(defn- imgsrc 
  ([ card back? ]
    (str "/img/ra/" (:type card) "-" (if back? "back" (:id card)) ".jpg"))
  ([ card ] (imgsrc card false)))

(defn- resource-icon [ type n ]
  (let [color (case type (:elan :death) "white" "black")
        scale 1]
    [:div.text-center.mx-1.mt-1 {
        :key (gensym)
        :style {
          :background-image (str "url(img/ra/res-" (name type) ".png")
          :background-size "contain"
          :background-repeat "no-repeat"
          :background-position-y "0%"
          :color color
          :width     (str (* scale 1.1) "em")
        }
      }
      n]))

(defn- select-btn-handler [ action card ]
  (comms/ra-send! {:action action :card (:uid card)}))

(defn- card-click-handler [ card ]
  (swap! ra-app assoc :modal {:show? true :card card}))

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
    [:div.ra-chat.border.rounded.p-1
      [:div.chatbox.p-1.mb-1
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
          [:button.btn.btn-sm.btn-secondary {:type "btn"} [:i.fas.fa-arrow-right]]]]]))


(defn- getmagicitem [ items uname ]
  (->> items  
        (filter #(-> % :owner (= uname))) 
        first))

(defn- render-card-simple 
  ([ card size back? ]
    (let [scale (case size :lg 1.3 :sm 0.7 1)]
      [:div.clickable {:key (gensym) :style {:position "relative"}}
        [:img.card {
          :title (if back? (:type card) (str card))
          :width  (* (-> @ra-app :settings :cardsize :w) scale) 
          :class (cond (:disabled? card) "disabled" (:target? card) "target" :else "")
          :src (imgsrc card back?)
          :on-click        (fn [e] (.stopPropagation e) (card-click-handler card))
          :on-touch-start  (fn [e] (.stopPropagation e) (card-click-handler card))
          ;:on-mouse-move  (fn [e] (.stopPropagation e) (swap! ra-app assoc :modal card))
        }]])
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
            [:div.btn-grp-sm
              (for [res (:collect-resource card)]
                [:button.btn.btn-light {:key (gensym) :on-click #(comms/ra-send! {:action :collect-resource :resources res :card card})} 
                  (resource-icon (-> res keys first) (-> res vals first))])]
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
          :on-click       (fn [e] (.stopPropagation e) (card-click-handler card))
          :on-touch-start  (fn [e] (.stopPropagation e) (card-click-handler card))
          ;:on-mouse-move  (fn [e] (.stopPropagation e) (swap! ra-app assoc :modal card))
          ;:on-mouse-out #(swap! ra-app dissoc :modal)
          }]]))
  ([ card ]
    (render-card card nil)))

(defn- render-cards 
  ([ cards size simple?]
    [:div.d-flex.justify-content-center ;;{:class (if (-> cards first :type (= "magicitem")) "flex-wrap")}
      (doall (for [ c cards ] (if simple? (render-card-simple c size) (render-card c size))))])
  ([ cards size ] (render-cards cards size false))
  ([ cards ] (render-cards cards nil false)))

(defn update-options! [ options ele ]
  (reset! options 
    [ (-> js/document (.getElementById "res1") .-value keyword) 
      (-> js/document (.getElementById "res2") .-value keyword)] ))

;; modal 

(defn- modal-place [ ]
  (let [options (r/atom [])]
    (fn []
      (let [card (-> @ra-app :modal :card) 
            label (if (= "artifact" (:type card)) "Place" "Claim")] 
        [:div.mb-3 {:style {:border-bottom "1px solid #222222"}}
          [:h4.text-center (str label " " (:name card))]
          [:div (-> card :cost str)]
          [:div.d-flex.mb-2
            [:button.btn.btn-primary.ms-auto {
                :on-click #((comms/ra-send! {:action :place :card card :resources (:cost card)}) (hidemodal))
              }
              label]]]))))

(defn- modal-discard [ ]
  (let [options (r/atom [:gold :na])] 
    (fn []
      (let [ card (-> @ra-app :modal :card)]
        (if (= "artifact" (:type card))
          [:div
            [:h4.text-center (str "Discard " (:name card))] 
            [:small.muted (str "Discard " (:name card) " to gain one Gold or two other resources.")]
            [:div.d-flex {:on-change #(update-options! options %)}
              [:select#res1.form-control.me-2 {:value (first @options)} 
                (for [r resource-list] [:option {:key (gensym) :value r } (-> r name clojure.string/capitalize)])]
              [:select#res2.form-control.me-2 {:value (last @options) :disabled (= :gold (first @options))} 
                (for [r resource-list] [:option {:key (gensym) :value (if (= :gold r) "na" r)} (if (= r :gold) "-" (-> r name clojure.string/capitalize))])]
              [:button.btn.ms-auto.btn-primary {
                  :disabled (and (not= :gold (first @options)) (= :na (last @options)))
                  :on-click #(let [?data (hash-map :action :discard :card card :resources (-> @options frequencies (dissoc :na)))] 
                                (comms/ra-send! ?data)
                                (hidemodal))
                } "Discard"]]])))))

(defn- modal [ pdata ] ; Content also driven via @ra-app
  (let [modal (:modal @ra-app)]
    [:div.modal.ra-main {:hidden (-> modal :show? nil?) :on-click #(hidemodal)}
      (if-let [card (:card modal)]
        [:div.modalcontent.p-2.rounded.bg-light.d-flex {:on-click #(.stopPropagation %)}
          [:div.h-100.text-center
            [:img.h-100 {:src (imgsrc card) }]]
          (if (:target? card)
            (if (-> pdata :action (= :play))
              [:div.ps-2
                [modal-place]
                [modal-discard]]
              [:div.ps-2
                [:div [:button.btn.btn-outline-secondary.mb-1.w-100 {:on-click #((select-btn-handler (:action pdata) card) (hidemodal))} "Select"]]
                [:div [:button.btn.btn-outline-secondary.mb-1.w-100 {:on-click #(hidemodal)} "Cancel"]]]))]
        
      (if-let [discard (:discard modal)]
        [:div.modalcontent.p-2.rounded.bg-light.d-flex.flex-wrap.w-100.justify-content-center
          (for [c discard] 
            [:div.m-1 {:key (gensym) :style {:width "20%" :height "auto"}}
              [:img.img-fluid { :src (imgsrc c)}]]
            )]))]))

(defn- pop-mon-mi-row [ selectmagicitem? ] 
  [:div.d-flex.justify-content-center
    [:div.p-1
      [:div.text-center.cardset "Places of Power"]       
      (render-cards (->> @gm :state :pops (map #(assoc % :target? true))) :lg) ;; DO THIS IN RAMODEL GAME LOGIC
      ;(-> @gm :state :pops (render-cards :lg))
      ]
    [:div.p-1
      [:div.text-center.cardset "Monuments"] 
      (render-cards (->> @gm :state :monuments :public (map #(assoc % :target? true))) :lg) ;; DO THIS IN RAMODEL GAME LOGIC
      ;(-> @gm :state :monuments :public (render-cards :lg))
      ]
    (when (not= :setup (-> @gm :state :status) )
      [:div.p-1
        [:div.text-center.cardset "Magic Items"]
        (render-cards (->> @gm 
                          :state 
                          :magicitems 
                          (remove :owner)
                          (map 
                            #(if (-> % :owner some?) 
                                 (assoc % :disabled? true) 
                                (if selectmagicitem? 
                                    (assoc % :target? true) 
                                    %)))) :md true)])])

(defn- opponentdisplay [ gs uname ]
  (let [magicitems (:magicitems gs)]
    [:div.d-flex
      (doall (for [k (->> gs :display-to (remove #(= % uname))) 
                    :let [ v       (get (:players gs) k) 
                           passed? (= :pass (:action v))
                           active? (and (not= :collect (:phase gs)) (contains? #{:selectmagicitem :play} (:action v)))
                           focus?  (= k (:focus @ra-app))]]
        [:div.border.border-dark.rounded.m-1 {
            :key (gensym) 
            :style {:flex-grow (cond (= uname k) 0 focus? 99 :else 1)} 
            :class (if active? "focus")
            :on-click #(if (= k (:focus @ra-app)) (swap! ra-app dissoc :focus) (swap! ra-app assoc :focus k))
          }
          [:div.d-flex.mx-1.p-1 ;.justify-content-between
            [:h5.me-3 (str k (if-let [mage (-> v :public :mage :name)] (str " - " mage)))]
            [:div [:div.d-flex.justify-content-center.resource-bar (for [ [r n] (-> v :public :resources)] (resource-icon r n) )]]
            ;(if (= :collect (:phase gs))
            ;    [:div [:i.fas {:class (if (:collected? v) "fa-check text-success" "fa-times text-danger")}]]
            ;    [:div])
            [:div]
            ]
          [:div.d-flex.mb-1.ms-1
            (if (-> v :public :mage map?) (-> v :public :mage (render-card-simple :sm)))
            (if-let [item (getmagicitem magicitems k)] (render-card-simple item :sm passed?))
            ]
         
        ]))
    ]))

(defn- playerdisplay [ gs uname pdata ]
  [:div.d-flex
    [:div.border.border-dark.rounded.m-1.px-2 {:style {:flex-grow 1 } :class (case (:action pdata) :pass "passed" :play "focus" "")}
      [:div.d-flex
        [:h3.me-3 (str uname (if-let [mage (-> pdata :public :mage :name)] (str " - " mage)))]
        (cond
          (= :collect (:phase gs))  (let [collected? (:collected? pdata)]
                                      [:div
                                        [:span.me-1 "Collect Resources"]
                                        [:button.btn.btn-outline-secondary.mt-1 {
                                            :class (if collected? "active") 
                                            :on-click #(comms/ra-send! {:action :collected})} 
                                          [:span.me-1 "Ready?"] ]
                                        ])
          (= :play (:action pdata))     [:div.my-auto "PLACE or DISCARD an artifact, CLAIM a Place of Power or Monument, USE a card, or PASS."]
          (= :selectmagicitem (:action pdata)) [:h3 "Select a new Magic Item"]
          :else [:div ]
        )]
      [:div.d-flex
        [:div.btn-group-vertical
          [:button.btn.btn-sm.btn-outline-secondary {:title "Pass" :on-click #(comms/ra-send! {:action :pass})} [:i.fas.fa-sign-out-alt.text-primary]]
          [:button.btn.btn-sm.btn-outline-secondary {:title "View Discard Pile" :on-click #(swap! ra-app assoc :modal {:show? true :discard (-> pdata :public :discard)})} [:i.fas.fa-trash-alt.text-danger]]
          ]
        (-> pdata :public :mage render-card)
        (-> gs :magicitems (getmagicitem uname) render-card)
      ]
      [:div.d-flex.justify-content-between {:style {:position "relative"}}
        [:div "Hand"]
        [:div.d-flex.justify-content-center (for [ [r n] (-> pdata :public :resources)] (resource-icon r n) )]
        [:div ]
      
        [:div.d-flex.hand 
          (doall (for [c (-> pdata :private :artifacts)] (render-card-simple (if (= :action (:phase gs)) (assoc c :target? true) c) :lg) ))]
    ]]
  ])


;;;;; STATES Setup / Play ;;;;;;

(defn- setup [ state uname pdata ]
  [:div.col-12.ra-main
    [opponentdisplay (->> state :players (remove #(-> % key (= uname))))]
    [:div.row
      [:div.col-2
        [:div.d-flex.justify-content-center
          [:img.img-fluid {:src (if-let [mage (-> pdata :public :mage)]
                                  (imgsrc mage)
                                  "/img/ra/mage-back.jpg")}]]]
      [:div.col-10
        (case (-> pdata :action)
          :selectmage 
            [:div.focus [:h3.text-center "Select Mage"] [render-cards (-> pdata :private :mages) :lg]]
          :selectstartitem
            [:div.focus [:h3.text-center "Select Starting Magic Item"] [render-cards (->> @gm :state :magicitems (remove #(-> % :owner some?)) (map #(assoc % :target? true))) :lg]]
          [:div.h4.text-center "Waiting for other players to make choices"])
        [:div.h4.text-center "Artifact Deck"]
        [render-cards (-> pdata :private :artifacts)]]
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
  (-> js/document .-body (.removeAttribute "style"))
  (-> js/document .-body .-style .-backgroundImage (set! "url(/img/ra/ra-bg.png)"))
  (-> js/document .-body .-style .-backgroundSize (set! "100%"))
  ;(-> ((js* "$") "#navbar") (.attr "hidden" true))
  (let [gs    (:state @gm)
        pdata (-> gs :players (get @uname))]
    [:div.container-fluid
      (modal pdata)
      [chat]
      [:div.d-flex.justify-content-between.ra-main.m-1
        [:h2.mt-auto.mb-0 "Res Arcana - " (if (= (:status gs) :setup) "Setup" (-> gs :phase name clojure.string/capitalize (str " phase")))]
        [:div.d-flex
          (for [p (:display-to gs) 
                  :let [plyr (-> gs :players (get p))  
                        ready?  (= :ready (:action plyr))
                        active? (contains? #{:selectstartitem :play} (:action plyr))
                        passed? (-> plyr :action (= :pass))
                        ]] 
            [:h5.text-center.plyr-status.mb-0.mx-1.p-1 {
                :key (gensym) 
                :class (cond ready? "ready" active? "active" passed? "pass" :else "")
              } 
              (-> plyr :public :mage :name) 
              [:small " - " p ]
              ])]
        [:div [:button.btn.btn-sm.btn-danger {:on-click #(if (js/confirm "Are you sure you want to Quit?") (comms/leavegame @gid))} [:i.fas.fa-times-circle]]]]
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