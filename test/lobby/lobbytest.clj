(ns lobby.lobbytest
  (:require 
    [expectations :refer :all]
    [lobby.model :as model]
    [lobby.ramodel :as ramodel]))
    
(defn newgamegid 
  ([ game ]
    (->> {:game game :title "P1 Game"}
         (model/creategame! "p1")
         :games
         (reduce-kv #(if (= (:owner %3) "p1") %2) {})))
  ([]
    (newgamegid "Res Arcana")))
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
        
;;; don't join full game 
(expect 4
  (let [gid (newgamegid)]
    (doseq [n (range 5)]
      (model/joingame! (gensym "P") gid))
    (-> @model/appstate
        :games
        gid
        :plyrs 
        count)))
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

    
;; Start 'Res Arcana' (2 players min)
(expect :setup
  (let [gid (newgamegid) gm (model/joingame! "p2" gid)]
    (-> (model/startgame! gid)
        :games 
        gid
        :state
        :status)))
        
;; Don't start if minplayers isn't met :minp
;; TODO 
;(expect nil
;  (let [gid (newgamegid)]
;    (-> (model/startgame! gid)
;        :games 
;        gid
;        :state
;        :status)))
    

;;; TODO Start nil game, non-existamt game
    
    
    
; Parse Chat Commands
;; /add <resource> <?n>
    
  