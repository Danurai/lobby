(ns lobby.raview
  (:require
    [reagent.core :as r]
    [lobby.model :as model :refer [ uname gid gm ]]
    [lobby.comms :as comms]
    [lobby.raessence :refer [action-bar essence-svg invert-essences essence-list]]
  ))
    
;;;;; ATOMS ;;;;;

(def ra-app (let [scale (or (.getItem js/localStorage "cardscale") 4)]
  (r/atom {
    ;:modal {
    ;  :show? true
    ;  :card {:id 6, :name "Obelisk", :type "monument", :cost {:gold 4}, :vp 1, :action [{:bought true, :gain {:any 6, :exclude #{:gold}}}], :uid "mon33365", :target? true}
    ;}
    :settings {
      :cardsize {
        :scale scale
        :w (* scale 20)
        :h (* scale 28)}
    ;  :tips {:active true :tip "Do you have dragons, creatures, or ways to make gold? This may suggest Places of Power that will work well for you or if you can buy several monuments."}
    }})))

(def preview (r/atom nil))  ; de-couple from ra-app

;;;;; FUNCTIONS ;;;;;
(defn- hidemodal []
  (swap! ra-app dissoc :modal))

(defn- imgsrc 
  ([ card back? ]
    (str "/img/ra/" (:type card) "-" (if back? "back" (:id card)) ".jpg"))
  ([ card ] (imgsrc card (and (:passed? card) (= "magicitem" (:type card))))))

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

(defn- pdata-public-cards [ pdata ]
  (apply conj [(-> pdata :public :mage)] (-> pdata :public :magicitem) (-> pdata :public :artifacts)))

(defn- filter-components [ components restriction ] ; restriction ~ {<field name> <string value contained in field>}
  (if (nil? restriction)
      components
      (let [[k v] (first restriction)]
        (println (re-pattern v))
        (filter #(re-find (re-pattern v) (k % "")) components))))
      
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
      (for [msg (-> @gm :state :chat reverse) :let [{:keys [msg uname timestamp event]} msg]]
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
(defn- card-title [ card ]
  (str 
    (case (:type card)
      "pop"       "Place of Power"
      "magicitem" "Magic Item"
      (clojure.string/capitalize (:type card)))
    ": "
    (:name card)))

(defn- render-card
  ([ card size ]
    (let [scale (case size :lg 1.3 :sm 0.7  1)]
      [:div.d-flex.clickable {
          :key (gensym)
          :style {:position "relative"} 
          ;:title (card-title card)
          :title (str card)
          :on-touch-start  (fn [e] (.stopPropagation e) (card-click-handler card))
          :on-click        (fn [e] (.stopPropagation e) (card-click-handler card))
          :on-mouse-over   #(reset! preview (select-keys card [:id :type]))
        }
        [:div.d-flex.w-100.justify-content-around {
            :style {
              :position "absolute" 
              :top (str (-> @ra-app :settings :cardsize :h (* scale) (/ 3)) "px")
              :z-index "50"}}
            [:div.d-flex ;.rounded ;{:style {:background "rgba(255,255,255,0.6)"}}
              (for [res (-> card :collect-essence)]
                (if (= :turn (-> res first key))
                  [:div.d-flex {:key (gensym)} [:i.fas.fa-directions.fa-lg.my-auto.me-1]]
                  [:div.d-flex {:key (gensym)}
                    (for [[k v] (dissoc res :exclude)] (essence-svg k v {:size :sm}))]))]]
        [:img.card.mx-1.mb-1 {
          :width  (* (-> @ra-app :settings :cardsize :w) scale)
          ;:height (* (-> @ra-app :settings :cardsize :h) scale)
          :style {
            :display "inline-block"
            }
          :src (imgsrc card)
          :class (cond (:turned? card) "disabled" (-> card :collect-essence some?) "collect" (:target? card) "target" (:disabled? card) "disabled" :else nil)
          }]
        [:div {:style {:position :absolute :right "3px" :top "3px"}}
          (for [[k v] (-> card :take-essence)] [:div {:key (gensym)} (essence-svg k v {:size :sm})])
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
  ;(println "can-pay-cost-hash" cost res)
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

(defn- can-pay-cost? 
  ([ cost res reducers ]
    (let [reduced-cost (reduce #(let [[k v] %2] (if (k %1) (update %1 k - v) %1)) cost (apply concat (vals reducers)))]
      ;(println "can-pay-cost?" cost res)
      (->>  (can-pay-cost-hash reduced-cost res )
            vals
            (filter #(not= % 0))
            count
            (= 0))))
  ([cost res] (can-pay-cost? cost res nil)))

(defn- default-payment [ cost res ]
  ;; returns the maximum payable of each essence
  (reduce-kv
    (fn [m k v]
      (let [minpayment (min v (k res))]
        (if (or (= k :any) (= 0 minpayment))
            (dissoc m k)
            (assoc m k minpayment)))) cost cost))

;(defn- render-any-essence [ any ]
;  [:div.d-flex {:key (gensym)}
;    [:div (essence-svg :any (:any any))]
;    (if-let [exclude (:exclude any)]
;      [:small.d-flex.mt-auto.mb-1 "(" (for [excl-ess exclude] (essence-svg excl-ess -1)) ")"])])

(defn- render-essence-list 
  ([ essences class ]
    [:div.d-flex.justify-content-center {:class class} (for [[k v] (dissoc essences :exclude)] (essence-svg k v))])
  ([ essences ] (render-essence-list essences "")))

;;; Modules
(defn- collect-essence-click-handler [ ra-app card action essence ]
  ;(swap! ra-app update-in [:modal :card] dissoc action)
  (comms/ra-send! {:action action :card card :essence essence})
  (hidemodal))

;(def useraction (r/atom nil)) ;; Parameters for a card action (action) :cost :gain :turn
; 1. Store - cost - gain ^^^^^^^^^^^^^^^^^^^^^
; 2. Use modules to display :any - cost - gain
; 3. Show cards to straighten
; 4. Show user(s) to target

; gain or pay? - parameterise
(defn- gain-any-essence-module [ card gain-any collect-gain-any ]
  [:div
    (render-essence-list gain-any)
    [:h5 "Select essences to gain"]
    [:div.d-flex.mb-3
      [:div.btn-group.mx-auto 
        (for [ess (remove #(contains? (:exclude gain-any) %) essence-list)]
          [:button.btn.btn-outline-secondary {:key (gensym) :on-click #(swap! ra-app update-in [:modal :card :collect-gain-any ess] inc)}
            (essence-svg ess 1)]) 
        [:button.btn.btn-outline-secondary.text-dark {:title "Reset" :on-click #(swap! ra-app update-in [:modal :card] dissoc :collect-gain-any)} "X"]]]])

(defn- reset-essences [ tua k ] [:div.d-flex.px-2.essence.clickable {:title "Reset" :on-click #(swap! tua dissoc k)} [:h4.m-auto "X"]])

(defn- pay-module [ pdata {:keys [cost]} thisuseraction ]
  [:div.p-2.modal-module
    [:div.h5.text-center "Select essence to spend"]; [:small "(remaining shown)"]]
    [:div.d-flex.justify-content-center 
      (doall (for [[k v] (reduce (fn [m k] (dissoc m k)) (-> pdata :public :essence) (-> cost :exclude vec)) :let [adj_v (- v (-> @thisuseraction :cost (k 0)))]] 
        (if (> adj_v 0) 
          [:div.essence.clickable.px-1 {:key (gensym) :on-click #(swap! thisuseraction update-in [:cost k] inc)}
            (essence-svg k adj_v)])))
      (reset-essences thisuseraction :cost)]])

(defn- can-collect [ e pe ]
  (->> e
      (reduce-kv
        (fn [m k v]
          (update m k + v)) pe)
      vals
      (filter #(> 0 %))
      count (= 0)))

(defn- modal-collect-essence [ {:keys [collect-essence take-essence] :as card} pdata uname ]
  [:div.ps-1.pt-3
    ;[:div.debug (str card)]
    (if collect-essence
      [:div.mb-3 {:style {:min-width "250px"}}
        [:h4 "Collect Essence"]
        (if (:collect-mandatory card) [:div "You must select one."])
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
              (if (= :turn (-> essences first key))
                [:button.btn.btn-outline-secondary {
                    :key (gensym)
                    :on-click #(collect-essence-click-handler ra-app card :collect-essence essences)}
                  [:i.fas.fa-directions.fa-2x.text-dark]]
                [:button.btn.btn-outline-secondary {
                    :key (gensym) 
                    :disabled (not (can-collect essences (-> pdata :public :essence)))
                    :on-click #(collect-essence-click-handler ra-app card :collect-essence essences)} 
                  (render-essence-list (dissoc essences :any :exclude) "h4")]))])])
    (if take-essence 
      [:div {:style {:min-width "250px"}}
        [:h4 "Take placed essence"]
        [:div.d-flex.justify-content-between ]
        [:button.btn.btn-outline-secondary.w-100 {:on-click #(collect-essence-click-handler ra-app card :take-essence nil)}
          (render-essence-list take-essence "h4")]])])

(defn- render-action-bar [ a ]
  [:div.d-flex.justify-content-center.mb-2.bg-essence.py-2
    (if (:turn a) [:i.fas.fa-directions.fa-lg.my-auto.me-1])                      ; Turn
                                                                                  ; Extra card turn?
    (if (:cost a)
      (if-let [exclude (-> a :cost :exclude)]
        [:div.d-flex 
          [:div.wrap-label.mx-1 "+"]
          [:div.d-flex 
            (for [ess (->> essence-list (remove #(contains? exclude %)) (interpose "/"))] 
              [:div.essence-or {:key (gensym)} (if (= ess "/") [:h4.mx-1 "/"] (essence-svg ess -1))])]]  ; Display options for any list with exclusion 
        [:div.d-flex
          [:div.wrap-label.mx-1 "+"]
          (render-essence-list (reduce-kv #(assoc %1 %2 (- 0 %3)) {} (:cost a)))]))  ; Invert costs and display
                                                                                  ; Destroy
    [:i.fas.fa-caret-right.fa-lg.my-auto {:style {:color "darkblue"}}]            ; >
    (render-essence-list (:gain a) )                                              ; Gain
    (if-let [gr (:gainrival a)] 
      [:div.d-flex
        [:div "gain"] (essence-svg (:gain gr) "?") [:div "equal to"] (essence-svg (:rival gr) "?") [:div "of one rival"] ])
    (if (:draw3 a)
      [:div.wrap-label.mx-1 [:div "draw 3 cards, reorder, put back"][:div [:em "(may also use on Monument deck)"]]])
    (if-let [rivals (:rivals a)]                                                  ; Rivals Gain
      [:div.d-flex [:div.me-1 "+ all rivals gain"] (render-essence-list rivals)])
    (if-let [place (:place a)]                                                    ; Place essence
      [:div.d-flex (render-essence-list place) [:i.fas.fa-caret-square-down.fa-lg.my-auto]])
    (if-let [draw (:draw a)] [:div (str "draw " draw " card" )])                  ; Draw
    (if-let [straighten (:straighten a) ] [:i.fas.fa-arrow-up.fa-lg.my-auto.me-1 ]) ; Straighten
    (if-let [restriction (:restriction a)] [:span (str restriction)])             ; Straighten Restriction
    (if-let [loselife (:loselife a)]
      [:div.d-flex
        [:div.wrap-label.mx-1 "all\nrivals"]
        (essence-svg :life (- 0 (:loselife a)))
        (if (:ignore a)
          [:div.d-flex.ms-2
            (case (-> a :ignore first key)
              :destroy [:div.wrap-label.mx-1 "rivals may destroy\n1 artifact  to ignore"]
              :discard [:div.wrap-label.mx-1 "rivals may discard\n 1 card  to ignore"]
              [:div.d-flex [:div.wrap-label.mx-1 "rivals\nmay"] (essence-svg (-> a :ignore first key) -1) [:div.wrap-label.mx-1 "to\nignore"]])
            ])
        ])])
  
(defn- gain-module [ cardaction thisuseraction k ]
  [:div.py-2.modal-module
    [:div.h5.text-center (str "Select essence to " (name k))]
    ;[:div.debug (str @thisuseraction)]
    [:div.d-flex.justify-content-center 
      (doall (for [ess (remove #(contains? (set (apply conj (-> cardaction k :exclude) (-> cardaction :convertto :exclude))) %) essence-list)]
        [:div.essence.clickable.px-1 {:key (gensym) :on-click #(swap! thisuseraction update-in [k ess] inc)}
          (essence-svg ess 1)]))
      (reset-essences thisuseraction k)]])

(defn- target-component [ thisuseraction pdata card ]
  (let [target-action     (cond (:straighten @thisuseraction) :straighten 
                                (:turnextra @thisuseraction)  :turnextra
                                (:discard @thisuseraction)    :discard
                                (:destroy @thisuseraction)    :destroycard
                                :default :targetany)
        target-components (cond 
                              (:straighten @thisuseraction) (filter-components (filter :turned? (pdata-public-cards pdata) ) (:restriction @thisuseraction))
                              (:turnextra @thisuseraction)  (remove :turned? (filter #(= (:subtype %) (-> @thisuseraction :turnextra :subtype)) (pdata-public-cards pdata)))
                              (set? (:destroy @thisuseraction)) (->> pdata :public :artifacts (filter #(contains? (:destroy @thisuseraction) (:subtype %))))
                              (= :anyartifact (:destroy @thisuseraction)) (-> pdata :public :artifacts)
                              (= :otherartifact (:destroy @thisuseraction)) (->> pdata :public :artifacts (remove #(= (:uid %) (:uid card))))
                              (:discard @thisuseraction) (-> pdata :private :artifacts)
                              :default (pdata-public-cards pdata))]
    [:div.py-2.modal-module
      [:div.h5.text-center "Select target component"]
      [:div.d-flex.justify-content-center
        (doall (for [component target-components]
          [:img.clickable.mx-1 {
            :key (gensym)
            :src (imgsrc component) 
            :class (if (= (-> @thisuseraction target-action :uid) (:uid component)) "active") 
            :on-click #(
              (swap! thisuseraction assoc target-action component) ; (select-keys component [:uid :type :subtype :name] ))
              (if (:destroy @thisuseraction) 
                  (let [ga (->> component :cost vals (apply +) (+ (:convertplus @thisuseraction 0)))]
                    (if (-> @thisuseraction :convertto :any) (swap! thisuseraction assoc-in [:gainany :any] ga))
                    (if (-> @thisuseraction :convertto :gold) (swap! thisuseraction assoc-in [:gain :gold] ga))))
              )}]))]]))

(defn- target-rival [ plyrs thisuseraction [g eq] ]
  [:div.py-2.modal-module
    [:div.h5.text-center "Select rival"]
    [:div.d-flex.justify-content-center
      (for [[k v] plyrs]
        [:button.btn.btn-outline-secondary.mx-1 {:key (gensym) :on-click #(swap! thisuseraction assoc :gain (hash-map g (-> v :public :essence eq)))} 
          [:div.h5 (-> v :public :mage :name) " (" k ")"]
          (essence-svg eq (-> v :public :essence eq))
          ])]])

(defn- target-convert [ thisuseraction pdata action ]
  (let [cfkey  (-> @thisuseraction :convertfrom keys first) ctkey (-> @thisuseraction :convertto keys first) cfval (-> @thisuseraction :convertfrom cfkey)]
    [:div.py-2.modal-module
      [:h4.text-center "Convert"]
      [:div.d-flex.justify-content-center
        [:div.d-flex 
          (doall (for [e essence-list] 
            [:div.p-1.clickable.essence {:key (gensym) :class (if (-> @thisuseraction :convertfrom e) "active") :on-click #(swap! thisuseraction assoc :convertfrom {e :equal})} 
              (essence-svg e 1 {:size :sm})])) ]
        [:div.text-ab.mx-1 "x"]
        [:select.form-control.mx-1 {:style {:width "fit-content"} :value (str cfval) :on-change #(swap! thisuseraction assoc-in [:convertfrom cfkey] (-> % .-target .-value int))}
            (for [n (range (-> pdata :public :essence cfkey inc))] [:option {:key (gensym "opt") :value n} n])]
        [:div.text-ab.mx-1 "to"]
        (if (-> action :convertto :gold)
            (essence-svg :gold (if (= cfval :equal) "?" cfval))
            (doall (for [e (remove #(contains? (-> action :convertto :exclude) %) essence-list)] 
              [:div.p-1.clickable.essence {:key (gensym) :class (if (-> @thisuseraction :convertto e) "active") :on-click #(swap! thisuseraction assoc :convertto {e :equal})} 
                (essence-svg e (if (= cfval :equal) "?" cfval) {:size :sm})])))]]))

(defn- render-remaining-essence [ pdata cost ]
  [:div.d-flex.justify-content-center.mt-2
    [:div.me-2.mt-auto "Remaining Essence:"]
      [:small.d-flex.justify-content-center (for [[k v] (-> pdata :public :essence)] (essence-svg k (- v (k cost 0)) {:size :sm})  )]])

(defn- modal-use-action-ele [ card a pdata gs ]
  (let [thisuseraction (r/atom (assoc a 
                                  :destroycard (case (:destroy a) :this (select-keys card [:name :uid]) nil)
                                  :cost     (default-payment (-> a :cost (dissoc :exclude)) (-> pdata :public :essence)) 
                                  :gainany  (if (:destroy a) (select-keys (:convertto a) [:exclude]) nil) ;(select-keys (:convertto a) [:any]))
                                  :gain     (cond (:gain a) (-> a :gain (dissoc :any :exclude)) (:destroy a) {})
                                  :place    (-> a :place (dissoc :any :exclude)) ))] ; should be payable amount. Use @ra-app? or universal atom
    (fn []
      (let [{:keys [cost gain place target]} @thisuseraction 
            canpay        (and  (can-pay-cost? (:cost a) (:cost @thisuseraction)) (if (:remove a) (can-pay-cost? (:remove a) (:take-essence card)) true))
            cante         (or (-> a :turnextra nil?) 
                              (-> @thisuseraction :turnextra :uid some?))
            canconvert    (if (:destroy a) 
                              (:gain a) 
                              (if (:convertfrom a)  
                                  (and (->> @thisuseraction :convertfrom keys first (contains? (set essence-list)))
                                       (->> @thisuseraction :convertto keys first (contains? (->> essence-list (remove #(contains? (-> a :convertto :exclude) %)) set)))
                                       (-> @thisuseraction :convertfrom vals first number?)
                                  )
                                  true))
            candestroy    (if (:destroy a) (-> @thisuseraction :destroycard some?) true)
            candiscard    (if (:discard a) (-> @thisuseraction :discard :uid some?) true)
            cangainrival  (if (:gainrivalequal a) (some? (:gain @thisuseraction)) true)
            cangain       (if (:gain a) (and cangainrival (can-pay-cost? (:gain a) (:gain @thisuseraction))) true)
            cangainany    (if (-> @thisuseraction :gainany :any) (can-pay-cost? (:gainany @thisuseraction) (:gain @thisuseraction)) true)
            canplace      (can-pay-cost? (:place a) (:place @thisuseraction))
            canstraighten (or (-> a :straighten nil?) 
                              (-> @thisuseraction :straighten :uid some?))]
        [:div.mb-2.modal-action 
          ;[:div.debug (str a)]
          ;[:div.debug (str @thisuseraction)]
          (action-bar a)
          (if (-> a :cost :any)
              (pay-module pdata a thisuseraction))
          (if (:discard a)
              (target-component thisuseraction pdata card))
          (if (and (:destroy a) (not= :this (:destroy a)))
              (target-component thisuseraction pdata card))
          (if (or (-> a :gain :any) (and (-> a :convertfrom nil?) (-> a :convertto :any)))
              (gain-module a thisuseraction :gain))
          (if (-> a :place :any)
              (gain-module a thisuseraction :place))
          (if (:straighten a)
              (target-component thisuseraction pdata card))
          (if (:targetany a)
              (target-component thisuseraction pdata card))
          (if-let [gre (:gainrivalequal a)]
              (target-rival (-> gs :players (dissoc @uname)) thisuseraction gre))
          (if (:turnextra a)
              (target-component thisuseraction pdata card))
          (if (:convertfrom a) ; {:any equal}
              (target-convert thisuseraction pdata a))
          ;[:div.debug (str "canpay:" canpay " cangain:" cangain " canplace:" canplace " canstraighten:" canstraighten " cangainrival:" cangainrival " candestroy:" candestroy " cangainany:" cangainany " canconvert:" canconvert) ]
          [:div.d-flex.justify-content-between.mb-1.py-2
            [:div.d-flex.justify-content-center
              (if (:cost a)         [:div.d-flex [:div.my-auto "Pay: "] (render-essence-list (if (empty? cost)  (:cost a)  cost))])
              ;(if (:convertfrom @thisuseraction) [:div.d-flex [:div.my-auto "Pay: "] (render-essence-list (if (empty? cost)  (:convertfrom @thisuseraction)  cost))])
              (if (:destroy a)      [:div.my-auto (str "Destroy: " (-> @thisuseraction :destroycard :name))])
              (if (:discard a)      [:div.my-auto (str "Discard: " (-> @thisuseraction :discard :name))])
              (if (or (:gainrivalequal a) (:gain a) (:destroy a))  
                [:div.d-flex 
                  (if-let [ga (-> @thisuseraction :gainany :any)]
                    [:div.d-flex [:div.my-auto.me-1 "Gain"] (essence-svg :any ga) [:div.my-auto.mx-1 ":"]]
                    [:div.my-auto "Gain:"] )
                  (render-essence-list (if (empty? gain)  (:gain a)  gain))])
              (if (:place a) [:div.d-flex [:div "Place:"] (render-essence-list (cond (empty? place) (:place a) (:cost place) (:cost @thisuseraction) :default place))])
              (if (:target a) [:div.d-flex [:div "Target"] ])]
            [:button.btn.btn-primary {
                :disabled (not (and canpay cangain canplace canstraighten cangainrival cante candestroy candiscard cangainany canconvert))
                :on-click #((comms/ra-send! {:action :usecard :useraction @thisuseraction :card card}) (hidemodal))
              } "Use Action"]
          ] 
        ]))))

(defn- modal-use [ gs card pdata ]
  (let [actions (->> (:action card) (remove :react) (remove :reducer_a))]
    (if (or (-> card :name (= "Guard Dog")) (and (-> card :turned? nil?) (not-empty actions))) ;; GUARD DOG CAN BE STRAIGHTENED
      [:div.px-1
        ;[:div.debug (str card)]
        [:h3.text-center.pt-3 (str "Use " (:name card))]
        (for [action actions] ^{:key (gensym)}[modal-use-action-ele card action pdata gs])])))

(defn- useraction-click-handler [ action card useraction ]
  (comms/ra-send! {:action action :card card :useraction useraction})
  (hidemodal))

(defn- reducer-module [ card pdata thisuseraction ]
  (let [reducers (->> pdata pdata-public-cards (remove :turned?) (filter #(->> % :action (map :reducer_a) (remove nil?) not-empty)))]
    (if (not-empty reducers)
      [:div.mb-3
        ;[:div.debug (str reducers)]
        ;[:div.debug (str @thisuseraction)]
        [:div.d-flex 
          [:h5.me-2 "Apply Cost Reductions"] ; hide if there are none?
          [:i.fas.fa-info-circle.text-secondary.fa-sm  {:title "Click on the Essence to reduce"}]]
        (doall (for [r  reducers 
                      :let [a  (->> r :action (filter :reducer_a) first)
                            re_key (-> r :uid keyword)                      ; TODO MAKE ALL UIDs into KEYS
                            [rk rv] (-> a :restriction first)
                            ur (->> @thisuseraction :reducers re_key vals (apply +)) 
                            remaining-reductions (-> a :reduction :any (- ur))]]
            [:div.d-flex {:key (gensym)}
              ;[:div.debug (str a)]
              ;[:div.debug (str ur)]
              ;[:div.debug (str r)]
              [:div.d-flex
                [:div.me-1.my-auto (:name r)]
                (render-essence-list (reduce-kv #(if (number? %3) (assoc %1 %2 (str "- " %3)) %1) (:reduction a) (:reduction a)))
                [:div.my-auto ":"]]
              (render-essence-list (->> @thisuseraction :reducers re_key ) "small")
              (if (= (rk card) rv)
                (if (not= remaining-reductions 0) 
                  [:div.d-flex.ms-auto
                    ;(render-essence-list (-> a :reduction (assoc :any remaining-reductions)) "h5")
                    [:div.d-flex.justify-content-center.mx-2
                      (for [[k v] (reduce #(dissoc %1 %2) (:cost @thisuseraction) (-> a :reduction :exclude) )]
                        (if (not= v 0) [:div.essence.clickable.px-1 {
                          :key (gensym) 
                          :on-click #(
                            (swap! thisuseraction update-in [:cost k] dec)
                            (swap! thisuseraction update-in [:reducers re_key k] inc))
                          } (essence-svg k v)]))]]
                  [:div.d-flex.ms-auto 
                    ;[:div.me-2 [:i.fas.fa-check.text-success]]
                    [:div.d-flex.px-2.essence.clickable {
                      :title "Reset" 
                      :on-click #(
                        (swap! thisuseraction assoc :cost (:cost card))
                        (swap! thisuseraction update :reducers dissoc re_key))
                      } [:h4.m-auto "X"]]])
              [:div.me-1 (-> a :restriction first val clojure.string/capitalize (str "s only"))] ;(if (-> a :restriction) 
            )]))])))

(defn- modal-place-card-ele [ card pdata ]
  (let [thisuseraction (r/atom {:cost (default-payment (:cost card) (-> pdata :public :essence))})
        essence (r/atom (default-payment (:cost card) (-> pdata :public :essence)))]
    (fn []
      (let [card (-> @ra-app :modal :card) 
            label   (if (= "artifact" (:type card)) "Place" "Claim")
            canpay  (can-pay-cost? (:cost card) (:cost @thisuseraction) (:reducers @thisuseraction) )
            cangain (if (-> card :action first :bought) (can-pay-cost? (-> card :action first :gain) (:gain @thisuseraction)) true)] 
        [:div.pt-3.pb-2.mb-3.ps-2 {:style {:border-bottom "1px solid #222222"}}
          [:h3 (str label " " (:name card))]
          ;[:div.debug (str card)]
          ;[:div.debug (str (can-pay-cost-hash (:cost card) {:cost @thisuseraction}))]
          ;[:div.debug (str @thisuseraction)]
          (if (:cost card) 
            [:div
              [:div.bg-essence.mb-2 (render-essence-list (:cost card) "h5")]  ;; TODO REDUCED COST?
              (reducer-module card pdata thisuseraction)
              (if (-> card :cost :any) 
                (pay-module pdata nil thisuseraction)
                (render-remaining-essence pdata (:cost @thisuseraction)))
              (if (-> card :action first :bought)
                [:div 
                  (action-bar (-> card :action first))
                  (gain-module (-> card :action first) thisuseraction :gain)])
              [:div.d-flex.my-2
                [:div.d-flex.me-3
                  [:div.h4.me-3.my-auto "Pay:"]
                  (render-essence-list (:cost @thisuseraction) )]
                (if (-> card :action first :bought)
                  [:div.d-flex 
                    [:div.h4.me-3 "Gain:"]
                    (render-essence-list (:gain @thisuseraction))])
                [:button.btn.btn-primary.ms-auto {
                  :on-click #((comms/ra-send! {:action :place :card card :essence (-> @thisuseraction :cost (dissoc :any)) :gain (:gain @thisuseraction)}) (hidemodal))
                  :disabled (not (and canpay cangain)) } label]]]
            [:div.d-flex.my-2 [:button.btn.btn-primary.ms-auto {:on-click #((comms/ra-send! {:action :place :card card}) (hidemodal))} label]])
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
        [:div.carousel-item.h-100 {:class (if (= pg 4) "active") :key (gensym "carousel") } 
          [:div.p-2.h-100 {:style {:background "#BBB"}}
            [:h4 "Chat Commands"]
            [:ul 
              [:li "/essence (name) (amount) - Set your current Essence (name) to Value (amount)" ]
              [:li "/endturn - End your turn" ]
              [:li [:i "/turn (card name) - Turn or Straighten Card (card name) (case sensitive)" ]]
              ]]]]
      [:button.carousel-control-prev {:type "button" :on-click (fn [] (swap! ra-app update-in [:modal :info] #(max (dec %) 0)))} [:span.carousel-control-prev-icon]]    
      [:button.carousel-control-next {:type "button" :on-click (fn [] (swap! ra-app update-in [:modal :info] #(min (inc %) 4)))} [:span.carousel-control-next-icon]]    
        
    ]])

;;; Main
(defn- modal [ gs pdata uname ] ; Content also driven via @ra-app
  (let [{:keys [show? card discard info]} (:modal @ra-app)]
    [:div.modal.ra-main {:hidden (not show?) :on-click #(hidemodal)}
      (cond 
        info    [modal-info info]
        card    [:div.modalcontent.p-2.rounded.d-flex {:on-click #(.stopPropagation %)}
                  [:div.h-100.text-center {:style {:position "relative"}}
                    [:img.card.h-100 {:src (imgsrc card) :class (cond (:turned? card) "disabled")}]
                    (if-let [te (:take-essence card)] 
                        [:div.p-1.rounded {:style {:position "absolute" :top "10px" :right "10px" :background "rgba(0,0,0,0.3)"}}
                          (for [[k v] te] [:div {:key (gensym)} (essence-svg k v {:size :lg})])])]
                  [:div ; {:style {:width "500px"}}
                    [:button.btn-sm.btn-close.ms-auto.bg-light {:on-click #(hidemodal) :style {:position "absolute" :right "5px" :top "5px"}}]
                  (cond   ;; options based on card and game state
                    (= :collect (:phase gs))          (modal-collect-essence card pdata uname)
                    (:can-use? card)                  (modal-use gs card pdata)
                    (:target? card)                   (modal-target card pdata))]]
        
        discard [:div.modalcontent.p-2.rounded.d-flex.flex-wrap.w-100.justify-content-center
                  [:div (str discard)]
                  (for [c discard] 
                    [:div.m-1 {:key (gensym) :style {:width "20%" :height "auto"}}
                      [:img.img-fluid { :src (imgsrc c)}]]
                    )])]))

;; functions

(defn- all-player-essence? [ cost pess ]
  (->>  (reduce-kv 
          (fn [m k v]
            (update m k - v))
          pess cost)
        vals (filter #(> % 0)) (apply +) (= 0)))

(defn- react-btn-disabled? [ cost ess pdata ]
  (not 
    (or
      (all-player-essence? ess (-> pdata :public :essence))
      (can-pay-cost? 
        {:any (-> cost :life (* 2))}  ; double all :life cost
        (reduce-kv #(if (= :life %2)
                        (assoc %1 %2 (* 2 %3))
                        %1) ess ess)  ;count life paid as double
        ))))

(defn- pay-essence-module [ pdata cost ]
  (let [useraction (r/atom {})]
    (fn []
      [:div
        (conj 
          (pay-module pdata {:cost cost} useraction)
          [:button.btn.btn-outline-secondary.w-100.mt-1 {
              :disabled (react-btn-disabled? cost (:cost @useraction) pdata)
              :on-click #(comms/ra-send! {:action :react :card nil :useraction @useraction})}
            [:div.d-flex.justify-content-center 
              [:div.me-2.text-dark "Pay"] 
              (render-essence-list (:cost @useraction))]])
              ])))

;;; Main
(defn- react-modal [ gs pdata uname]
  (let [ll-active? (->> gs :players vals (map :loselife) (remove nil?) count (< 0))
        ll-plyr (->> gs :players vals (filter :loselife) first :loselife :plyr)
        ll-plyrs (reduce (fn [m k] (if (= k ll-plyr) (dissoc m k) m)) (:players gs) (-> gs :plyr-to))]
    [:div.modal.ra-main {:hidden (not ll-active?)}
      [:div.modalcontent.p-2.rounded {:style {:min-width "400px" :height "80%"}}
        [:div.row
          [:div.col
            [:h4 "Waiting for players to react to " (->> gs :players vals (filter :loselife) first :loselife :name str)]
            [:div.row
              [:div.col-7
                [:img.img-fluid {:src (->> gs :players vals (filter :loselife) first :loselife imgsrc)}]]
              [:div.col-5
                (for [[llp-k llp-v] ll-plyrs]
                  [:div.d-flex.mb-2 {:key (gensym)} 
                    [:div.h4.me-2.my-auto llp-k]
                    (if-let [loselife (:loselife llp-v)]
                            (essence-svg :life (- 0 (:loselife loselife)))
                            [:div {:key (gensym)} (str llp-k)])])]]]
          (if (-> pdata :loselife)
            (let [reactions   (->>  (pdata-public-cards pdata) 
                                    (remove :turned?) 
                                    (filter #(->> % :action 
                                      (filter (fn [a] (and (or (-> a :source nil?) (= (:source a) (-> pdata :loselife :source))) (= :loselife (:ignore a))))) 
                                      count (< 0))))
                  cost        {:life (-> pdata :loselife :loselife)}
                  ignorecost  (-> pdata :loselife :ignore)
                  ess         (-> pdata :public :essence)]
              [:div.col
              ; lose life
                [:div.d-flex.justify-content-between.mb-2
                  [:div.h4 "Choose how to react"]
                  [:i.fas.fa-info-circle.text-secondary {:title "Each rival must lose the indicated number of Life essences from their pool. For each Life essence a rival does not have, they must instead, if possible, lose any 2 other essences in their pool (including Gold)."}]]
                ;[:div.debug (-> pdata :loselife :source str)]
                [:button.btn.btn-outline-secondary.w-100.me-1.mb-1 {
                    :disabled (not (can-pay-cost? cost {:life (min (:life cost) (:life ess))})) 
                    :on-click #(comms/ra-send! {:action :react :card nil :useraction {:cost cost}})} 
                  [:div.d-flex.justify-content-center 
                    [:div.me-2.text-dark "Lose Life"] 
                    (essence-svg :life (-> cost :life (* -1)))]]
              ; ignore option(s)
                (if (some? ignorecost)
                    (case (-> pdata :loselife :ignore first key)
                      :discard 
                        [:div.modal-module.mb-2 
                          [:div.text-center "Ignore: Discard 1"]
                          [:div.d-flex.justify-content-center.py-1
                            (for [card (-> pdata :private :artifacts)]
                              [:div.mx-1.clickable {
                                  :key (gensym)
                                  :on-click #(comms/ra-send! {:action :react :useraction {:discard card}})
                                }
                                [:img.modal-reaction {:src (imgsrc card) :title (str "Discard " (:name card))}]
                              ])]]
                      :destroy 
                        [:div.modal-module.mb-2 
                          [:div.text-center "Ignore: Destroy an artifact"]
                          [:div.d-flex.justify-content-center.py-1
                            (for [card (-> pdata :public :artifacts)]
                              [:div.mx-1.clickable {
                                  :key (gensym)
                                  :on-click #(comms/ra-send! {:action :react :useraction {:destroy true :destroycard (select-keys card [:name :uid])}})
                                }
                                [:img.modal-reaction {:src (imgsrc card) :title (str "Destroy " (:name card))}]
                              ])]]
                      [:button.btn.btn-outline-secondary.w-100.me-1.mb-1 {
                          :disabled (not (can-pay-cost? ignorecost {(-> ignorecost first key) (min (-> ignorecost first val) ((-> ignorecost first key) ess))}))
                          :on-click #(comms/ra-send! {:action :react :card nil :useraction {:cost ignorecost}})}
                        [:div.d-flex.justify-content-center
                          [:div.me-2.text-dark "Ignore"] 
                          (essence-svg (-> ignorecost first key) (-> ignorecost first val (* -1)))]]))
                [pay-essence-module pdata cost]
                (if (not-empty reactions)
                  [:div.p-2.mt-2.modal-module
                    [:div.h5.text-center "Use a power"]
                    [:div.d-flex.justify-content-center.mb-1
                      (for [react reactions :let [a (->> react :action (filter #(= :loselife (:ignore %))) first)]]
                        [:button.btn.btn-outline-secondary.mx-1 {
                            :key (gensym) 
                            :disabled (not (can-pay-cost? (:cost a) (default-payment (:cost a) (-> pdata :public :essence))))
                            :title (str react)
                            :on-click #(comms/ra-send! {:action :react :card react :useraction a})}
                          [:img.modal-reaction.clickable {:src (imgsrc react)} ]])]])
              ]))]]]))

(defn- winner-modal [ gs ]
  [:div#winnermodal.modal.ra-main {:hidden (not= (:status gs) :gameover) }
    [:div.modalcontent.p-2.rounded {:style {:min-width "300px"}}
      [:div.h3.text-center "GAME OVER"]
      ;[:div.debug (-> gs :scores)]
      (for [s (map-indexed #(assoc %2 :place %1) (:scores gs))]
        [:div.d-flex {:key (gensym)}
          [:div.d-flex {:class (if (> (:score s) 9) "h4" "h5")}
            [:div.me-2.my-auto (get ["1st" "2nd" "3rd" "4th"] (:place s))]
            [:div.h4.vp.me-3  (:score s)]
            [:div.me-2.my-auto (:player s)]
            ]])
      ;[:div.debug (-> gs str)]
      [:div.d-flex [:button.btn.btn-primary.ms-auto {:on-click #(comms/leavegame @gid)} "Quit"]]
    ]])

;;; Main
(defn- divine-modal [ ]
  (let [useraction (r/atom [])]
    (fn []
      (let [gs    (:state @gm)
            mi    (->> gs :magicitems (filter #(= (:owner %) @uname)) first)
            pdata (-> gs :players (get @uname) (assoc-in [:public :magicitem] mi))
            uname @uname]
        [:div.modal.ra-main {:hidden (-> pdata :draw3 nil?)}
          [:div.modalcontent.p-2.rounded {:style {:min-width "400px"}}
            [:h4.p-2 "Draw 3 cards, reorder, put back " [:i.small "(may also use on Monument deck)"]]
            (if (-> pdata :draw3 true?)
                [:div
                  [:h5 "Select a deck"]
                  [:div.d-flex.justify-content-around.p-2
                    [:button.btn.btn-outline-secondary {:on-click #(comms/ra-send! {:action :draw3 :deck "artifact"})} "Artifact"]
                    [:button.btn.btn-outline-secondary {:on-click #(comms/ra-send! {:action :draw3 :deck "monument"})} "Monument"]
                    ;[:button.btn.btn-outline-secondary {:on-click #(comms/ra-send! {:action :draw3 :cancel? true})} "Cancel"] ;TODO would require turning trigger action also
                    ]]
                [:div
                  [:h5.text-center (-> pdata :draw3 (str " deck") clojure.string/capitalize)]
                  [:div.d-flex.justify-content-center.mb-2
                    (doall (for [a (-> pdata :private :draw3)] 
                      (if (contains? (->> @useraction (map :name) set) (:name a))
                        [:img.modal-divine.selected.mx-1 {:key (gensym) :src (imgsrc a)}]
                        [:img.clickable.modal-divine.active.mx-1 {:key (gensym) :on-click #(reset! useraction (conj @useraction a)) :src (imgsrc a)}])))]
                  [:h5.text-center "Replace Order"]
                  [:div.d-flex.justify-content-center
                    [:div.my-auto.me-1 "Top"] 
                    [:div.d-flex.justify-content-center
                      (for [a @useraction] [:img.clickable.modal-divine.mx-1 {:key (gensym) :src (imgsrc a)}])]
                    [:div.my-auto.ms-1 "Bottom"]]
                  [:div.d-flex 
                    [:button.btn.ms-auto.btn-outline-secondary {:on-click #(reset! useraction [])} "Reset"]
                    [:div.d-flex [:button.ms-2.btn.btn-primary {:on-click #((comms/ra-send! {:action :draw3 :useraction @useraction}) (reset! useraction []))} "OK"]]]]
                  )
          ]]
    ))))


;; Main View
;;; Modules
(defn- pop-mon-mi-row [ status phase action ] 
  (let [target? (= [:play :play] [status action])]
  ; Toogle :target? based on (:phase gs) (:action pdata)
    [:div.d-flex.justify-content-around
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
            [:div [:div.d-flex.justify-content-center (for [ [r n] (-> v :public :essence)] (essence-svg r n {:size :sm}) )]]
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
                (let [collected? (:collected? pdata) collect-remaining (filter :collect-essence (-> gs (player-public-cards uname))) ; include take-essence also
                      disabled?  (->> pdata :public :artifacts (filter :collect-essence) (filter :collect-mandatory) count (< 0))]
                  [:div.d-flex
                    [:div.me-2.my-auto "Collect essence"]
                    [:button.btn.mt-1 {
                        :class   (cond collected? "btn-success active" (empty? collect-remaining) "btn-primary" disabled? "btn-secondary" :default "btn-warning") 
                        :disabled disabled?
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
          [:div.d-flex.justify-content-center (doall (for [ [r n] (-> pdata :public :essence)] (essence-svg r n) ))]
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
          [:img.img-fluid {:src (if-let [setupmage (->> pdata :private :mages (filter #(= (:uid %) (-> pdata :public :mage))) first)]
                                        (imgsrc setupmage)
                                        (if-let [mage (-> pdata :public :mage)]
                                          (imgsrc mage)
                                          (imgsrc {:type "mage"} "back")))}]]]
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

(defn- play [ gs uname pdata ]
  [:div.row.ra-main
    (opponentdisplay gs uname)
    (pop-mon-mi-row (:status gs) (:phase gs) (:action pdata))
    (playerdisplay gs uname pdata)
  ])

(defn- page-banner [ gs ] 
  [:div.d-flex.justify-content-between.ra-main.m-1 
    [:h2.mt-auto.mb-0 (str "Res Arcana - " (case (:status gs) :setup "Setup" :gameover "Game Over" (-> gs :phase name clojure.string/capitalize (str " phase"))))]
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
      (if (contains? #{"p1" "p2" "AI123"} @uname)
        [:div.btn-group.btn-group-sm.me-2
          [:button.btn.btn-secondary {:on-click #(comms/ra-send! {:action :swapgame :game 1})} "G1"]
          [:button.btn.btn-secondary {:on-click #(comms/ra-send! {:action :swapgame :game 2})} "G2"]
          [:button.btn.btn-secondary {:on-click #(comms/ra-send! {:action :swapgame :game 3})} "G3"]
          [:button.btn.btn-secondary {:on-click #(comms/ra-send! {:action :swapgame :game 4})} "G4"]
          [:button.btn.btn-secondary {:on-click #(comms/ra-send! {:action :swapgame :game 5})} "G5"]])
      [:button.btn.btn-sm.btn-primary.ms-auto.me-2 {:on-click #(swap! ra-app assoc :modal {:show? true :info (if (= :setup (:status gs)) 0 1)})} [:i.fas.fa-info-circle]]
      [:button.btn.btn-sm.btn-danger {:on-click #(if (js/confirm "Are you sure you want to Quit?") (comms/leavegame @gid))} [:i.fas.fa-times-circle]]]])

;;; MAIN ;;;
(defn ramain [ ]
  (-> js/document .-body (.removeAttribute "style"))
  (-> js/document .-body .-style .-backgroundImage (set! "url(/img/ra/ra-bg.png)"))
  (-> js/document .-body .-style .-backgroundSize (set! "100%"))
  ;(-> ((js* "$") "#navbar") (.attr "hidden" true))
  (let [gs    (:state @gm)
        mi    (->> gs :magicitems (filter #(= (:owner %) @uname)) first)
        pdata (-> gs :players (get @uname) (assoc-in [:public :magicitem] mi))]
    [:div.container-fluid
      ;[:div.debug (-> gs (dissoc :monuments :magicitems) str)]
      (react-modal gs pdata @uname)
      [divine-modal gs pdata @uname]
      (modal gs pdata @uname)
      (winner-modal gs)
      [chat gs]
      [page-banner gs]
      (case (-> @gm :state :status)
        :setup [setup gs @uname pdata]
        [play gs @uname pdata])
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