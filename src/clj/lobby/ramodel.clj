(ns lobby.ramodel
  (:require 
    [lobby.radata :refer [data]]))
  
;(defonce gamestate (atom {}))

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
  
;(reduce-kv (fn [m k v] (assoc m k (count v))) {} t)
  
(defn obfuscate [ state plyr ]
  (-> state 
      (assoc-in [:monuments :secret] (-> state :monuments :secret count))
      (assoc :players
        (reduce-kv 
          (fn [m k v]
            (-> m 
              (assoc-in [k :public] (:public v))
              (assoc-in [k :private] 
                (if (= k plyr)
                    (:private v)
                    (reduce-kv #(assoc %1 %2 (count %3)) {} (:private v))))
              (assoc-in [k :secret] (reduce-kv #(assoc %1 %2 (count %3)) {} (:secret v))))
          ) {} (:players state)))))

(defn setup [ plyrs ]
  (let [mages (-> @data :mages shuffle)
        artifacts (-> @data :artifacts shuffle)
        monuments (-> @data :monuments shuffle)
        pops (-> @data :placesofpower)]
    {
      :ready #{}
      :status :setup
      :pops (map (fn [base] (rand-nth (filter #(= (:base %) base) pops))) (->> pops (map :base) frequencies keys))
      :monuments {
        :public (take 2 monuments)
        :secret (nthrest monuments 2)}
      :players (zipmap 
        plyrs 
        (map-indexed  
          (fn [id nm]
            (let [mstart (* id 2) astart (* id 8)]
              (-> playerdata
                  (assoc-in [:private :mages] (subvec mages mstart (+ mstart 2)))
                  (assoc-in [:private :artifacts] (subvec artifacts astart (+ astart 8))))))
          plyrs))
    }))
    
    
(defn parseaction [ gamestate ?data uname ]
(prn gamestate )
(prn ?data)
  (case (:action ?data)
    :toggleready (update gamestate :ready (if (contains? (:ready gamestate) uname) disj conj) uname)
    gamestate))