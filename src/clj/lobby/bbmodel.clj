(ns lobby.bbmodel
  (:require 
    [lobby.bbdata :as bbdata]))

(defn obfuscate [ state uname ]
  state)

(defn parseaction [ state ?data uname]
  state)

(defn setup [ plyrs ]
  (let [ highlights (-> bbdata/highlights shuffle) ]
    {  
      :teams ( filter #(not= "Freebooter" (:team %)) bbdata/players )
      :highlights {
        :public (take 2 highlights)
        :secret (nthrest highlights 2)
        :discard []
      }
      :status :setup
      :players (zipmap plyrs (repeat {:public {} :private {} :secret {}}))
      :chat []
    }))