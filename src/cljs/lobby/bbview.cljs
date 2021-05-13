(ns lobby.bbview
  (:require
    [reagent.core :as r]
    [lobby.model :as model :refer [ uname gid gm ]]
    [lobby.comms :as comms]))
    
; Atom and Atom Functions

(def bb-app (atom {}))

(defn bbmain [ ]
  (let [state (:state @gm)]
    [:div.container-fluid.my-2
      [:h4 "BBTM"]
      [:div (str state)]]))