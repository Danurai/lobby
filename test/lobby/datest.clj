(ns lobby.datest
  (:require 
    [expectations :refer :all]
    [lobby.model :as model]
    [lobby.damodel :as damodel]
    [lobby.lobbytest :refer :all]))
    
(def onepgm 
  (let [gid (newgamegid "Death Angel")]
    (-> (model/startgame! gid) :games gid :state 
      (damodel/pickteam "p1" :blue)
      (damodel/pickteam "p1" :green)
      (damodel/pickteam "p1" :yellow))))
    
(expect "Death Angel"
  (let [gid (newgamegid "Death Angel")]
    (-> @model/appstate :games gid :game)))
    
(expect {:status :setup :teamlimit 3 :players {"p1" {:public {:teams []}}} :maxsupport 12}
  (let [gid (newgamegid "Death Angel")]
    (-> (model/startgame! gid)
        :games 
        gid
        :state
        (dissoc :teams :timelimit :chat))))

(expect 6
  (let [gid (newgamegid "Death Angel")]
    (-> (model/startgame! gid)
        :games 
        gid
        :state 
        :teams
        count)))
        
;; Limit # teams based on player count : 1p
(expect 1
  (let [gid (newgamegid "Death Angel")]
    (count (filter (fn [[k v]] (= (:cmdr v) "p1"))
      (-> (model/startgame! gid) :games gid :state 
        (damodel/pickteam "p1" :blue)
        :teams)))))
        
; as a parsed action
(expect 1
  (let [gid (newgamegid "Death Angel")]
    (count (filter (fn [[k v]] (= (:cmdr v) "p1"))
      (-> (model/startgame! gid) :games gid :state 
        (damodel/parseaction {:action :pickteam :team :blue} "p1")
        :teams)))))
        
(expect 2
  (let [gid (newgamegid "Death Angel")]
    (count (filter (fn [[k v]] (= (:cmdr v) "p1"))
      (-> (model/startgame! gid) :games gid :state 
        (damodel/pickteam "p1" :blue)
        (damodel/pickteam "p1" :green)
        :teams)))))
        
(expect 3
  (->> onepgm :teams 
       (filter (fn [[k v]] (= (:cmdr v) "p1")))
       count))
        
(expect 3
  (count (filter (fn [[k v]] (= (:cmdr v) "p1"))
    (-> onepgm
        (damodel/pickteam "p1" :purple)
        :teams))))
          
; Release / drop Team
(expect 0
  (let [gid (newgamegid "Death Angel")]
    (count (filter (fn [[k v]] (= (:cmdr v) "p1"))
      (-> onepgm 
        (damodel/pickteam "p1" :blue  )
        (damodel/pickteam "p1" :yellow)
        (damodel/pickteam "p1" :green )
        :teams)))))
  
; Start 1p game
(expect true
  (-> onepgm (damodel/parseaction :start "p1") some?))
  
; 1. Setup decks
(expect true
  (-> onepgm (damodel/parseaction {:action :start} "p1") 
    :event some?))
(expect 29
  (-> onepgm (damodel/parseaction {:action :start} "p1") 
    (damodel/obfuscate "p1") :events))
(expect 20
  (-> onepgm (damodel/parseaction {:action :start} "p1") 
     (damodel/obfuscate "p1") :gdeck))
; 3. Setup location 'deck'
(expect [:2 :3 :4]
  (-> onepgm (damodel/parseaction {:action :start} "p1") 
    :path))
(expect {:maj 2 :min 1}
  (-> onepgm (damodel/parseaction {:action :start} "p1") 
    :spawns))
; 2. Setup starting location
(expect {:id :void3 :stage :0 :name "Void Lock"}
  (-> onepgm (damodel/parseaction {:action :start} "p1") 
    :location (select-keys [:id :stage :name])))
; 4. Choose combat teams
; 5. Setup formation
(expect 6
  (-> onepgm (damodel/parseaction {:action :start} "p1") 
    :formation count))
; 6. Place support tokens
;; 7. Setup terrain and blips
;(expect {:top 2 :bottom 2}
;  (-> onepgm (damodel/parseaction {:action :start} "p1") 
;    :terrain frequencies))
;; 8. Spawn starting genestealers
;(expect true
;  (-> onepgm (damodel/parseaction {:action :start} "p1") 
;    :enemies))