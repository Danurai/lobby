(ns lobby.view
  (:require 
    [lobby.model :refer [gm]]
    [reagent.core :as r]))

(defn main []
  (let [msg (r/atom "")]
    (fn [] 
      (-> ((js* "$") "body") (.removeAttr "style") )
      [:div
        [:div.container "Hello, world. Empty @gm? " (-> @gm nil? str)]])))

;  (let [msg (r/atom "")]
;    (fn []
;      (-> ((js* "$") "body") (.removeAttr "style"))
;      [:div
;        (if (:state @gm)
;          (gamehooks)
;          [:div.container.my-3 {:style {:min-height "400px"}}
;            [:div.row
;              (if @gm
;                (gamelobby @gid @gm @uname)
;                (createjoin))    
;              [:div.col-sm-4.h-100
;                [:div.p-2.border.rounded.mb-2 {:style {:height "50%"}}
;                  [:h5 "Connected"]
;                  (for [conn (:user-hash @model/app)]
;                    [:div {:key (gensym)} (key conn)])]
;                [:div
;                  [:div.border.rounded.mb-1.p-1 {:style {:height "200px" :font-size "0.8rem" :overflow-y "scroll" :display "flex" :flex-direction "column-reverse"}}
;                    (for [msg (:chat @model/app) :let [{:keys [msg uname timestamp]} msg]]
;                      [:div {:key (gensym) :style {:word-wrap "break-word"}}
;                        [:span.me-1 (model/timeformat timestamp)]
;                        [:b.text-primary.me-1 (str uname ":")]
;                        [:span msg]])]
;                  [:form {:on-submit (fn [e] (.preventDefault e) (comms/sendmsg! @msg) (reset! msg ""))}
;                    [:div.input-group
;                      [:input.form-control.bg-light {
;                        :type "text" :placeholder "Type to chat"
;                        :value @msg
;                        :on-change #(reset! msg (-> % .-target .-value))}]
;                      [:span.input-group-append [:button.btn.btn-outline-secondary {:type "btn"} [:i.fas.fa-arrow-right]]]]]]]]
;            [:div.row                      
;              [:div [:small (str (dissoc @model/app :chat))]]
;              [:button.btn.btn-sm.btn-danger {:on-click #(comms/reset)} "Reset"]]])])))