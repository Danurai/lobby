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
        :games gid :state
        (dissoc :teams :timelimit :chat))))

(expect 6
  (let [gid (newgamegid "Death Angel")]
    (-> (model/startgame! gid)
        :games gid :state 
        :teams count)))
        
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
  (->> onepgm :teams (filter (fn [[k v]] (= (:cmdr v) "p1"))) count))
        
(expect 3
  (count (filter (fn [[k v]] (= (:cmdr v) "p1"))
    (-> onepgm (damodel/pickteam "p1" :purple) :teams))))
          
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
;; :event, :events & obfuscation
(expect true
  (-> onepgm (damodel/parseaction {:action :start} "p1") 
    :event some?))
(expect 29
  (-> onepgm (damodel/parseaction {:action :start} "p1") 
    (damodel/obfuscate "p1") :events))
;; :blipdeck 
;; Spawn: 3
(expect  #(> % 16)
  (->> (damodel/parseaction onepgm {:action :start} "p1") 
       :blipdeck (map :type) frequencies vals (apply +)))
;; & obfuscation
(expect  #(> % 16)
  (-> onepgm (damodel/parseaction {:action :start} "p1") 
      (damodel/obfuscate "p1") :blipdeck))

; 3. Setup location deck
;; :path
(expect [:2 :3 :4]
  (-> onepgm (damodel/parseaction {:action :start} "p1") 
    :path))
;; :spawns (based on teamcount)
(expect {:maj 2 :min 1}
  (-> onepgm (damodel/parseaction {:action :start} "p1") 
    :spawns))
    
; 2. Setup starting location
(expect {:id :void3 :stage :0 :name "Void Lock"}
  (-> onepgm (damodel/parseaction {:action :start} "p1") 
    :location (select-keys [:id :stage :name])))
(expect {:top 6 :bot 6}  
  (->> (damodel/parseaction onepgm {:action :start} "p1") 
    :blips
    ))
; 4. Choose combat teams
;; pre

; 5. Setup formation
;; 2 zones per team
(expect 6
  (-> onepgm (damodel/parseaction {:action :start} "p1") 
    :formation count))

; 6. Place support tokens
(expect 12 
  (-> onepgm (damodel/parseaction {:action :start} "p1") :maxsupport))
  
; 7. Setup terrain and blips
;; [{:zone n :marine m :top {:terrain [] :swarm [{:id :type}]} :bot {:terrain [] :swarm [{:id :type}]}}]
;;; zone
(expect 1
  (-> onepgm (damodel/parseaction {:action :start} "p1") 
    :formation first :zone))
;;; marine    
(expect :top
  (-> onepgm (damodel/parseaction {:action :start} "p1") 
    :formation first :marine :facing))
;;; terrain
(expect {:id :door :facing :top :threat 2}
  (select-keys 
    (-> onepgm (damodel/parseaction {:action :start} "p1") :formation (get 0) :terrain first)
    [:id :facing :threat]))
(expect {:id :corner :facing :top :threat 3}
  (select-keys 
    (-> onepgm (damodel/parseaction {:action :start} "p1") :formation (get 2) :terrain first)
    [:id :facing :threat]))
(expect {:id :vent :facing :bot :threat 4}
  (select-keys 
    (-> onepgm (damodel/parseaction {:action :start} "p1") :formation (get 3) :terrain first)
    [:id :facing :threat]))
(expect {:id :corridor :facing :bot :threat 1}
  (select-keys 
    (-> onepgm (damodel/parseaction {:action :start} "p1") :formation (get 4) :terrain first)
    [:id :facing :threat]))
;; 8. Spawn starting genestealers
;(expect #(> % 2)
;  (-> onepgm (damodel/parseaction {:action :start} "p1")
;      :formation first :top count
;    ))
;(expect 1
;  (->> (damodel/parseaction onepgm {:action :start} "p1") 
;    :formation (map #(apply conj (-> % :top :swarm) (-> % :bot :swarm))) (remove empty?) first
;    ))
;(expect true
;  (-> onepgm (damodel/parseaction {:action :start} "p1") 
;    :enemies))