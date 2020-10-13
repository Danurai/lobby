(ns lobby.ramodel
  (:require
    [lobby.comms :as comms]))

(defn ramain [ gm ]
  [:div.container-fluid {:style {:background-color "grey"}}
    [:div.row
      
      [:button.btn.btn-sm.btn-danger {:on-click #(comms/leavegame (:gid gm))} "Leave"]
      ]]
)