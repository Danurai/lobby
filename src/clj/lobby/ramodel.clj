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
 
(defn- start-game [ gs ]
  (-> gs
      (assoc :status :started)    ; Start State
      (assoc :players
        (reduce-kv 
          (fn [m k v]
            (let [artifacts (-> v :private :artifacts)]
              (-> m
                  (assoc k v)
                  (assoc-in [k :action] (if (= k (-> gs :turnorder first)) :play :waiting))
                  (assoc-in [k :public :mage] (->> v :private :mages (filter #(= (:uid %) (-> v :public :mage))) first)) ; Switch Selected Mage to Public
                  (update-in [k :private] dissoc :mages)                                                                 ; Remove Mages
                  (assoc-in [k :private :artifacts] (take 3 artifacts))                                                  ; Draw 3 artifacts
                  (assoc-in [k :secret :artifacts] (nthrest artifacts 3))                                                ; Artifact deck
          ))) {} (:players gs)))))


(defn- selectmagicitem [ gamestate ?data uname ]
  (assoc gamestate
    :players (reduce-kv 
              (fn [m k v]
                (if (= k uname)
                  (assoc m k (assoc v :action :pass))
                  (assoc m k v))) {} (:players gamestate))
    :magicitems (mapv 
                  #(if (= (:owner %) uname)
                       (dissoc % :owner)
                       (if (= (:uid %) (:card ?data))
                            (assoc % :owner uname)
                            %)) (:magicitems gamestate))))
                            

(defn ai-choose-magicitem [ gs uname ]
  (selectmagicitem
    gs 
    {:card (->> gs :magicitems (filter #(-> % :owner nil?)) first :uid)}
    uname))
          
(defn choosing-player [ gs ]
  (->> gs 
      :turnorder 
      (remove #(contains? (->> gs :magicitems (map :owner) (remove nil?) set) %)) 
      last))
         
(defn check-start [ gs ]
  (let [nplayers (-> gs :players keys count)
        nmages   (->> gs :players (reduce-kv (fn [m k v] (if (-> v :public :mage some?) (inc m) m)) 0))
        nitems   (->> gs :magicitems (filter :owner) count)]
    ;(prn nplayers nmages nitems)
    (if (= nplayers nitems)
        (start-game gs)
        (if (= nplayers nmages)
            (let [cp (choosing-player gs)]
              (if (some? (re-matches #"AI\d+" cp))
                  (-> gs 
                      (ai-choose-magicitem cp)
                      check-start)
                  (assoc-in gs [:players cp :action] :selectstartitem)))
            gs))))
            
(defn- set-selected-mage [ v cardid ]
  (if (contains? (->> v :private :mages (map :uid) set) cardid)
    (-> v 
      (assoc-in [:public :mage] cardid)
      (assoc-in [:private :mages] (->> v :private :mages (map #(dissoc % :target?))))
      (assoc :action :waiting))
    v))
    
(defn- ai-choose-mage [ players ]
;; TODO push out reduce-kv loop?
  (reduce-kv  
    (fn [m k v]
      (assoc m k
        (if (some? (re-matches #"AI\d+" k))
          (case (:action v)
            :selectmage (set-selected-mage v (-> v :private :mages first :uid))
            v)
          v))) {} players))
          
            
(defn selectmage [ gamestate ?data uname ]
; Only runs for game setup
; ?data = {:card <id>}
  (-> gamestate
    (assoc :players 
      (reduce-kv  
        (fn [m k v]
          (assoc m k
            (if (= k uname)
                (set-selected-mage v (:card ?data))
                v))) {} (:players gamestate)))
    check-start))
  
(defn selectstartitem [ gamestate ?data uname ]
  (-> gamestate
     (selectmagicitem ?data uname)
     check-start
     ))

(defn parseaction [ gamestate ?data uname ]
  (println ?data uname)
  (case (:action ?data)
    :selectmage (selectmage gamestate ?data uname)
    :selectstartitem (selectstartitem gamestate ?data uname)
    :selectmagicitem (selectmagicitem gamestate ?data uname)
    gamestate))

    
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
      
(defn- add-uid [ coll t ]
  (map #(assoc % :uid (-> t gensym keyword)) coll))
      
(defn setup [ plyrs ]
  (let [mages     (-> @data :mages         (add-uid "mag") shuffle)
        artifacts (-> @data :artifacts     (add-uid "art") shuffle)
        monuments (-> @data :monuments     (add-uid "mon") shuffle)
        pops      (-> @data :placesofpower (add-uid "pop") shuffle)
        players   (set-player-hands plyrs mages artifacts) ]
    (-> gs 
      (assoc :status :setup
            :pops (map (fn [base] (rand-nth (filter #(= (:base %) base) pops))) (->> pops (map :base) frequencies keys))
            :monuments {
              :public (take 2 monuments)
              :secret (nthrest monuments 2)}
            :magicitems (-> @data :magicitems (add-uid "itm"))
            :turnorder (shuffle plyrs)
            :players (ai-choose-mage players))
      check-start)))