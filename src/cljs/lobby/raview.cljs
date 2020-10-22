(ns lobby.raview
  (:require
    [reagent.core :as r]
    [lobby.comms :as comms]))
    
(def ra-app (r/atom {}))
(def cardsize (r/atom {:base 4 :w 80 :h 112}))

  
(defn rendercard [ type card ext ]
  (let [imgsrc (str "/img/ra/" type "-" (:id card) ext)]
    [:img.img-fluid.card.mr-2 {
      :key (gensym)
      :style {:display "inline-block"}
      :width (:w @cardsize) 
      :height (:h @cardsize)
      :src imgsrc
      ;:class (if (= (str type (:id card)) (:selected @ra-app)) "active")
      ;:on-click #(let [sel (str type (:id card))] 
      ;            (if (= sel (:selected @ra-app))
      ;              (swap! ra-app dissoc :selected)
      ;              (swap! ra-app assoc :selected sel)))
      :on-mouse-over #(swap! ra-app assoc :preview imgsrc)
      :on-mouse-out #(swap! ra-app assoc :preview nil)}]))
  
(defn rendercardback [ type ]
  [:img.img-fluid.mr-2 {
    :width (:w @cardsize) :height (:h @cardsize)
    :src (str "/img/ra/" type "-back.png")}])
  
(defn placesofpower [ gm ]
  [:div.mx-2
    [:div.h5.text-center "Places of Power"]
    [:div.d-flex
      (doall (for [pop (-> gm :state :pops)]
        (rendercard "pop" pop ".png")))]])
    
(defn monuments [ gm ]
  [:div.mx-2
    [:div.h5.text-center "Monuments"]
    [:div.d-flex
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
          
;(defn ra-send [ ?data ]
;  (chsk-send! [:lobby/ra-action ?data] 5000 nil))
    
(defn ramain [ gid gm uname ]
  (-> ((js* "$") "body") 
      (.css "background-image" "url(/img/ra/ra-bg.png")
      (.css "background-size" "100%"))
  ;(-> ((js* "$") "#navbar") (.attr "hidden" true))
  [:div.container-fluid.my-2
    [:div.row 
      [:div.col-sm-9        
        (case (-> gm :state :status)
          :setup (setup gid gm uname)
          [:h5 "Unmapped Status"])]
      [:div.col-sm-3
        [:div {:style {:height "400px"}}
          (let [preview (:preview @ra-app)]
            [:img.img-fluid {:hidden (nil? preview) :src preview}])]
        [:div#chat.h-100 {:style {:background-color "white" :opacity "0.8" :border "1px solid grey" :border-radius "5px"}}]]]])