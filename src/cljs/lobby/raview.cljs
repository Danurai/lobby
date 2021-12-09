(ns lobby.raview
  (:require
    [reagent.core :as r]
    [lobby.model :as model :refer [ uname gid gm ]]
    [lobby.comms :as comms]))
    
;;;;; ATOMS ;;;;;

(def ra-app (let [scale (or (.getItem js/localStorage "cardscale") 4)]
  (r/atom {
    ;:modal {
    ;  :show? true 
    ;  :card {:id 7, :name "Transmutation", :type "magicitem", :action [{:turn true, :cost {:any 3}, :gain {:any 3, :exclude #{:gold}}}], :uid :mi53308, :owner "dan", :target? true, :can-use? true}}
    :settings {
      :cardsize {
        :scale scale
        :w (* scale 20)
        :h (* scale 28)}
    ;  :tips {:active true :tip "Do you have dragons, creatures, or ways to make gold? This may suggest Places of Power that will work well for you or if you can buy several monuments."}
    }})))

(def preview (r/atom nil))  ; de-couple from ra-app

(defonce essence-list [:gold :calm :life :elan :death])

;;;;; FUNCTIONS ;;;;;
(def essence-clr {
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
  ([ card ] (imgsrc card (and (:passed? card) (= "magicitem" (:type card))))))

(defn- essence-icon [ type n ]
  (let [color (case type (:elan :death) "white" "black")
        scale 1]
    (if (and (not= n 0) (some? n))
        [:div.text-center.mx-1.mt-1 {
            :key (gensym)
            :style {
              :position "relative"
              :background-image (str "url(img/ra/res-" (name type) ".png")
              :background-size "contain"
              :background-repeat "no-repeat"
              :background-position-y "0%"
              :color (if (< -2 n 2) "rgba(0,0,0,0)" color)
              :width     (str (* scale 1.1) "em")
            }
          }
          (if (< n 0) [:div {:style {:color (case type :elan "black" "darkred") :position "absolute" :left "-4px" :bottom "-6px" :font-size "1em"}} "x"])
          (Math.abs n)])))

(defn- select-btn-handler [ action card ]
  (comms/ra-send! {:action action :card (:uid card)}))

(defn- card-click-handler [ card ]
  (println "handler:" card)
  (swap! ra-app assoc :modal {:show? true :card card}))

(defn- select-card-keys [ c ]
  (select-keys c [:name :type :id]))

(defn- getmagicitem [ items uname ]
  (->> items  
        (filter #(-> % :owner (= uname))) 
        first))

(defn- player-public-cards [ gs uname ]
  (let [pdata (-> gs :players (get uname))
        mage  (-> pdata :public :mage) 
        mi (getmagicitem (:magicitems gs) uname) 
        artifacts (-> pdata :public :artifacts)]
    (apply conj [] mage mi artifacts)))

(defn- first-player-token-holder [ gs ]
  (if-let [passp1 (-> gs :pass-to first)]
    passp1
    (-> gs :plyr-to first)))

(defn- set-tags [ cards tag bool? ]
  (map 
    (fn [ c ]
      (if bool? (assoc c tag true) (dissoc c tag)))
    cards ))


;; Chat

(defn- parse-msg [ msg all-cards ]
  (if (empty? msg)
      [:span msg]
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
            [:span msg]))))

(defn- chat [ gs ]
  [:div.ra-chat.border.rounded
    [:div.chatbox.p-1.mb-1
      (for [msg (-> @gm :state :chat) :let [{:keys [msg uname timestamp event]} msg]]
        [:div.px-1 {:key (gensym) :style {:word-wrap "break-word"} :class (cond (nil? uname) "bg-secondary text-light" (= event :usercmd) "bg-warning" (= event :usercmdhelp) "bg-info")}
          ;[:span.mr-1 (model/timeformat timestamp)]
          [:b.text-primary.me-1 uname]
          [:span (parse-msg msg (:allcards gs))]])]
    [:form {:on-submit (fn [e] (.preventDefault e) (comms/sendmsg! (:msg @ra-app) @gid ) (swap! ra-app assoc :msg ""))}
      [:div.input-group
        [:input.form-control.form-control-sm.bg-light {
          :type "text" :placeholder "Type to chat"
          :value (:msg @ra-app)
          :on-change #(swap! ra-app assoc :msg (-> % .-target .-value))}]
        [:button.btn.btn-sm.btn-secondary {:type "btn"} [:i.fas.fa-arrow-right]]]]])

;; Render Cards

(defn- render-card
  ([ card size ]
    (let [scale (case size :lg 1.3 :sm 0.7  1)]
      [:div.d-flex.clickable {
          :key (gensym)
          :style {:position "relative"} 
          :on-touch-start  (fn [e] (.stopPropagation e) (card-click-handler card))
          :on-click        (fn [e] (.stopPropagation e) (card-click-handler card))
          :on-mouse-over   #(reset! preview (select-keys card [:id :type]))
        }
        [:div.d-flex.w-100.justify-content-around {
            :style {
              :position "absolute" 
              :top (str (-> @ra-app :settings :cardsize :h (* scale) (/ 3)) "px")
              :z-index "50"}}
            [:div.d-flex.rounded {:style {:background "rgba(255,255,255,0.6)"}}
              (for [res (-> card :collect-essence)]
                [:div.d-flex {:key (gensym)}
                  (for [[k v] (dissoc res :exclude)] (essence-icon k v))])]]
        [:img.img-fluid.card.mx-1.mb-1 {
          :title (str card)
          :width  (* (-> @ra-app :settings :cardsize :w) scale)
          ;:height (* (-> @ra-app :settings :cardsize :h) scale)
          :style {
            :display "inline-block"
            }
          :src (imgsrc card)
          :class (cond (:turned? card) "disabled" (-> card :collect-essence some?) "collect" (:target? card) "target" (:disabled? card) "disabled" :else nil)
          }]
        [:div {:style {:position :absolute :right "0"}}
          (for [[k v] (-> card :take-essence)] (essence-icon k v))
        ]]))
  ([ card ]
    (render-card card nil)))

(defn- render-cards 
  ([ cards size simple?]
    [:div.d-flex.justify-content-center ;;{:class (if (-> cards first :type (= "magicitem")) "flex-wrap")}
      (doall (for [ c cards ] (if simple? (render-card c size) (render-card c size))))])
  ([ cards size ] (render-cards cards size false))
  ([ cards ] (render-cards cards nil false)))

;; Modal 
;;; Functions
(defn update-options! [ options ele ]
  (let [opt1 (-> js/document (.getElementById "res1") .-value keyword)
        opt2 (-> js/document (.getElementById "res2") .-value keyword)]
    (reset! options 
      [  opt1 (if (= :gold opt1) :na opt2)] )))

(defn- can-pay-cost-hash [ cost res ]
  (println "can-pay-cost-hash" cost res)
  (->> (reduce-kv         ; 2. convert excess essence to :any
          (fn [m k v] 
            (if (< v 0)
                (update m :any + v)
                (assoc m k v))) 
          {:any (:any cost 0)}
          (reduce-kv      ; 1. subtract committed essence from requirement
            (fn [m k v] 
              (update m k - v))
            (dissoc cost :exclude) res)))) ; TODO - any selected exclusions...?

(defn- can-pay-cost? [ cost res ]
  (println "can-pay-cost?" cost res)
  (->>  (can-pay-cost-hash cost res)
        vals
        (filter #(not= % 0))
        count
        (= 0)))

(defn- default-payment [ cost res ]
  ;; returns the maximum payable of each essence
  (reduce-kv
    (fn [m k v]
      (let [minpayment (min v (k res))]
        (if (or (= k :any) (= 0 minpayment))
            (dissoc m k)
            (assoc m k minpayment)))) cost cost))

(defn- render-any-essence [ any ]
  [:div.d-flex {:key (gensym)}
    [:div (essence-icon :any (:any any))]
    (if-let [exclude (:exclude any)]
      [:small.d-flex.mt-auto.mb-1 "(" (for [excl-ess exclude] (essence-icon excl-ess -1)) ")"])])

(defn- render-essence-list 
  ([ essences class ]
    [:div.d-flex.justify-content-center {:class class} (for [[k v] (dissoc essences :exclude)] (if (= k :any) (render-any-essence essences) (essence-icon k v)))])
  ([ essences ] (render-essence-list essences "")))

;;; Modules
(defn- collect-essence-click-handler [ ra-app card action essence ]
  (swap! ra-app update-in [:modal :card] dissoc action)
  (comms/ra-send! {:action action :card card :essence essence})
  ;(if (-> (->> @ra-app :modal :card (remove nil?)) (select-keys [:collect-essence :take-essence]) empty?) (hidemodal))
  (hidemodal)
  )

; gain or pay? - parameterise
(defn- gain-any-essence-module [ card gain-any collect-gain-any ]
  [:div
    (render-essence-list gain-any)
    [:h5 "Select essences to gain"]
    [:div.d-flex.mb-3
      [:div.btn-group.mx-auto 
        (for [ess (remove #(contains? (:exclude gain-any) %) essence-list)]
          [:button.btn.btn-outline-secondary {:key (gensym) :on-click #(swap! ra-app update-in [:modal :card :collect-gain-any ess] inc)}
            (essence-icon ess 1)]) 
        [:button.btn.btn-outline-secondary.text-dark {:title "Reset" :on-click #(swap! ra-app update-in [:modal :card] dissoc :collect-gain-any)} "X"]]]])


(defn- modal-collect-essence [ {:keys [collect-essence take-essence] :as card} uname ]
  [:div.ps-1.pt-3
    ;[:div (str card)]
    (if collect-essence
      [:div.mb-3 {:style {:min-width "250px"}}
        [:h4 "Collect Essence"]
        (if (-> collect-essence first :any)                                                                           ; Assume a collect-essence {:any n} will only have one option
          (let [gain-any (first collect-essence) collect-gain-any (:collect-gain-any card)]                           ; ANY Essence
            [:div 
              [gain-any-essence-module card gain-any collect-gain-any]
              [:button.btn.btn-outline-secondary.w-100.text-dark {
                  :on-click #(collect-essence-click-handler ra-app card :collect-essence collect-gain-any) 
                  :disabled (not (can-pay-cost? (dissoc gain-any :exclude) collect-gain-any)) 
                }
                [:div.h4.d-flex.justify-content-around 
                  [:div.my-auto "Collect:"]
                  (if collect-gain-any
                      (render-essence-list collect-gain-any "h4")
                      [:div "?"])]]])
          [:div.btn-group-vertical.w-100
            (for [essences collect-essence]                                                                           ; Link to actual game state instead of ra-app? TODO
                [:button.btn.btn-outline-secondary {:key (gensym) :on-click #(collect-essence-click-handler ra-app card :collect-essence essences)} 
                  (render-essence-list (dissoc essences :any :exclude) "h4")])])])
    (if take-essence 
      [:div {:style {:min-width "250px"}}
        [:h4 "Take placed essence"]
        [:div.d-flex.justify-content-between ]
        [:button.btn.btn-outline-secondary.w-100 {:on-click #(collect-essence-click-handler ra-app card :take-essence nil)}
          (render-essence-list take-essence "h4")]])])

(defn- render-action-bar [ a ]
  [:div.d-flex.justify-content-center.mb-2.bg-essence
    (if (:turn a) [:i.fas.fa-directions.fa-lg.my-auto.me-1])                      ; Turn
                                                                                  ; Extra card turn?
    (render-essence-list (reduce-kv #(assoc %1 %2 (- 0 %3)) {} (:cost a)))        ; Invert costs and display
                                                                                  ; Destroy
    [:i.fas.fa-caret-right.fa-lg.my-auto {:style {:color "darkblue"}}]            ; >
    (render-essence-list (:gain a) )                                              ; Gain
    (if-let [rivals (:rivals a)]                                                  ; Rivals Gain
      [:div.d-flex [:div.me-1 "+ all rivals gain"] (render-essence-list rivals)])
    (if-let [place (:place a)]                                                    ; Place essence
      [:div.d-flex (render-essence-list place) [:i.fas.fa-caret-square-down.fa-lg.my-auto]])
    (if-let [draw (:draw a)] [:div (str "draw " draw " card" )])                  ; Draw
    (if-let [straighten (:straighten a) ] [:i.fas.fa-arrow-up.fa-lg.my-auto.me-1 ]) ; Straighten TODO TYPE
  ])


;(def useraction (r/atom nil)) ;; Parameters for a card action (action) :cost :gain :turn
; 1. Store - cost - gain ^^^^^^^^^^^^^^^^^^^^^
; 2. Use modules to display :any - cost - gain
; 3. Show cards to straighten
; 4. Show user(s) to target
(defn- reset-essences [ tua k ] [:div.d-flex.px-2.essence.clickable {:title "Reset" :on-click #(swap! tua dissoc k)} [:h4.m-auto "X"]])

(defn- pay-module [ pdata thisuseraction ]
  [:div.py-2.modal-module
    [:div.h5.text-center "Select essence to spend"]; [:small "(remaining shown)"]]
    ;[:div.debug (str @thisuseraction)]
    [:div.d-flex.justify-content-center 
      (doall (for [[k v] (-> pdata :public :essence) :let [adj_v (- v (-> @thisuseraction :cost (k 0)))]] 
        (if (> adj_v 0) 
          [:div.essence.clickable.px-1 {:key (gensym) :on-click #(swap! thisuseraction update-in [:cost k] inc)}
            (essence-icon k adj_v)])))
      (reset-essences thisuseraction :cost)]])
  
(defn- gain-module [ cardaction thisuseraction k ]
  [:div.py-2.modal-module
    [:div.h5.text-center (str "Select essence to " (name k))]
    ;[:div.debug (str @thisuseraction)]
    [:div.d-flex.justify-content-center 
      (doall (for [ess (remove #(contains? (-> cardaction k :exclude) %) essence-list)]
        [:div.essence.clickable.px-1 {:key (gensym) :on-click #(swap! thisuseraction update-in [k ess] inc)}
          (essence-icon ess 1)]))
      (reset-essences thisuseraction k)]])

(defn- render-remaining-essence [ pdata cost ]
  [:div.d-flex.justify-content-center.mt-2
    [:div.me-2.mt-auto "Remaining Essence:"]
      [:small.d-flex.justify-content-center (for [[k v] (-> pdata :public :essence)] (essence-icon k (- v (k cost 0)))  )]])
      
(defn- modal-use-action-ele [ card a pdata ]
  (let [thisuseraction (r/atom (assoc a 
                                  :cost (default-payment (:cost a) (-> pdata :public :essence)) 
                                  :gain (-> a :gain (dissoc :any :exclude))
                                  :place (-> a :place (dissoc :any :exclude)) ))] ; should be payable amount. Use @ra-app? or universal atom
    (fn []
      (let [{:keys [cost gain place target]} @thisuseraction 
            canpay (can-pay-cost? (:cost a) (:cost @thisuseraction)) 
            cangain (can-pay-cost? (:gain a) (:gain @thisuseraction))
            canplace (can-pay-cost? (:place a) (:place @thisuseraction))]
        [:div.mb-2.modal-action 
          ;[:div.debug (str @thisuseraction)]
          (render-action-bar a)
          (if (-> a :cost :any)
              (pay-module pdata thisuseraction))
          (if (-> a :gain :any)
              (gain-module a thisuseraction :gain))
          (if (-> a :place :any)
              (gain-module a thisuseraction :place))
          [:div.d-flex.justify-content-around.mb-1.py-2
            (if (:cost a)  [:div.d-flex [:div "Pay:"]   (render-essence-list (if (empty? cost)  (:cost a)  cost))])
            (if (:gain a)  [:div.d-flex [:div "Gain:"]  (render-essence-list (if (empty? gain)  (:gain a)  gain))])
            (if (:place a) [:div.d-flex [:div "Place:"] (render-essence-list (if (empty? place) (:place a) place))])
            (if (:target a) [:div.d-flex [:div "Target"] ])
          ]
          ;[:div.debug (str "canpay " canpay " cangain " cangain " canplace " canplace)]
          [:div.d-flex.mb-1
            [:button.btn.btn-primary.ms-auto {
                :disabled (not (and canpay cangain canplace))
                :on-click #((comms/ra-send! {:action :usecard :useraction @thisuseraction :card card}) (hidemodal))
              } "Use Action"]]
        ]))))

(defn- modal-use [ card uname ]
  (if (and (-> card :turned? nil?) (->> (:action card) (remove :react) empty? not)) ;; GUARD DOG CAN BE STRAIGHTENED
    [:div.px-1
      ;[:div.debug (str card)]
      [:h3.text-center.pt-3 (str "Use " (:name card))]
      (for [action (remove :react (:action card))] ^{:key (gensym)}[modal-use-action-ele card action uname])]))

(defn- useraction-click-handler [ action card useraction ]
  (comms/ra-send! {:action action :card card :useraction useraction})
  (hidemodal))

(defn- modal-place-card-ele [ card pdata ]
  (let [thisuseraction (r/atom {:cost (default-payment (:cost card) (-> pdata :public :essence))})
        essence (r/atom (default-payment (:cost card) (-> pdata :public :essence)))]
    (fn []
      (let [card (-> @ra-app :modal :card) 
            label (if (= "artifact" (:type card)) "Place" "Claim")] 
        [:div.pt-3.pb-2.mb-3.ps-2 {:style {:border-bottom "1px solid #222222"}}
          [:h3 (str label " " (:name card))]
          ;[:div.debug (str card)]
          ;[:div.debug (str (can-pay-cost-hash (:cost card) {:cost @thisuseraction}))]
          ;[:div.debug (str @thisuseraction)]
          [:div.bg-essence.mb-2 (render-essence-list (:cost card) "h5")]
          [:div.d-flex.my-2
            [:div.h4.me-3 "Pay:"]
            (render-essence-list (:cost @thisuseraction) )
            [:button.btn.btn-primary.ms-auto {
              :on-click #((comms/ra-send! {:action :place :card card :essence (-> @thisuseraction :cost (dissoc :any))}) (hidemodal))
              :disabled (not (can-pay-cost? (:cost card) (:cost @thisuseraction))) } label]]
          (if (-> card :cost :any) 
              (pay-module pdata thisuseraction)
              (render-remaining-essence pdata (:cost @thisuseraction)))
        ]))))

(defn- modal-discard-card-ele [ ] ;Revisit this atom use?
  (let [options (r/atom [:gold :na])] 
    (fn []
      (let [ card (-> @ra-app :modal :card)]
        (if (= "artifact" (:type card))
          [:div
            [:h4.text-center (str "Discard " (:name card))] 
            [:small.muted (str "Discard " (:name card) " to gain one Gold or two other essence.")]
            [:div.d-flex {:on-change #(update-options! options %)}
              [:select#res1.form-control.me-2 {:value (first @options) :on-click #()} 
                (for [r essence-list] [:option {:key (gensym) :value r :on-click #()} (-> r name clojure.string/capitalize)])]
              [:select#res2.form-control.me-2 {:value (last @options) :on-click #() :disabled (= :gold (first @options))} 
                (for [r essence-list] [:option {:key (gensym) :value (if (= :gold r) "na" r) :on-click #()} (if (= r :gold) "-" (-> r name clojure.string/capitalize))])]
              [:button.btn.ms-auto.btn-primary {
                  :disabled (and (not= :gold (first @options)) (= :na (last @options)))
                  :on-click #(let [?data (hash-map :action :discard :card card :essence (-> @options frequencies (dissoc :na)))] 
                                (comms/ra-send! ?data)
                                (hidemodal))
                } "Discard"]]])))))

(defn- modal-target [ card pdata ]
  (if (-> pdata :action (= :play))
    [:div.ps-1 {:style {:min-width "200px"}}
      [modal-place-card-ele card pdata]
      [modal-discard-card-ele ]]
    [:div.ps-1 {:style {:min-width "200px"}}
      [:h5.text-center (str "Select " (:name card))]
      [:div [:button.btn.btn-outline-secondary.mb-1.w-100 {:on-click #((select-btn-handler (:action pdata) card) (hidemodal))} "Select"]]
      [:div [:button.btn.btn-outline-secondary.mb-1.w-100 {:on-click #(hidemodal)} "Cancel"]]]))


(defn- modal-info [ pg ]
  [:div.modalcontent.p-2.rounded.bg-light {:on-click #(.stopPropagation %)}
    [:button.btn-sm.btn-close.ms-auto.bg-light {:on-click #(hidemodal) :style {:position "absolute" :right "5px" :top "5px" :z-index 99}}]
    [:div#infocarousel.carousel.slide.h-100 {:data-ride "carousel"}
      [:div.carousel-inner.h-100
        [:div.carousel-item.h-100 {:class (if (= pg 0) "active") :key (gensym "carousel")} [:img.d-block.h-100 {:src (str "/img/ra/ref-0.jpg")}]]
        [:div.carousel-item.h-100 {:class (if (= pg 1) "active") :key (gensym "carousel")} [:img.d-block.h-100 {:src (str "/img/ra/ref-1.jpg")}]]
        [:div.carousel-item.h-100 {:class (if (= pg 2) "active") :key (gensym "carousel")} [:img.d-block.h-100 {:src (str "/img/ra/ref-2.jpg")}]]
        [:div.carousel-item.h-100 {:class (if (= pg 3) "active") :key (gensym "carousel")} [:img.d-block.h-100 {:src (str "/img/ra/ref-3.jpg")}]]
        ]
      [:button.carousel-control-prev {:type "button" :on-click (fn [] (swap! ra-app update-in [:modal :info] #(max (dec %) 0)))} [:span.carousel-control-prev-icon]]    
      [:button.carousel-control-next {:type "button" :on-click (fn [] (swap! ra-app update-in [:modal :info] #(min (inc %) 3)))} [:span.carousel-control-next-icon]]    
        
    ]])

;;; Main
(defn- modal [ gs pdata uname ] ; Content also driven via @ra-app
  (let [{:keys [show? card discard info]} (:modal @ra-app)]
    [:div.modal.ra-main {:hidden (-> show? nil?) :on-click #(hidemodal)}
      (cond 
        info    [modal-info info]
        card    [:div.modalcontent.p-2.rounded.d-flex {:on-click #(.stopPropagation %)}
                  [:div.h-100.text-center
                    [:img.card.h-100 {:src (imgsrc card) :class (cond (:turned? card) "disabled")}]]
                  [:div
                    [:button.btn-sm.btn-close.ms-auto.bg-light {:on-click #(hidemodal) :style {:position "absolute" :right "5px" :top "5px"}}]
                  (cond   ;; options based on card and game state
                    (= :collect (:phase gs))          (modal-collect-essence card uname)
                    (:can-use? card)                  (modal-use card pdata)
                    (:target? card)                   (modal-target card pdata))]]
        
        discard [:div.modalcontent.p-2.rounded.d-flex.flex-wrap.w-100.justify-content-center
                  (for [c discard] 
                    [:div.m-1 {:key (gensym) :style {:width "20%" :height "auto"}}
                      [:img.img-fluid { :src (imgsrc c)}]]
                    )])]))

;; Main View
;;; Modules
(defn- pop-mon-mi-row [ status phase action ] 
  (let [target? (= [:play :play] [status action])]
  ; Toogle :target? based on (:phase gs) (:action pdata)
    [:div.d-flex.justify-content-center
      [:div.p-1
        [:div.text-center.cardset "Places of Power"]
        (-> @gm :state :pops (set-tags :target? target?) render-cards)]
      [:div.p-1
        [:div.text-center.cardset "Monuments"] 
        (-> @gm :state :monuments :public (set-tags :target? target?) render-cards)]
      (when (not= :setup (-> @gm :state :status) )
        [:div.p-1 {:class (if (= :selectmagicitem action) "focus")}
          [:div.text-center.cardset "Magic Items"]
          (render-cards (->> @gm 
                            :state 
                            :magicitems 
                            (remove :owner)
                            (map 
                              #(if (-> % :owner some?) 
                                  (assoc % :disabled? true) 
                                  (if (= action :selectmagicitem) 
                                      (assoc % :target? true) 
                                      %)))) :md true)])]))

(defn- p1token [ gs pname action ]
  (if (= pname (first-player-token-holder gs)) [:img.p1token {:src (str "/img/ra/player1" (if (= :pass action) "passed") ".png")}]))

(defn- opponentdisplay [ gs uname ]
  (let [magicitems (:magicitems gs)]
    [:div.d-flex
      (doall (for [k (->> gs :display-to (remove #(= % uname))) 
                    :let [ v       (get (:players gs) k) 
                           passed? (= :pass (:action v))
                           active? (and (not= :collect (:phase gs)) (contains? #{:selectmagicitem :play} (:action v)))
                           focus?  (= k (:focus @ra-app))]]
        [:div.border.border-dark.rounded.m-1.playerdisplay {
            :key (gensym) 
            :style {:flex-grow (cond (= uname k) 0 focus? 99 :else 1)} 
            :class (cond active? "focus" passed? "pass")
            :on-click #(if (= k (:focus @ra-app)) (swap! ra-app dissoc :focus) (swap! ra-app assoc :focus k))
          }
          [:div.d-flex.mx-1.p-1 ;.justify-content-between
            [:div.d-flex
              [:h5.vp.me-2 {:title (clojure.string/join "," (map #(str (key %) ": " (val %)) (:vp v)))} (->> v :vp vals (apply +)) ]
              [:h4.me-3 (str k (if-let [mage (-> v :public :mage :name)] (str " - " mage)))]]
            [:div [:div.d-flex.justify-content-center.essence-bar (for [ [r n] (-> v :public :essence)] (essence-icon r n) )]]
            [:div]
            ]
          [:div.d-flex.mb-1.ms-1
            (-> gs (player-public-cards k) (set-tags :collect-essence nil) (set-tags :passed? passed?) (render-cards :sm)) 
            (p1token gs k (:action v))
            ]
         
        ]))
    ]))

(defn- playerdisplay [ gs uname pdata ]
  (let [play? (= :play (:action pdata)) passed? (= :pass (:action pdata))]
    [:div.d-flex
      [:div.border.border-dark.rounded.m-1.px-2.playerdisplay {:style {:flex-grow 1} :class (case (:action pdata) :pass "pass" :play "focus" "")}
        [:div.d-flex.p-1
          [:div.d-flex
            [:h4.vp.me-2 {:title (clojure.string/join "," (map #(str (key %) ": " (val %)) (:vp pdata)))} (->> pdata :vp vals (apply +)) ]
            [:h3.me-3 (str uname (if-let [mage (-> pdata :public :mage :name)] (str " - " mage)))]]
          (cond
            (= :collect (:phase gs))  
                (let [collected? (:collected? pdata) collect-remaining (filter :collect-essence (-> gs (player-public-cards uname)))] ; include take-essence also
                  [:div
                    [:span.me-1 "Collect essence"]
                    [:button.btn.mt-1 {
                        :class   (if collected? "btn-success active" (if (empty? collect-remaining) "btn-primary" "btn-warning")) 
                        :on-click #(comms/ra-send! {:action :collected})} 
                      [:span.me-1 "Ready?"] ]
                    ])
            play?
                [:div.my-auto "PLACE or DISCARD an artifact, CLAIM a Place of Power or Monument, USE a card, or PASS."]
            (= :selectmagicitem (:action pdata)) 
                [:h3 "Pass: Select a new Magic Item."]
            :else
                [:div ]
          )]
        [:div.d-flex
          [:div.btn-group-vertical
            [:button.btn.btn-sm.btn-outline-secondary {:title "Pass" :on-click #(comms/ra-send! {:action :pass})} [:i.fas.fa-sign-out-alt.text-primary]]
            [:button.btn.btn-sm.btn-outline-secondary {:title "View Discard Pile" :on-click #(swap! ra-app assoc :modal {:show? true :discard (-> pdata :public :discard)})} [:i.fas.fa-trash-alt.text-danger]]]
          (-> gs (player-public-cards uname) (set-tags :passed? passed?) (set-tags :target? play?) (set-tags :can-use? play?) render-cards)
          (p1token gs uname (:action pdata))
          ;[:small (str pdata)]
        ]
        [:div.d-flex.justify-content-between {:style {:position "relative"}}
          [:div "Hand"]
          [:div.d-flex.justify-content-center.essence-bar (doall (for [ [r n] (-> pdata :public :essence)] (essence-icon r n) ))]
          [:div ]
          [:div.d-flex.hand (-> pdata :private :artifacts (set-tags :target? play?) render-cards)]
          ]
        ;[:div (-> pdata :public :artifacts str)]; (.stringify js/JSON (clj->js pdata))]
      ]
    ]))

;;; STATES Setup / Play ;;;;;;

(defn- setup [ state uname pdata ]
  [:div.col-12.ra-main
    [opponentdisplay (->> state :players (remove #(-> % key (= uname))))]
    [:div.row
      [:div.col-2
        [:div.d-flex.justify-content-center
          [:img.img-fluid {:src (if-let [mage (-> pdata :public :mage)]
                                        (imgsrc mage)
                                        "/img/ra/mage-back.jpg")}]]]
      [:div.col-8
        (case (-> pdata :action)
          :selectmage 
            [:div.focus [:h3.text-center "Select Mage"] [render-cards (-> pdata :private :mages) :lg]]
          :selectstartitem
            [:div.focus [:h3.text-center "Select Starting Magic Item"] [render-cards (->> @gm :state :magicitems (remove #(-> % :owner some?)) (map #(assoc % :target? true))) :lg]]
          [:div.h4.text-center "Waiting for other players to make choices"])
        [:div.h4.text-center "Artifact Deck"]
        [render-cards (-> pdata :private :artifacts)]]
      [:div.col-2
        (if @preview
          [:img.img-fluid {:src (str "/img/ra/" (:type @preview) "-" (:id @preview) ".jpg")}])]
    ]
    (pop-mon-mi-row :setup nil :waiting)])

(defn- gamestate [ gs uname pdata ]
  [:div.row.ra-main
    (opponentdisplay gs uname)
    (pop-mon-mi-row (:status gs) (:phase gs) (:action pdata))
    (playerdisplay gs uname pdata)
  ])

;(defn- icon-drop-down [v]
;  [:div.bg-light.border.px-2.w-100 {:style {:position "absolute" :top "1.5em" :left "0" :display (if (:hidden? @v) "none" "unset")}}
;    (for [r essence-list] [:div.my-2.text-center {:on-click (fn [e] (.stopPropagation e) (reset! v {:icon r :hidden? true})) :key (gensym) :class (str "icon-" (name r)) }] )])
;(defn- icon-select []
;  (let [v (r/atom {:icon :gold :hidden? true})]
;    (fn []
;      [:div [:div.border.bg-light.px-2.clickable {:style {:position "relative" :width "2em"} :on-click #(swap! v assoc :hidden? (-> @v :hidden? false?))}
;        [:div.text-center [:span {:class (str "icon-" (-> @v :icon name))}]]
;        [icon-drop-down v]
;        ]])))

(defn- page-banner [ gs ] 
  [:div.d-flex.justify-content-between.ra-main.m-1
    [:h2.mt-auto.mb-0 "Res Arcana - " (if (= (:status gs) :setup) "Setup" (-> gs :phase name clojure.string/capitalize (str " phase")))]
    ;[:div#debug (-> gs :chat str)]
    ;[icon-select]
    [:div.d-flex
      (for [p (:display-to gs) 
              :let [plyr (-> gs :players (get p))  
                    ready?  (or (:collected? plyr) (= :ready (:action plyr)))
                    active? (and (= :action (:phase gs)) (contains? #{:selectstartitem :play} (:action plyr)))
                    passed? (-> plyr :action (= :pass))
                    ]] 
        [:h5.text-center.plyr-status.mb-0.mx-1.p-1 {
            :key (gensym) 
            :class (cond ready? "ready" active? "active" passed? "pass" :else "")
          } 
          (-> plyr :public :mage :name) 
          [:small.ms-1 (str "(" p ")") ]
          ])]
    [:div
      [:div.btn-group.btn-group-sm.me-2
        [:button.btn.btn-secondary {:on-click #(comms/ra-send! {:action :swapgame :game 1})} "G1"]
        [:button.btn.btn-secondary {:on-click #(comms/ra-send! {:action :swapgame :game 2})} "G2"]]
      [:button.btn.btn-sm.btn-primary.ms-auto.me-2 {:on-click #(swap! ra-app assoc :modal {:show? true :info (if (= :setup (:status gs)) 0 1)})} [:i.fas.fa-info-circle]]
      [:button.btn.btn-sm.btn-danger {:on-click #(if (js/confirm "Are you sure you want to Quit?") (comms/leavegame @gid))} [:i.fas.fa-times-circle]]]])

;;; MAIN ;;;
(defn ramain [ ]
  (-> js/document .-body (.removeAttribute "style"))
  (-> js/document .-body .-style .-backgroundImage (set! "url(/img/ra/ra-bg.png)"))
  (-> js/document .-body .-style .-backgroundSize (set! "100%"))
  ;(-> ((js* "$") "#navbar") (.attr "hidden" true))
  (let [gs    (:state @gm)
        pdata (-> gs :players (get @uname))]
    [:div.container-fluid
      (modal gs pdata @uname)
      [chat gs]
      [page-banner gs]
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
;;;      :essence {
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