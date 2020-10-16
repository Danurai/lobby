(ns lobby.lobbytest
  (:require 
    [expectations :refer :all]
    [lobby.model :as model]
    [lobby.ramodel :as ramodel]))

(expect {:owner "p1" :game "RA" :title "Game"}
  (select-keys
    (-> (model/creategame "p1" {:game "RA" :title "Game"})
        :games
        last)
    [:owner :game :title]))

(expect 2
  (do 
    (swap! model/appstate assoc :games [])
    (model/creategame "p1" {:game "RA" :title "Game"})
    (model/creategame "andy" {:game "RA" :title "Game"})
    (-> @model/appstate :games count)))
    
(expect 1
  (do 
    (swap! model/appstate assoc :games [])
    (model/creategame "p1" {:game "RA" :title "Game"})
    (model/creategame "p1" {:game "RA" :title "Game"})
    (->> @model/appstate :games count)))
    
; RA 

; New game has 5 distinct :base places of power, 2 public monumnets and 8 hidden

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
; Public = P2 can see P1 public data
(expect true 
  (let [gs (ramodel/setup ["p1" "p2"])]
    (=
      (-> gs :players (get "p1") :public)
      (-> gs (ramodel/obfuscate "p2") :players (get "p1") :public))))
      
; Private = P1 can see P1 public data, only counts
(expect true
  (let [gs (ramodel/setup ["p1" "p2"])]
    (=
      (-> gs :players (get "p1") :private)
      (-> gs (ramodel/obfuscate "p1") :players (get "p1") :private))))
      
; Private = P2 can't see P1 public data, only counts
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