(ns lobby.view
	(:require 
		[lobby.model :as model]
		[lobby.comms :as comms]))
    
(defn main []
  [:div
    [:div.row
      [:div.col-sm-8
        [:div.list-group
          (for [x (range 10)]
            [:div.list-group-item {:key x} x])]]
      [:div.col-sm-4
        [:div.p-2.border  {:style {:max-height "50%"}}
          [:h5 "Connected"]
          (for [conn (:user-hash @model/app)]
            [:div {:key (gensym)} (key conn)])]]]
		[:div (str @model/app)]
		[:button.btn {:on-click #(comms/sendone)} "Send Req"]])