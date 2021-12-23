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
               
(defn- message-map 
  ([ msg uname event]
    (hash-map 
      :msg msg
      :uname uname 
      :event event
      :timestamp (new java.util.Date)))
  ([ msg uname ] (message-map msg uname nil)))

(defn- add-chat
  ([ gs msg uname event ]
    (update-in gs [:chat] 
      conj (message-map msg uname event)))
  ([ gs msg uname ] (add-chat gs msg uname nil))
  ([ gs msg ] (add-chat gs msg nil nil)))

(defn- is-ai? [ uname ]
  (some? (re-matches #"AI\d+" uname)))

(defn set-player-action [ gamestate p action ]
  (if (nil? p)
      gamestate
      (assoc-in gamestate [:players p :action] action)))

(defn- drawcard 
  ([ gamestate uname n]
    (if verbose? (println "drawcard:" uname n))
    (if n 
      (let [artdeck (-> gamestate :players (get uname) :secret :artifacts)]
        (-> gamestate
            (update-in [:players uname :private :artifacts] #(apply conj % (take n artdeck)))
            (assoc-in [:players uname :secret :artifacts] (nthrest artdeck n))
            (add-chat "Draw 1 card." uname)))
      gamestate))
  ([ gamestate uname ] (drawcard gamestate uname 1)))
 
(defn get-active-player [ gamestate ]
  (reduce-kv 
    (fn [m k v] 
      (if (contains? #{:play :selectmagicitem} (:action v)) k m)) nil (:players gamestate)))

(defn- invert-essence [ essence ]
  (reduce-kv (fn [m k v] (assoc m k (* -1 v))) {} essence))

;;; Turn Card
(defn- apply-turn [ card turn? ]  
  (if verbose? (println "apply-turn:" card turn? ))
  (if turn? (assoc card :turned? true) (dissoc card :turned?)))

(defn- turn-card [ gamestate card turn? uname]
  (if verbose? (println "turn-card:" uname card turn? ))
  (case (:type card)
    "mage"
      (update-in gamestate [:players uname :public :mage] #(apply-turn % turn?)) ; regardless of card id
    ("artifact" "pop" "monument")
      (assoc-in  gamestate [:players uname :public :artifacts] (mapv #(if (= (:uid %) (:uid card)) (apply-turn % turn?) %) (-> gamestate :players (get uname) :public :artifacts) )) 
    "magicitem"
      (assoc     gamestate :magicitems (map #(if (= (:uid %) (:uid card)) (apply-turn % turn?) %) (:magicitems gamestate)))
    gamestate))

;;; VP Functions ;;;;
(defn- vpfn-essence [ take-essence essence ]
  (if take-essence
      (essence take-essence 0)
      0))
(defn- vpfn [ card ]
  (let [te (:take-essence card)]
    (case (:name card)
      "Alchemist's Tower"     (vpfn-essence te :gold)
      "Dwarven Mines"         (vpfn-essence te :gold)
      "Catacombs of the Dead" (vpfn-essence te :death)
      "Cursed Forge"          (+ 1 (vpfn-essence te :gold))
      "Dragon's Lair"         (vpfn-essence te :gold)
      "Sacred Grove"          (+ 2 (vpfn-essence te :life))
      "Sacrificial Pit"       (+ 2 (vpfn-essence te :death))
      "Sunken Reef"           (vpfn-essence te :calm)
      "Sorcerer's Bestiary"   0
      (:vp card) ; mostly monuments and artifacts
    )))

;;;;; ESSENCE ;;;;; 

(defn update-player-essence [ gamestate {:keys [essence] :as ?data} uname ] 
  (if verbose? (println "update-player-essence:" essence uname))
  (reduce-kv 
    (fn [m k v] 
      (update-in m [:players uname :public :essence k] + v)) 
    gamestate 
    essence))

(defn- remove-card-essence [ gs card attr uname ]
  (case (:type card)
    "mage"      (update-in gs [:players uname :public :mage] dissoc attr)
    "magicitem" (assoc gs :magicitems (map #(if (= (:uid %) (:uid card)) (dissoc % attr) %) (:magicitems gs)))
    ("artifact" "monument" "pop")
                (assoc-in gs [:players uname :public :artifacts] (map #(if (= (:uid %) (:uid card)) (dissoc % attr) %) (-> gs :players (get uname) :public :artifacts)))
    gs))

(defn- collect-essence [ gamestate {:keys [essence card] :as ?data} uname ]
  ;(println "collect-essence:" essence card uname)
  (if (:turn essence)
      (-> gamestate
          (turn-card card true uname)
          (remove-card-essence card :collect-essence uname)
          (add-chat (str "Turned " (:name card)) uname))
      (-> gamestate
          (update-player-essence ?data uname)
          (remove-card-essence card :collect-essence uname)
          (add-chat 
            (str "collected " (clojure.string/join ", " (map #(str (val %) " " (-> % key name)) essence)) " from " (:name card))
            uname))))

; update functions above to take :collect-essence or :take-essence params)
(defn- take-essence [ gamestate {:keys [card] :as ?data} uname]
  (if verbose? (println "take-essence:" uname card))
  (-> gamestate 
      (update-player-essence {:essence (:take-essence card)} uname)
      (remove-card-essence card :take-essence uname)))

(defn- generate-essence [ gamestate ]
  ; copy from :collect to :collect-essence
  (if verbose? (println "Generate essence"))
    (-> gamestate
      (assoc :magicitems
        (map #(if (-> % :owner some?) (assoc % :collect-essence (:collect %)) %) (:magicitems gamestate)))
      (assoc :players
        (reduce-kv 
          (fn [m k v]
            (-> m
                (assoc-in [k :public :mage :collect-essence] (-> v :public :mage :collect)) ; mage
                (assoc-in [k :public :artifacts] (mapv #(assoc % :collect-essence (:collect %)) (-> v :public :artifacts)))   ; artifact / monument / pop
            )
          ) (:players gamestate) (:players gamestate))))) 

; AI Collect  

(defn- ai-collect-essence-artifacts [ gamestate ai-players ]
  (reduce-kv
    (fn [gs uname pdata]
      (reduce 
        (fn [gs artifact]
          (if-let [essence (:collect-essence artifact)]
            (collect-essence gs {:essence (first essence) :card artifact} uname)
            gs))
        gs
        (-> pdata :public :artifacts))
    )
    gamestate
    ai-players))

(defn- ai-collect-essence-mage [ gamestate ai-players ]
  (reduce-kv
    (fn [gs uname pdata]
      (let [mage (-> gs :players (get uname) :public :mage)]
        (if-let [essence (:collect-essence mage)]
          (collect-essence gs {:essence (first essence) :card mage} uname)
          gs)
    ))
    gamestate
    ai-players))

(defn- ai-collect-essence-magicitem [ gamestate ai-players ]
  (reduce-kv
    (fn [gs uname pdata]
      (let [magicitem (->> gs :magicitems (filter #(= (:owner %) uname)) first)]
        (if-let [essence (:collect-essence magicitem)]
          (collect-essence gs {:essence (first essence) :card magicitem} uname)
          gs)
    ))
    gamestate
    ai-players))

(defn- ai-collect-essence-done [ gamestate ai-players ]
  (reduce-kv
    (fn [m k v]
      (assoc-in m [:players k :collected?] true))
    gamestate
    ai-players))

(defn ai-collect-essence [ gamestate ]
  ; Take first available essence
  ; mage, magic item, artifact / monument / pop
  (let [ai-players (select-keys (:players gamestate) (filter is-ai? (-> gamestate :players keys)))]
    (if verbose? (println "AI-Collect-essence: ai-players" (keys ai-players)))
    (-> gamestate
      (ai-collect-essence-mage ai-players)
      (ai-collect-essence-magicitem ai-players)
      (ai-collect-essence-artifacts ai-players)
      (ai-collect-essence-done ai-players))
  ))
  
;;;;; DISCARD ;;;;;

(defn discardcard [ gs ?data uname ]
  (if verbose? (println uname "discard" (-> ?data :card :name) "essence" (:essence ?data)))
  (let [{:keys [essence card]} ?data]
    (if (= uname (get-active-player gs))
        (-> gs 
            (update-in [:players uname :public :discard] conj card)
            (assoc-in [:players uname :private :artifacts] (remove  #(= (:uid %) (:uid card)) (-> gs :players (get uname) :private :artifacts)))
            (update-player-essence ?data uname)
            (add-chat (str "Discard " (:name card)) uname)
            (add-chat (str "Gained " (clojure.string/join "," (map #(str (val %) " " (-> % key name clojure.string/capitalize)) essence))) uname)
            )
        gs)))

;;;;; PHASE TRANSITION ;;;;;

(defn- collect-phase [ gamestate ]
  (if verbose? "Collect phase")
  (-> gamestate
      (assoc :phase :collect)
      generate-essence
      (add-chat (str "Round " (:round gamestate)))
      (add-chat (str "Collect Phase"))
      ai-collect-essence))

(defn- determine-winner [ gamestate ] gamestate)

;;;;; ASSIGN MAGIC ITEMS ;;;;;       

(defn assignmagicitem [ gamestate ?data uname ]
  (if verbose? (println "assignmagicitem:" ?data uname))
    (-> gamestate
        (set-player-action uname :pass)
        (assoc :magicitems (mapv #(if (= (:owner %) uname)
                                      (dissoc % :owner :turned?)
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
(defn- player-public-components [ gs uname ]
  (let [pdata     (-> gs :players (get uname))
        mage      (-> pdata :public :mage) 
        mi        (->> gs :magicitems (filter #(= (:owner %) uname)) first) 
        artifacts (-> pdata :public :artifacts)] ; includes any claimed monuments and places of power
    (apply conj [] mage mi artifacts)))

(defn- update-vp [ gs ]
  (let [p1 (if (-> gs :pass-to empty?) (-> gs :plyr-to first) (-> gs :pass-to first))]
    (reduce-kv ; m = gs [k v] :players
      (fn [m k v]
        (assoc-in m [:players k :vp]
          (->>  (player-public-components m k)
                (filter :vp)
                (map #(hash-map (:name %) (vpfn %)))
                (concat (if (= p1 k) [{"First Player" 1}]))
                (apply conj )
          )))
      gs (:players gs))))

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
                (-> m 
                    (assoc-in [k :action] :waiting)
                    (update-in [k :public :mage] dissoc :turned?)
                    (assoc-in [k :public :artifacts] (->> v :public :artifacts (mapv #(dissoc % :turned?)))))) 
              (:players gamestate) (:players gamestate)))
          (set-player-action (-> gamestate :pass-to first) :play)
          (update :round inc)
          collect-phase)
      (add-chat gamestate "Start turn" (get-active-player gamestate))))

(defn end-action [ gamestate uname ]
  (let [nextplyrs (if (-> gamestate :players (get uname) :action (= :pass))
                      (:plyr-to gamestate)    
                      (->> gamestate :plyr-to (drop-while #(not= % uname)) rest))
        nextp     (if (empty? nextplyrs) (-> gamestate :plyr-to first) (first nextplyrs))
        loselife? (->> gamestate :players vals (map :loselife) (remove nil?) )]
    (if verbose? (println "end-action:" uname "nextplayer" nextp loselife? (empty? loselife?)))
    (if (not-empty loselife?)  ; DON@T ADVANCE IF THERE IS A ?LOSELIFE ACTION LIVE
        gamestate
        (-> gamestate
            (set-player-action uname (if (-> gamestate :plyr-to set (contains? uname)) :waiting :pass))
            (set-player-action nextp :play)
            (add-chat "End of turn" uname)
            new-round-check
            update-vp
            ))))

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
; Depends on update-player-essence
; depends on next player/round TODO
(defn- replacemonument [ gamestate draw? ]
  (if draw?
      (let [mondeck (-> gamestate :monuments :secret)]
        (-> gamestate
            (update-in [:monuments :public] conj (first mondeck))
            (assoc-in  [:monuments :secret] (-> mondeck rest vec))))
      gamestate))
    

(defn- playcard [ gamestate {:keys [card essence]} uname ]
  (if verbose? (println "playcard:" uname card "paid" essence ))
  (-> gamestate
    (assoc-in [:players uname :private :artifacts] (remove #(= (:uid %) (:uid card)) (-> gamestate :players (get uname) :private :artifacts))) ; Artifact
    (assoc :pops (->> gamestate :pops (remove #(= (:uid %) (:uid card)))))                                                                     ; Place of power
    (assoc-in [:monuments :public] (->> gamestate :monuments :public (remove #(= (:uid %) (:uid card))) vec))                                  ; Monument 
    (replacemonument (= "monument" (:type card)))
    (update-in [:players uname :public :artifacts] conj card)
    (add-chat (str (case (:type card) "artifact" "Played " "Claimed ") (:name card)) uname)
    (update-player-essence {:essence (invert-essence essence)} uname)
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
                      (update-in [k :public :mage] dissoc :collect-essence)
                      (assoc-in  [k :public :artifacts] (map #(dissoc % :collect-essence) (-> v :public :artifacts)))
                      )) (:players gamestate) (:players gamestate)))
            (assoc :magicitems (map #(dissoc % :collect-essence) (:magicitems gamestate)))
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

(defn- place-essence [ gamestate card essence uname ]
  ;; will only be a mage or in the players :public artifacts
  (if verbose? (println "place-essence:" uname card essence))
  (case (:type card)
    "mage" 
      (reduce-kv 
        (fn [m k v]
          (if (-> m :players (get uname) :public :mage :take-essence k)
              (update-in m [:players uname :public :mage :take-essence k] + v)
              (assoc-in  m [:players uname :public :mage :take-essence k]   v)))
        gamestate essence) ; regardless of card id
    ("artifact" "monument" "pop")
      (assoc-in gamestate [:players uname :public :artifacts] 
        (mapv 
          #(if (= (:uid %) (:uid card))
                (reduce-kv 
                  (fn [m k v] 
                      (if (-> m :take-essence k)
                          (update-in m [:take-essence k] + v)
                          (assoc-in  m [:take-essence k]   v))) 
                  % essence)
                %) (-> gamestate :players (get uname) :public :artifacts)))
    gamestate))

(defn- lose-life [ gamestate card action uname ]
  (if (and verbose? (:loselife action)) (println "lose-life:" card action uname))
  (if (:loselife action)
    (assoc gamestate :players
      (reduce
        (fn [ m k ]
          (if (not= k uname)
              (assoc-in m [k :loselife] (conj action (select-keys card [:name :id :type]) {:plyr uname}) )
              m))
        (:players gamestate) (:plyr-to gamestate)))
    gamestate))

;;; USE A CARD ;;;
;; Request: {:action :usecard, :useraction {:turn true, :cost {}, :gain {:death 2}, :rivals {:death 1}}, :gid :gm93866} dan
(defn- use-card [ gamestate {:keys [card useraction]} uname] 
    (if verbose? (println "use-card:" card useraction uname))
    (-> gamestate
        (add-chat (str "Used " (:type card) " " (:name card)) uname)                ; Chat
        (assoc :players                                                 ; Rivals gain 
          (:players 
            (reduce-kv (fn [m k v] 
              (if (not= uname k)
                  (update-player-essence m {:essence (:rivals useraction)} k)
                  m)) gamestate (:players gamestate))))
        (update-player-essence {:essence (invert-essence (:cost useraction))} uname)     ; Pay cost
        (update-player-essence {:essence (:gain useraction)} uname)     ; Gain essence
        (place-essence card (:place useraction) uname)                  ; Place essence
        (turn-card (:straighten useraction) nil uname)                  ; Straighten
        (drawcard uname (:draw useraction))                             ; Draw
        (turn-card card (:turn useraction) uname)                       ; Turn
        (lose-life card useraction uname)                               ; Trigger Lose Life responses
    ))


;;; REACT ;;;
(defn- react [ gamestate {:keys [card useraction] :as ?data} uname]
  (if verbose? (println "react:" uname useraction card))
  (-> gamestate 
      (add-chat "Reacted to lose life effect" uname)
      (use-card ?data uname)
      (update-in [:players uname] dissoc :loselife)))

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
                (assoc-in  [k :public :mage]  (-> (->> v :private :mages (filter #(= (:uid %) (-> v :public :mage))) first) (dissoc :target?) ))
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
            16) ])))

; PLACE an artifact
; CLAIM a place of power or Monument
; DISCARD a card for 1g or 2other
; USE a power
; conditions/codes
; - 1  player action is play
; - 2  Artifact card is in hand
; - 4  Place of Power card is in pops
; - 8  Monument is in Monument pool
; - 16 Not an Artifact, Pop, or Monument
; - 32 Card is turned

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
      :collect-essence  (collect-essence  gamestate ?data uname)
      :take-essence     (take-essence     gamestate ?data uname)
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
      :usecard          (let [err 0]     ;error handler
                          (if (= err 0)
                              (-> gamestate 
                                  (use-card ?data uname)
                                  (end-action uname)
                                  ai-action)
                              (assoc-in gamestate [:players uname :err] err)))
    ; PASS
      :pass             (pass             gamestate uname)
    ; REACT (lose life, check for winner)
      :react            (-> gamestate 
                            (react ?data uname)
                            (end-action (get-active-player gamestate))
                            ai-action)
    ; TESTING ONLY
    ; Done - Only used for Testing
      :done             (end-action       gamestate uname)
      :swapgame         (case (:game ?data)
                              1 ragames/game1
                              2 ragames/game2
                              3 ragames/game3
                              4 ragames/game4
                              gamestate)
      gamestate)))

(defn- essence-match-handler [ gs uname k v ]
  (if verbose? (println "essence-match-handler:" uname k v))
  (-> gs 
      (assoc-in [:players uname :public :essence (-> k clojure.string/lower-case keyword)] (-> v read-string))
      (add-chat (str "Set " k " to " v) uname :usercmd)))

(def chat-fn-help {
  "essence" "/essence <essence name> <new value>"
})

(defn- usercmd-playcard [ gs cardname uname ]
;; IMPORTANT FOR TESTING ONLY TODO LIMIT BY ENV VARIABLE
  (if verbose? (println "PLAYING" cardname))
  (if-let [card (->> @data :artifacts (filter #(= (:name %) cardname)) first)]
    (-> gs
        (update-in [:players uname :public :artifacts] conj (assoc card :uid (gensym "art")))
        (add-chat (str "Played " cardname " OUT OF NOWHERE!") uname :usercmd))
    gs))

(defn chat-handler [ gs msg uname ]
  (let [fn-hint (re-find #"(?i)\/(\w+)" msg)
        essence-match (re-matches #"(?i)\/(essence)\s(gold|elan|calm|life|death)\s(\d+)" msg)
        card-match    (re-matches #"(?i)\/playcard\s(.+)" msg)
        mage-match    (re-matches #"(?i)\/setmage\s(.+)"  msg)
        loselife-match (re-matches #"(?i)\/loselife\s(\d)\s(gold|elan|calm|life|death)(|.+)" msg)
        ]
    (if verbose? (println "chat-handler:" uname msg essence-match card-match mage-match loselife-match ))
    (cond
      essence-match   (-> gs 
                      (add-chat msg uname :usercmd) 
                      (essence-match-handler uname (nth essence-match 2) (last essence-match)))
      card-match      (-> gs 
                          (add-chat msg uname :userdm) 
                          (usercmd-playcard (last card-match) uname))     
      mage-match      (if-let [mage (->> @data :mages (filter #(= (:name %) (last mage-match))) first)]
                              (-> gs 
                                  (add-chat msg uname :userdm)
                                  (add-chat (str "Changed Mage to " (last mage-match)) uname :usercmd)
                                  (assoc-in [:players uname :public :mage] (assoc mage :uid (gensym "mage"))))
                              (-> gs 
                                  (add-chat msg uname :userdm)))
      loselife-match  (let [life (nth loselife-match 1) px (if (empty? (nth loselife-match 3)) uname (-> loselife-match (nth 3) clojure.string/trim)) essence (nth loselife-match 2)]
                        (-> gs 
                            (add-chat msg uname :userdm)
                            (add-chat (str px " triggered Lose Life event: Lose " life " life. Spend 1 " essence " to ignore" ))
                            (lose-life {:name "User Action"} {:loselife (read-string life) :ignore {(keyword essence) 1}} px)
                            (end-action px)))
      fn-hint         (case (last fn-hint)
                            "endturn" (-> gs 
                                          (add-chat msg uname :usercmd)
                                          (end-action uname)
                                          ai-action)
                            (-> gs 
                                (add-chat msg uname :usercmdhelp) 
                                (add-chat (str "help: " (-> fn-hint last clojure.string/lower-case (chat-fn-help fn-hint ))) uname :usercmdhelp)))
      :default        (add-chat gs msg uname))))
;; Player States :action
; :waiting
; :play
; :selectmagicitem
; :reaction?