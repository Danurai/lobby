(ns lobby.bbmodel
  (:require 
    [lobby.bbdata :as bbdata]))

(defonce gamestate {
  :teams (filter #(not= "Freebooter" (:team %)) bbdata/players)
  :turn 0
  :chat []
})

(defn obfuscate [ state uname ]
  state)
(defn- isai? [ plyr ]
  (some? (re-matches #"AI\d+" plyr) ))

;; ACTIONS


(defn- end-turn 
  ; dissoc player state, id next player)
  ([ state pass ]
    (let [ap    (-> state :activeplyr)
          to    (->> state :turnorder (repeat 2) (apply concat) (remove #(-> state :players (get %) :passed true?)) vec)
          apidx (.indexOf to ap)
          np    (get to (inc apidx))]
      (prn "end turn" ap np "pass?" pass)
      ;(if (and (-> state :players (get ap) :abilitylist count (> 0)) (-> state :players (get ap) :status (= :abilities)))
      ;    state
          (-> state
              (update :turn inc)
              (assoc :activeplyr np)
              (update-in [:players ap] dissoc :state)
              (assoc-in [:players ap :passed?] pass)
              (assoc-in [:players np :state] :matchup))))
      ;)
  ([ state ] 
    (let [ap (-> state :activeplyr)]
      (end-turn state (-> state :players (get ap) :team :private empty?)))))

(defn commit-player [ state uname playerid highlightid zone ]
  (let [p (->> (-> state :players (get uname) :team :private) (filter #(= (:id %) playerid)) first)]
    (-> state 
      (assoc-in [:players uname :team :private] (remove #(= (:id %) playerid) (-> state :players (get uname) :team :private)))
      ;(assoc-in [:players uname :status] :abilities)
      ;(assoc-in [:players uname :abilitylist] (:abilities p))
      (assoc-in [:highlights :public]
        (mapv 
          #(if (= (:id %) highlightid)
              (update-in % [:zone zone] conj p)
              %) (-> state :highlights :public)))
      end-turn))) ; (-> state :players (get uname) :team :public empty?)))))   

;; AI

(defn ai-choose-team [ state ]
  (let [chosen-teams (->> state :players (map #(-> % last :team)) (remove nil?) set) 
        teams        (->> state :teams (map :team) distinct (remove chosen-teams)) ]
    ;(prn "ai-choose-team" (first teams))
    (rand-nth teams)))

(defn get-team-zone-map [ hl ]
  (reduce-kv
    (fn [m k v]
      (let [tm (-> v first :team)]
        (if tm (assoc m tm k) m)))
    {} (:zone hl)))

(defn- get-player-zone [ hl pl ] 
  (let [team-zone-map  (get-team-zone-map hl)
        committed-zone (get team-zone-map (:team pl))
        rand-zones (-> [:a :b] shuffle)]
    (prn "get-player-zone" (:id hl) team-zone-map committed-zone)
    (if (nil? committed-zone)
        (if (-> hl :zone (get (first rand-zones)) empty?)
            (first rand-zones)
            (last rand-zones))
        committed-zone)))

(defn ai-commit-player [ state uname ]
  (let [ pl   (-> state :players (get uname) :team :private rand-nth)
         plid (:id pl) 
         hlid (->> state :highlights :public (map :id) rand-nth)
         hl   (->> state :highlights :public (filter #(= (:id %) hlid)) first) 
         zone (get-player-zone hl pl)]
    (prn "ai-commit" plid hlid zone)
    [ plid hlid zone ]))

; ACTIONS

(defn- start-turn [ state ]
  (let [ap (:activeplyr state)]
    (prn "start turn" ap "isai?" (isai? ap) "passed?" (-> state :players (get ap) :passed?))
    (if (-> state :players (get ap) :passed?)
        (-> state 
          (assoc :activeplyr nil)
          (assoc :status :scoreboard))
        (if (isai? ap)
            (case (-> state :players (get ap) :state)
              :matchup (apply commit-player state ap (ai-commit-player state ap))
              state)
            state))))

;; SETUP 

(defn- make-team [ teams team ]
  (let [ teamplyrs (->> teams (filter #(= (:team %) team)) shuffle) ]
    (hash-map
      :alliance (-> teamplyrs first :alliance)
      :team (-> teamplyrs first :team)
      :public []
      :private (take 6 teamplyrs)
      :secret  (nthrest teamplyrs 6) )))

(defn start-game [ state ]
  (let [nhl (-> state :turnorder count)]
    (-> state 
        (assoc :status :started)
        (dissoc :teams)
        (assoc :turn 0)
        ; set player hands 
        (assoc :players 
          (reduce-kv 
            (fn [m k v]
              (assoc m k {:team (make-team (:teams state) (:team v))})) {} (:players state)))
        (assoc :activeplyr (-> state :turnorder first))
        (assoc-in [:players (-> state :turnorder first) :state] :matchup)
        ; VV Replace with draw-highlights? VV ;
        (assoc-in [:highlights :public] (->> state :highlights :secret (take nhl) (map #(assoc % :zone {:a [] :b []}))))
        (assoc-in [:highlights :secret] (-> state :highlights (nthrest nhl)))
        start-turn
    )))
  
(defn- choose-team [ state team uname ]
  (-> state
      (assoc-in [:players uname :team] team)
      (update :turn inc)))

(defn- check-start [ state ]
  (if (= (:turn state) (-> state :turnorder count) )
      (start-game state)      
      (let [ cp (get (:turnorder state) (:turn state) )]
        (if (isai? cp)
            (-> state 
                (choose-team (ai-choose-team state) cp)
                check-start)
        state))))

(defn setup [ plyrs ]
  (let [ turnorder  (-> plyrs shuffle) ]
    (-> gamestate 
      (assoc :status :setup)
      (assoc :highlights {:secret (-> bbdata/highlights shuffle) :discard []})
      (assoc :players (zipmap plyrs (repeat {:public {} :private {} :secret {}})))
      (assoc :turnorder turnorder)
      check-start)))

(defn parseaction [ state ?data uname ]
  (prn ?data)
  (case (:action ?data)
    :chooseteam   (-> state (choose-team (:team ?data) uname) check-start)
    :commitplayer (-> state (commit-player uname (:plid ?data) (:hlid ?data) (:zone ?data)) start-turn)
    :pass         (-> state (end-turn true) start-turn)
    state))