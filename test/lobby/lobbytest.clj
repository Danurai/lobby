(ns lobby.lobbytest
  (:require 
    [expectations :refer :all]
    [lobby.model :as model]
    [lobby.ramodel :as ramodel]))
    
(defn newgamegid []
  (->> {:game "Res Arcana" :title "P1 Game"}
       (model/creategame! "p1")
       :games
       (reduce-kv #(if (= (:owner %3) "p1") %2) {})))  
; User connect / disconnect

; Create / Join / Leave / Start

;; Create multiple games, only one game per owner
(expect {:owner "p1" :game "Test" :title "P1 Game"}
  (select-keys
    (->> (model/creategame! "p1" {:game "Test" :title "P1 Game"})
         :games
         (reduce-kv #(if (= (:owner %3) "p1") %3) {}))
    [:owner :game :title]))

(expect 2
  (do 
    (swap! model/appstate dissoc :games)
    (model/creategame! "p1" {:game "Test" :title "P1 Game"})
    (model/creategame! "p2" {:game "Test" :title "P2 Game"})
    (-> @model/appstate :games count)))
    
(expect 1
  (do 
    (swap! model/appstate dissoc :games)
    (model/creategame! "p1" {:game "Test" :title "P1 Game"})
    (model/creategame! "p1" {:game "Test" :title "P1 Game"})
    (->> @model/appstate :games count)))
    

;; Join
;;; Join Real Game
(expect true
  (let [gid (newgamegid)]
    (-> (model/joingame! "p2" gid)
        :games
        gid
        :plyrs
        (contains? "p2"))))

;;; TODO Join nil game, non-existamt game

;; Leave
;;;P2 Leaves
(expect false
  (let [gid (newgamegid)]
    (do 
      (model/joingame! "p2" gid)
      (-> (model/leavegame! "p2" gid)
          :games
          gid
          :plyrs
          (contains? "p2")))))

;;;Owner Leaves
(expect nil
  (let [gid (newgamegid)]
    (-> (model/leavegame! "p1" gid)
        :games
        gid)))
        

;;; TODO Leave nil game, non-existamt game

    
;; Start 'Res Arcana'
(expect :setup
  (let [gid (newgamegid)]
    (-> (model/startgame! gid)
        :games 
        gid
        :state
        :status)))
    

;;; TODO Start nil game, non-existamt game
    
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
      
; New AI Player has a Public/private selected Mage
(expect 1
  (-> (ramodel/setup ["p1" "AI"])
      :players
      (get "AI")
      :public
      :mage))
(expect 1
  (count 
    (filter :selected
      (-> (ramodel/setup ["p1" "AI"])
          :players
          (get "AI")
          :private
          :mages))))

; Player has a Public/private selected Mage
(expect 1 
  (let [gs (ramodel/setup ["p1" "p2" "AI"])]
      (-> gs
          (ramodel/selectmage {:card (-> gs :players (get "p1") :private :mages first) :select? true} "p1")
          :players
          (get "p1")
          :public
          :mage)))
; ..even if they do it multiple times
(expect 1 
  (let [gs (ramodel/setup ["p1" "p2" "AI"])]
      (-> gs
          (ramodel/selectmage {:card (-> gs :players (get "p1") :private :mages first) :select? true} "p1")
          (ramodel/selectmage {:card (-> gs :players (get "p1") :private :mages second) :select? true} "p1")
          :players
          (get "p1")
          :public
          :mage)))
; Deselect
(expect 0 
  (let [gs (ramodel/setup ["p1" "p2" "AI"])]
      (-> gs
          (ramodel/selectmage {:card (-> gs :players (get "p1") :private :mages first) :select? true} "p1")
          (ramodel/selectmage {:card (-> gs :players (get "p1") :private :mages first) :select? false} "p1")
          :players
          (get "p1")
          :public
          :mage)))
; Private, select mage twice        
(expect 1
  (let [gs (ramodel/setup ["p1" "p2" "AI"])]
    (count
      (filter :selected
        (-> gs
            (ramodel/selectmage {:card (-> gs :players (get "p1") :private :mages first) :select? true} "p1")
            (ramodel/selectmage {:card (-> gs :players (get "p1") :private :mages second) :select? true} "p1")
            :players
            (get "p1")
            :private
            :mages)))))

; All players selected a mage (including AI setup), game on!
(expect :started
  (let [gs (ramodel/setup ["p1" "AI"])]
    (-> gs
        (ramodel/selectmage {:card (-> gs :players (get "p1") :private :mages first) :select? true} "p1")
        :state)))
          
(expect 2
  (let [gs (ramodel/setup ["p1" "AI"])]
    (->> (ramodel/selectmage gs {:card (-> gs :players (get "p1") :private :mages first) :select? true} "p1")
          :players
          (map (fn [[k v]] (-> v :public :mage)))
          (map :name)
          count)))
      
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