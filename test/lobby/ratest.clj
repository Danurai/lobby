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
;; TODO ??
        
; All players selected a mage (including AI setup), and a Magic Item game on!
;; TODO 
;(expect :started
;  (let [gs (ramodel/setup ["p1" "AI"])]
;    (-> gs
;        (ramodel/selectstartmage {:card (-> gs :players (get "p1") :private :mages first :name)} "p1")

;        :status)))
          
(expect 2
  (let [gs (ramodel/setup ["p1" "AI"])]
    (->> (ramodel/selectstartmage gs {:card (-> gs :players (get "p1") :private :mages first :name)} "p1")
          :players
          (map (fn [[k v]] (-> v :public :mage)))
          (map :name)
          count
          )))

;; Select Mage 
;; TODO 

          
;; Select Magic Item
; one at a time
;; TODO 
; reverse turn order
;; TODO
;(expect :selectmagicitem
;  (let [gs (ramodel/setup ["p1"])]
;    (-> (ramodel/selectstartmage gs {:card (-> gs :players (get "p1") :private :mages first :name)} "p1")
;        :players
;        (get "p1")
;        :action
;        )))
     
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