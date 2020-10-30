(ns lobby.ramodel
  (:require 
    [lobby.radata :refer [data]]))
  
;(defonce gamestate (atom {}))

(def playerdata {
  :public {
    :mage 0
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
    :vp 0
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
                (assoc k v)
                (assoc-in [k :private] 
                  (if (= k plyr)
                      (:private v)
                      (reduce-kv #(assoc %1 %2 (count %3)) {} (:private v))))
                (assoc-in [k :secret] (reduce-kv #(assoc %1 %2 (count %3)) {} (:secret v))))
          ) {} (:players state)))))
          
(defn- set-player-hands [ plyrs mages artifacts ]
  (zipmap 
    plyrs 
    (map-indexed  
      (fn [id nm]
        (let [mstart (* id 2) astart (* id 8)]
          (-> playerdata
              (assoc-in [:private :mages] (subvec mages mstart (+ mstart 2)))
              (assoc-in [:private :artifacts] (subvec artifacts astart (+ astart 8))))))
      plyrs)))
      
(defn- make-ai-choices [ players ]
  ;(prn players)
  (reduce-kv  
    (fn [m k v]
      (assoc m k
        (if (= k "AI")
            (-> v 
                (assoc-in [:private :mages 0 :selected] true) 
                (assoc-in [:public :mage] 1))
            v))) {} players))

(defn setup [ plyrs ]
  (let [mages (-> @data :mages shuffle)
        artifacts (-> @data :artifacts shuffle)
        monuments (-> @data :monuments shuffle)
        pops (-> @data :placesofpower)
        turnorder (shuffle plyrs)
        players (set-player-hands plyrs mages artifacts) ]
    {
      :ready #{}
      :status :setup
      :pops (map (fn [base] (rand-nth (filter #(= (:base %) base) pops))) (->> pops (map :base) frequencies keys))
      :monuments {
        :public (take 2 monuments)
        :secret (nthrest monuments 2)}
      :magicitems (:magicitems @data)
      :turnorder turnorder
      :p1 (first turnorder)
      :players (make-ai-choices players)}))
      
(defn- start-game [ gs ]
  (-> gs
      (assoc :state :started)    ; Start State
      (assoc :players
        (reduce-kv 
          (fn [m k v]
            (let [artifacts (-> v :private :artifacts)]
              (-> m
                  (assoc k v)
                  (assoc-in [k :public :mage] (->> v :private :mages (filter :selected) first)) ; Switch Selected Mage to Public
                  (assoc-in [k :private :artifacts] (take 2 artifacts))    ; Draw 2 artifacts
                  (assoc-in [k :secret :artifacts] (nthrest artifacts 2))  ; Artifact deck
          ))) {} (:players gs)))
  
  ))
      
      
(defn- check-start [ gs ]
  (if (= (count (:players gs)) (reduce + (map (fn [[k v]] (-> v :public :mage)) (:players gs)))) ; All players have selected a mage
    ; All players have selected a Magic Item
      (start-game gs)
      gs))
      
(defn selectmage [ gamestate ?data uname ]
; Only runs for game setup...
  (-> gamestate
    (assoc :players 
      (reduce-kv  
        (fn [m k v]
          (assoc m k
            (if (= k uname)
                (-> v 
                    (assoc-in [:private :mages] (map #(if (= (:name %) (-> ?data :card :name)) (assoc % :selected true) (dissoc % :selected)) (-> v :private :mages))) 
                    (assoc-in [:public :mage] (if (-> ?data :select? true?) 1 0) ))
                v))) {} (:players gamestate)))
    check-start))
    
(defn parseaction [ gamestate ?data uname ]
  (println "Action:" ?data)
  (case (:action ?data)
    :toggleready (update gamestate :ready (if (contains? (:ready gamestate) uname) disj conj) uname)
    :selectmage (selectmage gamestate ?data uname)
    gamestate))