(ns lobby.ramodel
  (:require 
    [lobby.radata :refer [data]]
    [lobby.ragames :refer [gs playerdata] :as ragames]))

(defonce verbose? false)


;;;;; FUNCTIONS ;;;;;

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

(defn- add-chat
  ([ gs msg uname ]
    (update-in gs [:chat] 
      conj (message-map msg uname)))
  ([ gs msg ] (add-chat gs msg nil)))

(defn- is-ai? [ uname ]
  (some? (re-matches #"AI\d+" uname)))

(defn exhausttoggle [gamestate {:keys [card]} uname ]
  (case (:type card)
    "magicitem" (assoc gamestate :magicitems (map #(if (= (:uid %) (:uid card)) (if (:exhausted %) (dissoc % :exhausted) (assoc % :exhausted true)) %) (:magicitems gamestate)))
    "mage"      (if (-> gamestate :players (get "p1") :public :mage :exhausted true?)
                    (update-in gamestate [:players uname :public :mage] dissoc :exhausted)
                    (assoc-in  gamestate [:players uname :public :mage :exhausted] true))
    "artifact"  (assoc-in gamestate [:players uname :public :artifacts]
                  (map #(if (= (:uid %) (:uid card)) (if (:exhausted %) (dissoc % :exhausted) (assoc % :exhausted true)) %) (-> gamestate :players (get uname) :public :artifacts)))
    gamestate))

(defn set-player-action [ gamestate p action ]
  (if (nil? p)
      gamestate
      (assoc-in gamestate [:players p :action] action)))

(defn- drawcard [ gamestate uname ]
  (let [artdeck (-> gamestate :players (get uname) :secret :artifacts)]
    (-> gamestate
        (update-in [:players uname :private :artifacts] conj (first artdeck))
        (assoc-in [:players uname :secret :artifacts] (rest artdeck))
        (add-chat "Draw 1 card." uname))))
 
(defn get-active-player [ gamestate ]
  (reduce-kv 
    (fn [m k v] 
      (if (contains? #{:play :selectmagicitem} (:action v)) k m)) nil (:players gamestate)))

;;;;; RESOURCES ;;;;; 

(defn amendresource [ gamestate {:keys [resources card] :as ?data} uname ] 
  (reduce-kv 
    (fn [m k v] 
      (update-in m [:players uname :public :resources k] + v)) 
    gamestate 
    resources))

(defn- remove-collect-resources [ gs card uname ]
  (case (:type card)
    "mage"      (update-in gs [:players uname :public :mage] dissoc :collect-resource)
    "magicitem" (assoc gs :magicitems (map #(if (= (:uid %) (:uid card)) (dissoc % :collect-resource) %) (:magicitems gs)))
    "artifact"  (assoc-in gs [:players uname :public :artifacts] (map #(if (= (:uid %) (:uid card)) (dissoc % :collect-resource) %) (-> gs :players (get uname) :public :artifacts)))
    gs))

(defn- collect-resource [ gamestate {:keys [resources card] :as ?data} uname ]
  (-> gamestate
      (amendresource ?data uname)
      (remove-collect-resources card uname)
      (add-chat 
        (str "collected " (clojure.string/join ", " (map #(str (val %) " " (-> % key name)) resources)) " from " (:name card))
        uname)))

(defn- generate-resources [ gamestate ]
  ; copy from :collect to :collect-resource
  (if verbose? (println "Generate Resources"))
    (-> gamestate
      (assoc :magicitems
        (map #(if (-> % :owner some?) (assoc % :collect-resource (:collect %)) %) (:magicitems gamestate)))
      (assoc :players
        (reduce-kv 
          (fn [m k v]
            (-> m
                (assoc-in [k :public :mage :collect-resource] (-> v :public :mage :collect)) ; mage
                (assoc-in [k :public :artifacts] (mapv #(assoc % :collect-resource (:collect %)) (-> v :public :artifacts)))   ; artifact / monument / pop
            )
          ) (:players gamestate) (:players gamestate))))) 

; AI Collect  

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
    (if verbose? (println "AI-Collect-Resources: ai-players" (keys ai-players)))
    (-> gamestate
      (ai-collect-resources-mage ai-players)
      (ai-collect-resources-magicitem ai-players)
      (ai-collect-resources-artifacts ai-players)
      (ai-collect-resources-done ai-players))
  ))
  
;;;;; DISCARD ;;;;;

(defn discardcard [ gs ?data uname ]
  (if verbose? (println uname "discard" (-> ?data :card :name) "resources" (:resources ?data)))
  (let [{:keys [resources card]} ?data]
    (if (= uname (get-active-player gs))
        (-> gs 
            (update-in [:players uname :public :discard] conj card)
            (assoc-in [:players uname :private :artifacts] (remove  #(= (:uid %) (:uid card)) (-> gs :players (get uname) :private :artifacts)))
            (amendresource ?data uname)
            (add-chat (str "Discard " (:name card)) uname)
            (add-chat (str "Gained " (clojure.string/join "," (map #(str (val %) " " (-> % key name clojure.string/capitalize)) resources))) uname)
            )
        gs)))

;;;;; PHASE TRANSITION ;;;;;

(defn- collect-phase [ gamestate ]
  (if verbose? "Collect phase")
  (-> gamestate
      (assoc :phase :collect)
      generate-resources
      (add-chat (str "Round " (:round gamestate)))
      (add-chat (str "Collect Phase"))
      ai-collect-resources))

(defn- determine-winner [] nil)

;;;;; ASSIGN MAGIC ITEMS ;;;;;       

(defn assignmagicitem [ gamestate ?data uname ]
  (if verbose? (println "assignmagicitem:" ?data uname))
    (-> gamestate
        (set-player-action uname :pass)
        (assoc :magicitems (mapv #(if (= (:owner %) uname)
                                      (dissoc % :owner)
                                      (if (and (-> % :owner nil?) (= (:uid %) (:card ?data)))
                                          (assoc % :owner uname)
                                          %) ) (:magicitems gamestate)))))

(defn- selectmagicitem [ gamestate ?data uname ]
  (let [newmi (->> gamestate :magicitems (filter #(= (:uid %) (:card ?data))) first)]
    (-> gamestate
        (assignmagicitem ?data uname)
        (update-in [:chat] conj (message-map (str "chose Magic Item:" (:name newmi)) (->  uname)))
        (drawcard uname))))
        
(defn ai-choose-magicitem [ gs uname ]
  (if verbose? (println "ai-choose-magicitem:" uname ))
  (assignmagicitem
    gs 
    {:card (->> gs :magicitems (filter #(-> % :owner nil?)) first :uid)}
    uname))

;;;;; NEXT TURN / ROUND ;;;;;

(defn- new-round-check [ gamestate ]
  (if verbose? (println "new round check:" (-> gamestate :plyr-to empty?)))
  (if (-> gamestate :plyr-to empty?)
      (-> gamestate
          (assoc :plyr-to (:pass-to gamestate))
          (assoc :display-to (:pass-to gamestate))
          (assoc :pass-to [])
          (assoc :players 
            (reduce-kv 
              (fn [m k v] 
                (assoc m k (assoc v :action :waiting))) {} (:players gamestate)))
          (set-player-action (-> gamestate :pass-to first) :play)
          (update :round inc)
          collect-phase)
      (add-chat gamestate "Start turn" (get-active-player gamestate))))

(defn end-action [ gamestate uname ]
  (let [nextplyrs (if (-> gamestate :players (get uname) :action (= :pass))
                      (:plyr-to gamestate)    
                      (->> gamestate :plyr-to (drop-while #(not= % uname)) rest))
        nextp     (if (empty? nextplyrs) (-> gamestate :plyr-to first) (first nextplyrs))]
    (if verbose? (println "End Action:" uname "nextplayer" nextp ))
    (-> gamestate
        (set-player-action uname (if (-> gamestate :plyr-to set (contains? uname)) :waiting :pass))
        (set-player-action nextp :play)
        (add-chat "End of turn" uname)
        new-round-check
        )))

(defn- pass [ gamestate uname ]
  (let [can-pass? (and (-> gamestate :phase (not= :collect)) (-> gamestate :players (get uname) :action (= :play)))
        nextplyrs (->> gamestate :plyr-to (repeat 2) (apply concat) (drop-while #(not= % uname)) vec)
        newto     (if (-> gamestate :pass-to empty?) 
                      (->> nextplyrs (take (-> gamestate :players keys count)) vec) 
                      (:pass-to gamestate))]
    (if verbose? (println "Pass:" uname "status/phase" (:status gamestate) (:phase gamestate) "can-pass?"  can-pass?))
    (if can-pass? 
        (-> gamestate                                     
          (set-player-action uname :selectmagicitem)
          (update-in [:chat] conj (message-map "passed." uname))
          (assoc-in [:plyr-to] (->> gamestate :plyr-to (remove #(= uname %)) vec))
          (assoc :pass-to newto))
        gamestate)))

;;;;; ACTIONS ;;;;;
;;; PLAY A CARD ;;
; Depends on amendresource
; depends on next player/round TODO
(defn playcard [ gamestate {:keys [card resources]} uname ]
  (-> gamestate 
    (assoc-in [:players uname :private :artifacts] (remove #(= (:uid %) (:uid card)) (-> gamestate :players (get uname) :private :artifacts)))
    (update-in [:players uname :public :artifacts] conj card)
    (add-chat (str "Played Artifact " (:name card)) uname)
    (amendresource {:resources (reduce-kv (fn [m k v] (assoc m k (* -1 v))) {} resources)} uname)
  ; next player
    ))

(defn ai-action [ gamestate ]
  ; TODO - Add some AI logic here
  (let [all-ai?    (= (-> gamestate :players keys count) (->> gamestate :players keys (map is-ai?) (remove false?) count))
        activeplyr (get-active-player gamestate)]
    (if verbose? (println "ai-action-check: all-ai?" all-ai? "activeplayer" activeplyr))
    (if (or (nil? activeplyr) all-ai? (-> gamestate :phase (= :collect))) ; prevent infinite loops during develoment
        gamestate 
        (if (is-ai? activeplyr)
            (-> gamestate (pass activeplyr) (ai-choose-magicitem activeplyr) (end-action activeplyr) ai-action )
            gamestate))))

;;;;; Transfer from Collect to Action Phase ;;;;;

(defn- collect-to-action-phase [ gamestate ]
  ; if all players are ready move to Action Phase
  ; Trigger ai-action
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
                      (assoc-in  [k :public :artifacts] (map #(dissoc % :collect-resource) (-> v :public :artifacts)))
                      )) (:players gamestate) (:players gamestate)))
            (assoc :magicitems (map #(dissoc % :collect-resource) (:magicitems gamestate)))
            (add-chat "Action Phase")
            ai-action)
        gamestate)))
  
(defn- collected [ gamestate uname ]
  (let [newstate? (-> gamestate :players (get uname) :collected? nil?)]
  ; toggle collected? status
      (if verbose? (println "collected" newstate? uname))
      (-> gamestate
          (update-in [:players uname] #(if newstate? (assoc % :collected? true) (dissoc % :collected?)))
          collect-to-action-phase)))

;;;;;;;;;; SETUP/START GAME ;;;;;;;;;;

(defn- choosing-player [ gs ]
  (->> gs 
      :plyr-to 
      (remove #(contains? (->> gs :magicitems (map :owner) (remove nil?) set) %)) 
      last))
      
(defn- start-game [ gs ]
  (-> gs
      (assoc :status :play)                                                               ; Start State
      (assoc :phase  :collect)                                                            ; Collect phase
      (assoc :players
        (reduce-kv 
          (fn [m k v]
            (let [artifacts (-> v :private :artifacts shuffle)]
              (-> m
                  (assoc-in [k :action] (if (= k (-> gs :plyr-to first)) :play :waiting))  ; first player
                  (assoc-in [k :private :artifacts] (take 3 artifacts))                    ; Draw 3 artifacts
                  (assoc-in [k :secret :artifacts] (nthrest artifacts 3))                  ; Artifact deck
          ))) (:players gs) (:players gs)))
      collect-phase))
          
(defn- reveal-all-mages [ gs ]
  (assoc gs :players
    (reduce-kv
      (fn [m k v]
        (if (-> v :private :mages some?)  ; called when all players have chosen a Mage and eaxh time a Magic Item is selected
            (-> m 
                (assoc-in  [k :public :mage]  (dissoc (->> v :private :mages (filter #(= (:uid %) (-> v :public :mage))) first) :target?))
                (update-in [k :private] dissoc :mages)
                (assoc-in  [k :action] :waiting))
            m)) 
        (:players gs) (:players gs))))
  
(defn- check-start [ gs ]
  (let [nplayers (-> gs :players keys count)
        nmages   (->> gs :players (reduce-kv (fn [m k v] (if (-> v :public :mage some?) (inc m) m)) 0))
        nitems   (->> gs :magicitems (filter :owner) count)]
    (if verbose? (println "Check Start" (-> gs :players keys) nplayers nmages nitems))
    (if (= nplayers nitems)
        (start-game gs)
        (if (= nplayers nmages)
            (let [cp  (choosing-player gs)
                  ngs (reveal-all-mages gs)] ; reveal mages
              (if (is-ai? cp)
                  (-> ngs 
                      (ai-choose-magicitem cp)
                      check-start)
                  (assoc-in ngs [:players cp :action] :selectstartitem)))
            gs))))
            
(defn- set-selected-mage [ v carduid ]
  (if (contains? (->> v :private :mages (map :uid) set) carduid)
    (-> v 
      (assoc-in [:public :mage] carduid)
      (assoc :action :ready))
    v))
    
(defn- ai-choose-mage [ players ]
  (reduce-kv  
    (fn [m k v]
      (assoc m k
        (if (some? (re-matches #"AI\d+" k))
          (case (:action v)
            :selectmage (set-selected-mage v (-> v :private :mages first :uid))
            v)
          v))) {} players))

(defn- selectmage [ gamestate ?data uname ]
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
  
(defn- selectstartitem [ gamestate ?data uname ]
  (-> gamestate
    (assignmagicitem ?data uname)
    (update-in [:chat] conj (message-map "selected starting item." uname))
    check-start))  

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
        players   (set-player-hands plyrs mages artifacts) 
        turnorder (-> plyrs shuffle)] ;(apply sorted-set (shuffle plyrs)) ]
    (-> gs 
      (assoc  :status :setup
              :pops       (map (fn [base] (rand-nth (filter #(= (:base %) base) pops))) (->> pops (map :base) frequencies keys))
              :allcards   ragames/get-all-cards
              :monuments {
                :public (take 2 monuments)
                :secret (nthrest monuments 2)}
              :magicitems (-> @data :magicitems (add-uid "itm"))
              :plyr-to    turnorder
              :display-to turnorder
              :players    (ai-choose-mage players))
              ;:chat       (->> turnorder (filter is-ai?) (mapv #(message-map "selected starting mage" %)))
      check-start)))

;; Round
;; - Collect Essence
;; - Do Actions
;; - Check VPs.

;;;;;;;;;; ACTION HANDLER ;;;;;;;;;;

;;;;; Parse Action Error Handlers ;;;;;
(defn- can-place-card [ gs {:keys [card]} uname]
; Conditions / Codes
; - 1  player action is play
; - 2  Artifact card is in hand
; - 4  Place of Power card is in pops
; - 8  Monument is in Monument pool
; - 16 Not an Artifact, Pop, or Monument
  (let [pdata (-> gs :players (get uname))]
    (apply + [
      (if (= :play (:action pdata)) 0 1)
      (case (:type card)
            "artifact" (if (contains? (->> pdata :private :artifacts (map :uid) set) (:uid card)) 0 2)
            "monument" (if (contains? (->> gs :monuments :public (map :uid) set) (:uid card))     0 4) 
            "pop"      (if (contains? (->> gs :pops (map :uid) set) (:uid card))                  0 8)
            16) 
    ])))

; PLACE an artifact
; CLAIM a place of power or Monument
; DISCARD a card for 1g or 2other
; USE a power
; PASS - exchange magic items and draw 1 - first to pass becomes 1st player.


;; TODO Add tests for actions before triggering the thread.
(defn parseaction [ gamestate ?data fromname ]
  (let [uname (:uname ?data fromname)]
    (if verbose? (println "ACTION:" ?data uname))
    (case (:action ?data)
      :selectmage       (selectmage       gamestate ?data uname)
      :selectstartitem  (selectstartitem  gamestate ?data uname)
      :selectmagicitem  (-> gamestate 
                            (selectmagicitem ?data uname)
                            (end-action uname)
                            ai-action)
      :collect-resource (collect-resource gamestate ?data uname)
      :collected        (collected        gamestate uname)
    ; PLACE
    ; CLAIM
      :place            (let [err (can-place-card gamestate ?data uname)]
                          (if (= err 0)
                              (-> gamestate 
                                  (playcard ?data uname)
                                  (end-action uname)
                                  ai-action)
                              (assoc-in gamestate [:players uname :err] err)))
    ; DISCARD
      :discard          (-> gamestate 
                            (discardcard ?data uname)
                            (end-action uname)
                            ai-action)
    ; USE
    ;  :exhausttoggle    (exhausttoggle    gamestate ?data uname)
    ; PASS
      :pass             (pass             gamestate uname)
    ; Done - Only used for Testing
      :done             (end-action       gamestate uname)
    ; TESTING ONLY
      :swapgame         ragames/game1
      gamestate)))

(defn- res-match-handler [ gs uname func & args ]
  (println "Chat Handler" func args)
  (-> gs 
      (assoc-in [:players uname :public :resources (-> args first clojure.string/lower-case keyword)] (-> args second read-string))
      (add-chat (str "Set " (first args) " to " (second args)) uname)
      ))

(def chat-fn-help {
  "resource" "/resource <resource name> <new value>"
})

(defn chat-handler [ gs msg uname ]
  (let [gs-w-cmd (add-chat gs msg uname)
        res-fn-hint (re-find #"(?i)\/(\w+)" msg)
        res-match (re-matches #"(?i)\/(\w+)\s(gold|elan|calm|life|death)\s(\d+)" msg)]
    (prn "Chat Handler" msg res-match)
    (cond
      res-match (apply res-match-handler gs-w-cmd uname (rest res-match))
      res-fn-hint (add-chat gs-w-cmd (str "help: " (-> res-fn-hint last clojure.string/lower-case chat-fn-help)) uname)
      :default gs-w-cmd)))
;; Player States :action
; :waiting
; :play
; :selectmagicitem
; :reaction?