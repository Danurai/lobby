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
                (assoc-in [k :public] (:public v))
                (assoc-in [k :private] 
                  (if (= k plyr)
                      (:private v)
                      (reduce-kv #(assoc %1 %2 (count %3)) {} (:private v))))
                (assoc-in [k :secret] (reduce-kv #(assoc %1 %2 (count %3)) {} (:secret v))))
          ) (:players state) (:players state)))))
               
(defn- message-map [ msg uname ]
  (hash-map 
    :msg msg
    :uname uname 
    :timestamp (new java.util.Date)))

(defn add-chat 
  ([ gs msg uname ]
    (update-in gs [:chat] 
      conj (message-map msg uname)))
  ([ gs msg ] (add-chat gs msg nil)))

(defn- is-ai? [ uname ]
  (some? (re-matches #"AI\d+" uname)))

(defn amendresource [ gamestate {:keys [resources card] :as ?data} uname ] 
  (-> (reduce-kv (fn [m k v] (update-in m [:players uname :public :resources k] + v)) gamestate resources)
      (add-chat 
        (if (some? card) 
            (str "collected " (clojure.string/join ", " (map #(str (val %) " " (-> % key name)) resources)) " from " (:name card))
            (str "set " (clojure.string/join ", " (map #(str (-> % name clojure.string/capitalize) " to " (-> gs :players (get uname) :public :resources %)) (keys resources)))))
        uname)))

(defn- remove-collect-resources [ gs card uname ]
  (case (:type card)
    "mage" (update-in gs [:players uname :public :mage] dissoc :collect-resource)
    "magicitem" (assoc gs :magicitems (map #(if (= (:uid %) (:uid card)) (dissoc % :collect-resource) %) (:magicitems gs)))
    "artifact"  (assoc-in gs [:players uname :public :artifacts] (map #(if (= (:uid %) (:uid card)) (dissoc % :collect-resource) %) (-> gs :players (get uname) :public :artifacts)))
    gs))

(defn collect-resource [ gamestate {:keys [resources card] :as ?data} uname ]
  (-> gamestate
      (amendresource ?data uname)
      (remove-collect-resources card uname)))
      
(defn playcard [ gamestate {:keys [card resources]} uname ]
  (-> gamestate 
    (assoc-in [:players uname :private :artifacts] (remove #(= (:uid %) (:uid card)) (-> gamestate :players (get uname) :private :artifacts)))
    (assoc-in [:players uname :public :artifacts] (apply conj (-> gamestate :players (get uname) :public :artifacts) [card]))
    (update-in [:chat] conj (message-map (str "Played Artifact " (:name card)) uname))
    (amendresource {:resources (reduce-kv (fn [m k v] (assoc m k (* -1 v))) {} resources)} uname)
    ))
    
    
(defn set-active [ gamestate p action ]
  (-> gamestate
      (assoc-in  [:players p :action] action)))
    
(defn done [ gamestate uname ]
  (let [nextplyrs (->> gamestate :plyr-to (drop-while #(not= % uname)) rest)
        nextp     (if (empty? nextplyrs) (-> gamestate :plyr-to first) (first nextplyrs))]
    (-> gamestate
        (set-active uname :waiting)
        (update-in [:chat] conj (message-map "end of turn." uname))
        (set-active nextp :play)
        (update-in [:chat] conj (message-map "start turn." nextp)))))
        
        
(defn- generate-resources [ gamestate ]
; copy from :collect to :collect-resource
  (-> gamestate
    (assoc :magicitems
      (map #(if (-> % :owner some?) (assoc % :collect-resource (:collect %)) %) (:magicitems gamestate)))
    (assoc :players
      (reduce-kv 
        (fn [m k v] 
          (-> m
             (assoc-in [k :public :mage :collect-resource] (-> v :public :mage :collect)) ; mage
                                                                                          ; artifact / monument / pop
          )
        ) (:players gamestate) (:players gamestate))))) 

(defn- ai-collect-resources-artifacts [ gamestate ai-players ]
  (reduce-kv
    (fn [gs uname pdata]
      (reduce 
        (fn [gs artifact]
          (if-let [resources (:collect-resource artifact)]
            (collect-resource gs {:resources (first resources) :card artifact} uname)
            gs))
        gs
        (-> pdata :public :artifacts))
    )
    gamestate
    ai-players))

(defn- ai-collect-resources-mage [ gamestate ai-players ]
  (reduce-kv
    (fn [gs uname pdata]
      (let [mage (-> gs :players (get uname) :public :mage)]
        (if-let [resources (:collect-resource mage)]
          (collect-resource gs {:resources (first resources) :card mage} uname)
          gs)
    ))
    gamestate
    ai-players))

(defn- ai-collect-resources-magicitem [ gamestate ai-players ]
  (reduce-kv
    (fn [gs uname pdata]
      (let [magicitem (->> gs :magicitems (filter #(= (:owner %) uname)) first)]
        (if-let [resources (:collect-resource magicitem)]
          (collect-resource gs {:resources (first resources) :card magicitem} uname)
          gs)
    ))
    gamestate
    ai-players))

(defn- ai-collect-resources-done [ gamestate ai-players ]
  (reduce-kv
    (fn [m k v]
      (assoc-in m [:players k :collected?] true))
    gamestate
    ai-players))

(defn ai-collect-resources [ gamestate ]
  ; Take first available resource
  ; mage, magic item, artifact / monument / pop
  (let [ai-players (select-keys (:players gamestate) (filter is-ai? (-> gamestate :players keys)))]
    (-> gamestate
      (ai-collect-resources-mage ai-players)
      (ai-collect-resources-magicitem ai-players)
      (ai-collect-resources-artifacts ai-players)
      (ai-collect-resources-done ai-players))
  ))
          
(defn- collect-phase [ gamestate ]
  (-> gamestate
      generate-resources
      ai-collect-resources
      ))

(defn- newround [ gamestate ]
  (-> gamestate
      (assoc :plyr-to (:pass-to gamestate))
      (assoc :pass-to [])
      (assoc :players 
        (reduce-kv 
          (fn [m k v] 
            (assoc m k (assoc v :action :waiting))) {} (:players gamestate)))
      (set-active (-> gamestate :pass-to first) :play)
      collect-phase))
      
      
(defn- drawcard [ gamestate uname ]
  (let [artdeck (-> gamestate :players (get uname) :secret :artifacts)]
    ;(prn (-> gamestate :players (get uname) :private :artifacts (conj (first artdeck)) count) )
    (-> gamestate
        (update-in [:players uname :private :artifacts] conj (first artdeck))
        (assoc-in [:players uname :secret :artifacts] (rest artdeck))
        (update-in [:chat] conj (message-map "drew 1 card." uname)))))
      
(defn assignmagicitem [ gamestate ?data uname ]
  (let [gs (assoc gamestate
              :players (reduce-kv 
                        (fn [m k v]
                          (if (= k uname)
                            (assoc m k (assoc v :action :pass))
                            (if (= (:action v) :waitingforpass)
                              (assoc m k (assoc v :action :play))
                              (assoc m k v)))) {} (:players gamestate))
              :magicitems (mapv 
                            #(if (some? (:owner %))
                                 (if (= (:owner %) uname)
                                    (dissoc % :owner)
                                    %)
                                 (if (= (:uid %) (:card ?data))
                                      (assoc % :owner uname)
                                      %)) (:magicitems gamestate)))]
    (if (-> gamestate :plyr-to empty?)
        (newround gs)
        gs)))

(defn selectmagicitem [ gamestate ?data uname ]
  (let [newmi (->> gamestate :magicitems (filter #(= (:uid %) (:card ?data))) first)]
    (-> gamestate
        (assignmagicitem ?data uname)
        (update-in [:chat] conj (message-map (str "took " (:name newmi)) (->  uname)))
        (drawcard uname))))
        
(defn ai-choose-magicitem [ gs uname ]
  (assignmagicitem
    gs 
    {:card (->> gs :magicitems (filter #(-> % :owner nil?)) first :uid)}
    uname))
        
(defn pass [ gamestate uname ]
  (let [nextplyrs (->> gamestate :plyr-to (repeat 2) (apply concat) (drop-while #(not= % uname)) vec)
        nextp     (-> nextplyrs rest first)
        nextpaction (-> gamestate :players (get nextp) :action)
        newto     (if (-> gamestate :pass-to empty?) (->> nextplyrs (take (-> gamestate :players keys count)) vec) (:pass-to gamestate))
    ]
    (-> gamestate
        (set-active nextp (if (= :waiting nextpaction) :waitingforpass nextpaction))
        (set-active uname :selectmagicitem)
        (update-in [:chat] conj (message-map "passed." uname))
        (assoc-in [:plyr-to] (->> gamestate :plyr-to (remove #(= uname %)) vec))
        (assoc :pass-to newto))))
  
(defn exhausttoggle [gamestate {:keys [card]} uname ]
  (case (:type card)
    "magicitem" (assoc gamestate :magicitems (map #(if (= (:uid %) (:uid card)) (if (:exhausted %) (dissoc % :exhausted) (assoc % :exhausted true)) %) (:magicitems gamestate)))
    "mage"      (if (-> gamestate :players (get "p1") :public :mage :exhausted true?)
                    (update-in gamestate [:players uname :public :mage] dissoc :exhausted)
                    (assoc-in  gamestate [:players uname :public :mage :exhausted] true))
    "artifact"  (assoc-in gamestate [:players uname :public :artifacts]
                  (map #(if (= (:uid %) (:uid card)) (if (:exhausted %) (dissoc % :exhausted) (assoc % :exhausted true)) %) (-> gamestate :players (get uname) :public :artifacts)))
    gamestate))
    
 

                            

          
(defn choosing-player [ gs ]
  (->> gs 
      :plyr-to 
      (remove #(contains? (->> gs :magicitems (map :owner) (remove nil?) set) %)) 
      last))
      
(defn- start-game [ gs ]
  (-> gs
      (assoc :status :play)    ; Start State
      (assoc :phase  :collect)
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
      collect-phase))
          
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
    (update-in [:chat] conj (message-map "selected starting mage." uname))
    check-start))
  
(defn selectstartitem [ gamestate ?data uname ]
  (-> gamestate
    (assignmagicitem ?data uname)
    (update-in [:chat] conj (message-map "selected starting item." uname))
    check-start))  

(defn- collect-to-action-phase [ gamestate ]
  (let [pcount (-> gamestate :players keys count)
        ccount (->> gamestate :players vals (map :collected?) (remove nil?) count)]
      (if (= pcount ccount)
        (-> gamestate
            (assoc :phase :action)
            (assoc :players 
              (reduce-kv 
                (fn [ m k v ] 
                  (-> m 
                      (update-in [k] dissoc :collected?) 
                      (update-in [k :public :mage] dissoc :collect-resource)
                      (assoc-in [k :public :artifacts] (map #(dissoc % :collect-resource) (:artifacts v)))
                      )) (:players gamestate) (:players gamestate)))
            (assoc :magicitems (map #(dissoc % :collect-resource) (:magicitems gamestate)))
            (add-chat "Action Phase"))
        gamestate)))
  
(defn- collected [ gamestate uname ]
  (let [newstate? (-> gamestate :players (get uname) :collected? nil?)]
  ; toggle collected? status, 
  ; if all players are ready move to Action Phase
  ; Trigger ai-action
      (-> gamestate
          (update-in [:players uname] #(if newstate? (assoc % :collected? true) (dissoc % :collected?)))
          collect-to-action-phase)))
          
(defn parseaction [ gamestate ?data fromname ]
  (let [uname (:uname ?data fromname)]
    (println ?data uname)
    ;(println gamestate)
    (case (:action ?data)
      :selectmage       (selectmage       gamestate ?data uname)
      :selectstartitem  (selectstartitem  gamestate ?data uname)
      :selectmagicitem  (selectmagicitem  gamestate ?data uname)
      :collect-resource (collect-resource gamestate ?data uname)
      :amendresource    (amendresource    gamestate ?data uname)
      :playcard         (playcard         gamestate ?data uname)
      :pass             (pass             gamestate uname)
      :done             (done             gamestate uname)
      :collected        (collected        gamestate uname)
      :exhausttoggle    (exhausttoggle    gamestate ?data uname)
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
            :plyr-to (-> plyrs shuffle) ;(apply sorted-set (shuffle plyrs)) 
            :players (ai-choose-mage players))
      check-start)))