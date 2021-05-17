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

(defn parseaction [ state ?data uname]
  state)

(defn make-team [ teams team ]
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
        ; VV Replace with draw-highlights? VV ;
        (assoc-in [:highlights :public] (->> state :highlights :secret (take nhl)))
        (assoc-in [:highlights :secret] (-> state :highlights (nthrest nhl)))
    )))

(defn ai-choose-team [ state plyr ]
  (let [ chosen-teams (->> state :plyers (map #(-> % last :team)) (remove nil?) set) 
         teams        (->> state :teams (map :team) distinct (remove chosen-teams)) ]
    (-> state 
        (assoc-in [:players plyr :team] (first teams))
        (update :turn inc))))

(defn check-start [ state ]
  (if (= (:turn state) (-> state :turnorder count) )
      (start-game state)      
      (let [ cp (get (:turnorder state) (:turn state) )]
        (if (some? (re-matches #"AI\d+" cp) )
            (-> state 
                (ai-choose-team cp)
                check-start)
        state))))

(defn choose-team [ state team uname ]
  (-> state
      (assoc-in [:players uname :team] team)
      (update :turn inc)
      check-start))

(defn setup [ plyrs ]
  (let [ turnorder  (-> plyrs shuffle) ]
    (-> gamestate 
      (assoc :status :setup)
      (assoc :highlights {:secret (-> bbdata/highlights shuffle) :discard []})
      (assoc :players (zipmap plyrs (repeat {:public {} :private {} :secret {}})))
      (assoc :turnorder turnorder)
      check-start)))

(defn parseaction [ state ?data uname ]
  ;(prn ?data)
  (case (:action ?data)
    :chooseteam (choose-team state (:team ?data) uname)
    state))