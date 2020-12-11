(ns lobby.ramodel
  (:require 
    [lobby.radata :refer [data]]))
  
;(defonce gamestate (atom {}))
(def gs {
  :status :setup
  :pops []
  :monuments []
  :magicitems (:magicitems @data)
  :players {}
  :plyr-to []
  :pass-to []
})
  
(def playerdata {
  :public {
    :artifacts []
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
          
          

     
(defn- message-map [ msg uname ]
  (hash-map 
    :msg msg
    :uname uname 
    :timestamp (new java.util.Date)))
     
(defn amendresource [ gamestate {:keys [resources card] :as ?data} uname ]
  (prn card)
  (let [gs (reduce-kv 
            (fn [m k v] 
              (update-in m [:players uname :public :resources k] + v)) gamestate resources)]
    (update-in gs [:chat] 
      conj (message-map 
              (if (some? card) 
                  (str "Collect " (clojure.string/join ", " (map #(str (val %) " " (-> % key name)) resources)) " from " (:name card))
                  (str "Set " (clojure.string/join ", " (map #(str (-> % name clojure.string/capitalize) " to " (-> gs :players (get uname) :public :resources %)) (keys resources)))))
                  uname))))
           
(defn- remove-collect-resources [ pdata card ]
  (case (:type card)
    "mage" (update-in pdata [:mage] dissoc :collect-resource)
    "artifact" (assoc pdata :artifacts (map #(if (= (:uid %) (:uid card)) (dissoc % :collect-resource) %) (:artifacts pdata)))
    pdata))
           
(defn collect-resource [ gamestate {:keys [resources card] :as ?data} uname ]
  (-> gamestate
      (assoc-in [:players uname :public] (remove-collect-resources (-> gamestate :players (get uname) :public) card))
      (amendresource ?data uname)
      (assoc :magicitems (map #(if (= (:uid %) (:uid card)) (dissoc % :collect-resource) %) (:magicitems gamestate)))
      ))
      
(defn playcard [ gamestate {:keys [card resources]} uname ]
  (-> gamestate 
    (assoc-in [:players uname :private :artifacts] (remove #(= (:uid %) (:uid card)) (-> gamestate :players (get uname) :private :artifacts)))
    (assoc-in [:players uname :public :artifacts] (apply conj (-> gamestate :players (get uname) :public :artifacts) [card]))
    (update-in [:chat] conj (message-map (str "Played Artifact " (:name card)) uname))
    (amendresource {:resources (reduce-kv (fn [m k v] (assoc m k (* -1 v))) {} resources)} uname)
    ))
    
;(defn- removetarget [ coll ]
;  (map #(dissoc % :target?) coll))
;  
;(defn clearalltargets [ gamestate ]
;  (-> gamestate 
;      (assoc :magicitems (->> gamestate :magicitems removetarget))
;      (assoc :players 
;        (reduce-kv
;          (fn [m k v]
;            (-> m
;                (assoc k v)
;                (update-in [:public :mage] dissoc :target?)
;                (assoc-in [:public :artifacts] (-> v :public :artifacts removetarget))
;                (assoc-in [:private :artifacts] (-> v :private :artifacts removetarget))))
;          {} (:players gamestate)))))
;  
;(defn set-targets [ gamestate p action ]
;  (let [gs (clearalltargets gamestate)]
;    (case action
;      [:selectmagicitem :selectstartitem] 
;        (assoc ga :magicitems (->> gs :magicitems (map #(assoc % target? true))))
;      :play ga 
;      gs  )))
    
(defn set-active [ gamestate p action ]
  (-> gamestate
      (assoc-in  [:players p :action] action)))
    
(defn done [ gamestate uname ]
  (let [nextplyrs (->> gamestate :plyr-to (drop-while #(not= % uname)) rest)
        nextp     (if (empty? nextplyrs) (-> gamestate :plyr-to first) (first nextplyrs))]
    (-> gamestate
        (set-active uname :waiting)
        (set-active nextp :play))))
        
        
(defn- place-resources [ gamestate ]
  (-> gamestate
    (assoc :magicitems
      (map #(if (-> % :owner some?) (assoc % :collect-resource (:collect %)) %) (:magicitems gamestate)))
    (assoc :players
      (reduce-kv 
        (fn [m k v] 
          (-> m
             (assoc k v)
             (assoc-in [k :public :mage :collect-resource] (-> v :public :mage :collect)))) {} (:players gamestate)))))
          
(defn- newround [ gamestate ]
  (-> gamestate
      (assoc :plyr-to (:pass-to gamestate))
      (assoc :pass-to [])
      (assoc :players 
        (reduce-kv 
          (fn [m k v] 
            (assoc m k (assoc v :action :waiting))) {} (:players gamestate)))
      (set-active (-> gamestate :pass-to first) :play)
      place-resources))
      
      
        
(defn pass [ gamestate uname ]
  (let [nextplyrs (->> gamestate :plyr-to (repeat 2) (apply concat) (drop-while #(not= % uname)) vec)
        nextp     (-> nextplyrs rest first)
        newto     (->> nextplyrs (take (-> gamestate :players keys count)) vec)
    ]
    (if (-> gamestate :plyr-to (= [uname]))
        (newround gamestate)
        (-> gamestate
            (set-active uname :pass)
            (set-active nextp :play)
            (assoc-in [:plyr-to] (->> gamestate :plyr-to (remove #(= uname %)) vec))
            (assoc :pass-to newto)))))
      
(defn exhausttoggle [gamestate {:keys [card]} uname ]
  (case (:type card)
    "magicitem" (assoc gamestate :magicitems (map #(if (= (:uid %) (:uid card)) (if (:exhausted %) (dissoc % :exhausted) (assoc % :exhausted true)) %) (:magicitems gamestate)))
    "mage"      (if (-> gamestate :players (get "p1") :public :mage :exhausted true?)
                    (update-in gamestate [:players uname :public :mage] dissoc :exhausted)
                    (assoc-in  gamestate [:players uname :public :mage :exhausted] true))
    "artifact"  (assoc-in gamestate [:players uname :public :artifacts]
                  (map #(if (= (:uid %) (:uid card)) (if (:exhausted %) (dissoc % :exhausted) (assoc % :exhausted true)) %) (-> gamestate :players (get uname) :public :artifacts)))
    gamestate))
    
 

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
      :plyr-to 
      (remove #(contains? (->> gs :magicitems (map :owner) (remove nil?) set) %)) 
      last))
      
(defn- start-game [ gs ]
  (-> gs
      (assoc :status :started)    ; Start State
      (assoc :players
        (reduce-kv 
          (fn [m k v]
            (let [artifacts (-> v :private :artifacts)]
              (-> m
                  (assoc k v)
                  (assoc-in [k :action] (if (= k (-> gs :plyr-to first)) :play :waiting))
                  (assoc-in [k :public :mage] (->> v :private :mages (filter #(= (:uid %) (-> v :public :mage))) first)) ; Switch Selected Mage to Public
                  (update-in [k :private] dissoc :mages)                                                                 ; Remove Mages
                  (assoc-in [k :private :artifacts] (take 3 artifacts))                                                  ; Draw 3 artifacts
                  (assoc-in [k :secret :artifacts] (nthrest artifacts 3))                                                ; Artifact deck
          ))) {} (:players gs)))
      place-resources))
          
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
     check-start))  
  
(defn parseaction [ gamestate ?data fromname ]
  (let [uname (:uname ?data fromname)]
    (println ?data uname)
    (case (:action ?data)
      :selectmage       (selectmage       gamestate ?data uname)
      :selectstartitem  (selectstartitem  gamestate ?data uname)
      :collect-resource (collect-resource gamestate ?data uname)
      :amendresource    (amendresource    gamestate ?data uname)
      :playcard         (playcard         gamestate ?data uname)
      :pass             (pass             gamestate uname)
      :done             (done             gamestate uname)
      :exhausttoggle    (exhausttoggle    gamestate ?data uname)
      ;:selectmagicitem (selectmagicitem gamestate ?data uname)
      gamestate)))
    
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
            :plyr-to (apply sorted-set (shuffle plyrs)) 
            :players (ai-choose-mage players))
      check-start)))