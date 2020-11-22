(ns lobby.ramodel
  (:require 
    [lobby.radata :refer [data]]))
  
;(defonce gamestate (atom {}))
(def gs {
  :status :setup
  :pops []
  :monuments []
  :magicitems (:magicitems @data)
  :turnorder []
  :p1 nil
  :players {}
})
  
(def playerdata {
  :public {
    :mage 0
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
  }
  :secret { ; no-one knows
    :discard nil
  }
})

(defn obfuscate [ state plyr ]
  (-> state 
      (assoc-in [:monuments :secret] (-> state :monuments :secret count))
      (assoc :players
        (reduce-kv 
          (fn [m k v]
            (-> m 
                (assoc k v)
                (assoc-in [k :public] (:public v))
                (assoc-in [k :private] 
                  (if (= k plyr)
                      (:private v)
                      (reduce-kv #(assoc %1 %2 (count %3)) {} (:private v))))
                (assoc-in [k :secret] (reduce-kv #(assoc %1 %2 (count %3)) {} (:secret v))))
          ) {} (:players state)))))
          
(defn- make-ai-choices [ players ]
  (reduce-kv  
    (fn [m k v]
      (assoc m k
        (if (some? (re-matches #"AI\d+" k))
            (-> v 
                (dissoc :action)
                (assoc-in [:private :mages 0 :selected] true) 
                (assoc-in [:public :mage] 1))
            v))) {} players))
            
(defn- set-player-hands [ plyrs mages artifacts ]
  (zipmap 
    plyrs 
    (map-indexed  
      (fn [id nm]
        (let [mstart (* id 2) astart (* id 8)]
          (-> playerdata
              (assoc :action :selectmage)
              (assoc-in [:private :mages]     (mapv #(assoc % :target? true) (subvec mages mstart (+ mstart 2))))
              (assoc-in [:private :artifacts] (subvec artifacts astart (+ astart 8))))))
      plyrs)))
      

(defn setup [ plyrs ]
  (let [mages (-> @data :mages shuffle)
        artifacts (-> @data :artifacts shuffle)
        monuments (-> @data :monuments shuffle)
        pops (-> @data :placesofpower)
        turnorder (shuffle plyrs)
        players (set-player-hands plyrs mages artifacts) ]
    (assoc gs 
      :status :setup
      :pops (map (fn [base] (rand-nth (filter #(= (:base %) base) pops))) (->> pops (map :base) frequencies keys))
      :monuments {
        :public (take 2 monuments)
        :secret (nthrest monuments 2)}
      :magicitems (:magicitems @data)
      :turnorder turnorder
      :p1 (first turnorder)
      :players (make-ai-choices players))))
      
(defn- start-game [ gs ]
  (-> gs
      (assoc :status :started)    ; Start State
      (assoc :players
        (reduce-kv 
          (fn [m k v]
            (let [artifacts (-> v :private :artifacts)]
              (-> m
                  (assoc k v)
                  (dissoc :action)
                  (assoc-in [k :public :mage] (->> v :private :mages (filter :selected) first)) ; Switch Selected Mage to Public
                  (assoc-in [k :private :artifacts] (take 3 artifacts))                         ; Draw 2 artifacts
                  (assoc-in [k :secret :artifacts] (nthrest artifacts 3))                       ; Artifact deck
          ))) {} (:players gs)))))
  
(defn selectmagicitem [ gamestate ?data uname ]
  (assoc gamestate
    :players (reduce-kv 
              (fn [m k v]
                (if (= k uname)
                  (assoc m k (assoc-in v [:public :magicitem] (:card ?data)))
                  (assoc m k v))) {} (:players gamestate))
    :magicitems (mapv 
                  #(if (= (:owner %) uname)
                       (dissoc % :owner)
                       (if (= (:name %) (-> ?data :card :name))
                            (assoc % :owner uname)
                            %)) (:magicitems gamestate))))
      
      
(defn- choose-magicitem [ gs choosing-player ]
  (assoc gs :players
    (reduce-kv
      (fn [m k v]
        (if (= k choosing-player)
            (assoc m k (assoc v :action :selectmagicitem))
            (assoc m k (dissoc v :action)))) {} (:players gs))))
            
(defn- choose-magicitem-ai [ gs choosing-player ]
  (selectmagicitem 
    gs
    {:card (->> gs :magicitems (filter #(nil? (:owner %))) first)}
    choosing-player))
            
(defn choosing-player [ gs ]
  (let [nomi (reduce-kv #(if (-> %3 :public :magicitem nil?) (conj %1 %2)) #{} (:players gs))]
    (->> (:turnorder gs)
         reverse
         (mapv #(if (contains? nomi %) % nil))
         (remove nil?)
         first)))
         
(defn check-start [ gs ]
  (let [nplayers (-> gs :players keys count)]
    (if (= nplayers (reduce + (map (fn [[k v]] (-> v :public :mage)) (:players gs))))             ; All players have selected a Mage
        (if (> nplayers (reduce + (map (fn [[k v]] (if (-> v :public :magicitem nil?) 0 1)) (:players gs))))  ; Not all players have selected a Magic Item
            (let [cp (choosing-player gs)]
              (if (some? (re-matches #"AI\d+" cp)) 
                  (check-start (choose-magicitem-ai gs cp))
                  (choose-magicitem gs cp)))
            (start-game gs))
    gs)))
                   
      
(defn selectstartmage [ gamestate ?data uname ]
; Only runs for game setup..
  (-> gamestate
    (assoc :players 
      (reduce-kv  
        (fn [m k v]
          (assoc m k
            (if (= k uname)
                (-> v 
                    (assoc-in [:private :mages] (map #(if (= (:name %) (:card ?data)) (assoc % :selected true :target? nil) (dissoc % :selected :target?)) (-> v :private :mages)))
                    (assoc-in [:public :mage] 1))
                v))) {} (:players gamestate)))
    check-start))
         
(defn selectstartitem [ gamestate ?data uname ]
  (-> (selectmagicitem gamestate ?data uname)
      check-start))    
    
(defn parseaction [ gamestate ?data uname ]
  (println "Action:" ?data)
  (case (:action ?data)
    :selectstartmage (selectstartmage gamestate ?data uname)
    :selectstartitem (selectstartitem gamestate ?data uname)
    :selectmagicitem (selectmagicitem gamestate ?data uname)
    gamestate))