(ns lobby.ratest
  (:require
    [expectations :refer :all]
    [lobby.model :as model]
    [lobby.ramodel :as ramodel]
    [lobby.lobbytest :refer :all]))
    
    
; RA 
;; New game has 5 distinct :base places of power, 2 public monumnets and 8 hidden

(expect 5
  (->> (ramodel/setup ["p1"])
       :pops
       (map :base)
       distinct
       count))
       
(expect 2 
  (-> (ramodel/setup ["p1"])
      :monuments
      :public
      count))
(expect 8
  (-> (ramodel/setup ["p1"])
      :monuments
      :secret
      count))
; Obfuscated monument deck for p1
(expect 8
  (-> (ramodel/setup ["p1"])
      (ramodel/obfuscate "p1")
      :monuments
      :secret))

; Each new player has 2 mages and 8 artifacts

(expect 2
  (-> (ramodel/setup ["p1"])
      :players
      (get "p1")
      :private 
      :mages
      count))
(expect 8
  (-> (ramodel/setup ["p1"])
      :players
      (get "p1")
      :private 
      :artifacts
      count))
      
; new player :action is :selectmage
(expect :selectmage
  (-> (ramodel/setup ["p1"])
      :players
      (get "p1")
      :action))
      
; New AI Player has a Public/private selected Mage
(expect 1
  (let [aip (-> "AI" gensym str)]
    (-> (ramodel/setup ["p1" aip])
        :players
        (get aip)
        :public
        :mage)))
(expect 1
  (let [aip (-> "AI" gensym str)]
    (count (filter :selected 
      (-> (ramodel/setup ["p1" aip])
          :players
          (get aip)
          :private
          :mages)))))

; 1 player game has 1 player etc
(expect 2
  (let [gid (newgamegid) tmp (model/joingame! "p2" gid)]
    (-> (model/startgame! gid) :games gid :state :players keys count)))


;; Select Mage 
(expect :selectmage
  (let [gid (newgamegid) p2 (model/joingame! "p2" gid)]
    (-> (model/startgame! gid)
        :games 
        gid
        :state 
        :players
        (get "p1")
        :action)))
        
; Player has a Public/private selected Mage
(expect 1 
  (let [gid   (newgamegid)
        p2    (model/joingame! "p2" gid)
        state (model/startgame! gid)
        card  (-> state :games gid :state :players (get "p1") :private :mages first)]
    (-> (model/updategame! {:gid gid :action :selectstartmage :select? true :card card} "p1")
        :games
        gid
        :state
        :players
        (get "p1")
        :public 
        :mage)))
; ..even if they do it multiple times
(expect 1 
  (let [gid   (newgamegid)
        p2    (model/joingame! "p2" gid)
        state (model/startgame! gid)
        cards (-> state :games gid :state :players (get "p1") :private :mages)
        tmp   (model/updategame! {:gid gid :action :selectstartmage :select? true :card (first cards)} "p1")]
    (-> (model/updategame! {:gid gid :action :selectstartmage :select? true :card (last cards)} "p1")
        :games
        gid
        :state
        :players
        (get "p1")
        :public 
        :mage)))

          
; AI and P1 have selected a mage
(expect 2
  (let [gs (ramodel/setup ["p1" "AI"])]
    (->> (ramodel/selectstartmage gs {:card (-> gs :players (get "p1") :private :mages first :name)} "p1")
          :players
          (map (fn [[k v]] (-> v :public :mage)))
          (map :name)
          count
          )))

; All players selected a mage (including AI setup) - select Magic Item

(defn newgameselectmages [ plyrs ]
  (let [gid   (newgamegid)]
    (doseq [p (rest plyrs)]
      (model/joingame! p gid))
    (let [appstate (model/startgame! gid)]
      (doseq [p plyrs]
        (model/updategame! {:gid gid :action :selectstartmage :select? true :card (-> appstate :games gid :state :players (get p) :private :mages first)} p))
      gid)))

;; Select Magic Item
; reverse turn order
(expect :selectstartitem 
  (let [gid (newgameselectmages ["p1" "p2"])
        gm (-> @model/appstate :games gid)
        lp (-> gm :state :turnorder last)]
    (-> gm :state :players (get lp) :action)))
; one at a time
(expect 1
  (let [gid (newgameselectmages ["p1" "p2"])
        gm (-> @model/appstate :games gid)
        lp (-> gm :state :turnorder last)]
    (->> gm :state :players (filter (fn [[k v]] (= (:action v) :selectstartitem))) keys count)))

; MagicItem Selected    
(expect true
  (let [gid (newgameselectmages ["p1" "p2" "p3"])
        gm (-> @model/appstate :games gid)
        mi (-> gm :state :magicitems first)
        to (-> gm :state :turnorder)]
    (= (last to)
      (-> (model/updategame! {:gid gid :action :selectstartitem :select? true :card mi} (last to))
          :games gid :state :magicitems first :owner))))
;
;;; 2nd choice - reverse turn order
(expect :selectstartitem
  (let [gid (newgameselectmages ["p1" "p2" "p3"])
        gm (-> @model/appstate :games gid)
        mi (-> gm :state :magicitems first)
        to (-> gm :state :turnorder)]
    (-> (model/updategame! {:gid gid :action :selectstartitem :select? true :card mi} (last to))
        :games gid :state :players (get (nth to 2)) :action)))
(expect 1
  (let [gid (newgameselectmages ["p1" "p2" "p3"])
        gm (-> @model/appstate :games gid)
        mi (-> gm :state :magicitems first)
        to (-> gm :state :turnorder)]
    (->> (model/updategame! {:gid gid :action :selectstartitem :select? true :card mi} (last to))
         :games gid :state :players (filter (fn [[k v]] (= (:action v) :selectstartitem))) keys count)))
      
; All players selected a mage (including AI setup), and a Magic Item game on!
;; TODO 
(expect 2
  (let [gid (newgameselectmages ["p1" "p2"])
        gm  (-> @model/appstate :games gid)
        mis (-> gm :state :magicitems)
        to  (-> gm :state :turnorder)]
    (do 
      (model/updategame! {:gid gid :action :selectstartitem :select? true :card (first mis)} (first to))
      (model/updategame! {:gid gid :action :selectstartitem :select? true :card (last mis)} (last to)))
    (->> @model/appstate :games gid :state :players (reduce-kv (fn [m k v] (if (= :pass (:action v)) (inc m) m)) 0))))
(expect :started
  (let [gid (newgameselectmages ["p1" "p2"])
        gm  (-> @model/appstate :games gid)
        mis (-> gm :state :magicitems)
        to  (-> gm :state :turnorder)]
    (do 
      (model/updategame! {:gid gid :action :selectstartitem :select? true :card (first mis)} (first to))
      (model/updategame! {:gid gid :action :selectstartitem :select? true :card (last mis)} (last to)))
    (-> @model/appstate
        :games gid :state :status)))          

; Obfuscation 
; Public = P1 can see P1 public data, P2 can see P1 public data
(expect true 
  (let [gs (ramodel/setup ["p1" "p2"])]
    (=
      (-> gs :players (get "p1") :public)
      (-> gs (ramodel/obfuscate "p1") :players (get "p1") :public))))
(expect true 
  (let [gs (ramodel/setup ["p1" "p2"])]
    (=
      (-> gs :players (get "p1") :public)
      (-> gs (ramodel/obfuscate "p2") :players (get "p1") :public))))
      
; Private = P1 can see P1 private data P2 can't see P1 private data, only counts
(expect true
  (let [gs (ramodel/setup ["p1" "p2"])]
    (=
      (-> gs :players (get "p1") :private)
      (-> gs (ramodel/obfuscate "p1") :players (get "p1") :private))))
      
(expect 8
  (-> (ramodel/setup ["p1" "p2"])
      (ramodel/obfuscate "p2") 
      :players 
      (get "p1")
      :private
      :artifacts))
      
; Secret = P1 can't see P1 Secret Data, P2 can't see P1 secret data
(expect 0
  (-> (ramodel/setup ["p1" "p2"])
      (ramodel/obfuscate "p1") 
      :players 
      (get "p1")
      :secret
      :discard))

(expect 0
  (-> (ramodel/setup ["p1" "p2"])
      (ramodel/obfuscate "p1") 
      :players 
      (get "p2")
      :secret
      :discard))