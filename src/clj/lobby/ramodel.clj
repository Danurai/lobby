(ns lobby.ramodel
  (:require 
    [lobby.radata :refer [data]]))
  
(defonce gamestate (atom {}))

(def playerdata {
  :public {
    :mage nil
    :artifacts nil
    :monuments nil
    :pops nil
    :resources {
      :gold 1
      :calm 1
      :elan 1
      :life 1
      :death 1
    }
  }
  :private { ; player knows
    :mages nil
    :artifacts nil
  }
  :secret { ; no-one knows
    :discard nil
  }})
  
(defn setup [ plyrs ]
  (let [mages (-> @data :mages shuffle)
        artifacts (-> @data :artifacts shuffle)]
    (reset! gamestate {
      :status :setup
      :monuments (-> @data :monuments shuffle)
      :pops (->> @data :monuments shuffle (take 5))
      :players (zipmap 
        plyrs 
        (map-indexed  
          (fn [id nm]
            (let [mstart (* id 2) astart (* id 8)]
              (-> playerdata
                  (assoc-in [:private :mages] (subvec mages mstart (+ mstart 2)))
                  (assoc-in [:private :artifacts] (subvec artifacts astart (+ astart 8))))))
          plyrs))
    })))
    