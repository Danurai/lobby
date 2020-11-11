(ns lobby.daview
  (:require 
    [lobby.comms :as comms]))

(defn damain [ gid gm uname ]
  [:div.container.my-3
    [:h5 "Welcome to The Hulk"]
    [:div 
      [:button.btn.btn-sm.btn-dark {:on-click #(if (js/confirm "Are you sure you want to Quit?") (comms/leavegame gid))} "Quit"]]])