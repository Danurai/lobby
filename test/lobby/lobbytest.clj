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