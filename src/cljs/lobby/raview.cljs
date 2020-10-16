(ns lobby.raview
  (:require
    [reagent.core :as r]
    [lobby.comms :as comms]))
    
(def ra-app (r/atom {}))
(def cardsize (r/atom {:base 5 :w 125 :h 175}))

(defn getimgfile [ type id ]
  (str "/img/ra/" type "-" id ".jpg"))
    
(defn setup [ gm uname ]
  (let [mydata (get-in gm [:state :players uname])]
    [:div.col.mx-2
      [:div.row
        [:div.h5.mr-2 "Setup"][:div "Choose your Mage"]]
      [:div.row.mb-2.tip "Tip: Do you have dragons, creatures, or ways to make gold? This may suggest Places of Power that will work well for you or if you can buy several monuments."]
      [:div.row ;mage choice
        (doall (for [mage (-> mydata :private :mages) 
                      :let [mgid (:id mage) imgsrc (getimgfile "mage" mgid)]]
          ^{:key (gensym)}[:div.card.mr-2 {
            :class (if (= (str "mage" mgid) (:selected @ra-app)) "active")
            :on-mouse-over #(swap! ra-app assoc :preview imgsrc)
            :on-mouse-out #(swap! ra-app assoc :preview nil)
            :on-click #(swap! ra-app assoc :selected (str "mage" mgid))}
            [:img.img-fluid {:width (:w @cardsize) :height (:h @cardsize) :src imgsrc}]]))]
      [:div.row ;artifacts
        (doall (for [artifact (-> mydata :private :artifacts) 
                      :let [artifactid (:id artifact) imgsrc (getimgfile "artifact" artifactid)]]
          ^{:key (gensym)}[:div.card.mr-2 {
            :on-mouse-enter #(swap! ra-app assoc :preview imgsrc)
            :on-mouse-leave #(swap! ra-app assoc :preview nil)}
            [:img.img-fluid {:width (:w @cardsize) :height (:h @cardsize) :src imgsrc}]]))]
      [:div.row ;Ready
        [:button.btn "Ready"]
        ]
      ]))
    
(defn ramain [ gm uname ]
  (-> ((js* "$") "body") 
      (.css "background-image" "url(/img/ra/ra-bg.png")
      (.css "background-size" "100%"))
  ;(-> ((js* "$") "#navbar") (.attr "hidden" true))
  [:div.container-fluid.my-2 {:style {:position "relative"}}
    [:div.preview {:hidden (-> @ra-app :preview nil?)}
      [:img.img-fluid {:src (:preview @ra-app)}]]
    ;[:div.settings.mr-1.p-2
    ;  [:div [:i.fas.fa-cog.text-secondary]]
    ;  [:label "Image Size: " (get ["" "" "" "Tiny" "XS" "Small" "Medium" "Large" "XL" "Huge" "Full"] (:base @cardsize))]
    ;  [:input.custom-range.w-100 {
    ;    :type "range" :min 3 :max 10 :value (:base @cardsize) 
    ;    :on-change (fn [e] (let [base (js/parseInt (.. e -target -value)) mult (/ base 10)] (reset! cardsize {:base base :w (* 250 mult) :h (* 350 mult)})))
    ;    }]]
    [:div.row.mb-2
      (case (-> gm :state :status)
        :setup (setup gm uname)
        [:h5 "Unmapped Status"])]
    [:div.row
      [:button.btn.btn-sm.btn-danger {:on-click #(comms/leavegame (:gid gm))} "Leave"]]])