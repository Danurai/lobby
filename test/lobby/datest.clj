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
    
(expect {:status :setup :teamlimit 3 :players {"p1" {:public {:teams []}}}}
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
          
; Release Team
(expect 0
  (let [gid (newgamegid "Death Angel")]
    (count (filter (fn [[k v]] (= (:cmdr v) "p1"))
      (-> onepgm 
        (damodel/pickteam "p1" :blue  )
        (damodel/pickteam "p1" :yellow)
        (damodel/pickteam "p1" :green )
        :teams)))))
  