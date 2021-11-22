(ns lobby.bbmodel
  (:require 
    [lobby.bbdata :as bbdata]))

(defonce gamestate {
  :teams (filter #(not= "Freebooter" (:team %)) bbdata/players)
  :turn 0
  :chat []
})

(defn obfuscate [ state uname ]
  (let [rtn (-> state 
              (assoc :players (reduce-kv (fn [m k v] (if (-> v :response some?) (assoc m k (update-in v [:response] dissoc :cb)) (assoc m k v))) {} (:players state))))]
    (prn rtn)
    rtn))

(defn- isai? [ plyr ]
  (some? (re-matches #"AI\d+" plyr) ))

(defn- logaction [ state uname msg ]
  (let [msg {:msg msg :uname uname :timestamp (new java.util.Date)}]
    (update-in state [:chat] conj msg)))

(defonce verbose? false)
;; AI

(defn ai-choose-team [ state ]
  (let [chosen-teams (->> state :players (map #(-> % last :team)) (remove nil?) set) 
        teams        (->> state :teams (map :team) distinct (remove chosen-teams)) ]
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
    (if verbose? (prn "ai-commit" plid hlid zone))
    [ plid hlid zone ]))

; SKILLS

(defn- next-skill [ state uname ] 
  (if verbose? (prn uname "nextskill" (-> state :srcplayer :id))) 
  (update state :activeskill inc))

;; Responses
(def discard {
  :id :discard
  :msg "Discard one player"
  :options :hand
  :cb (fn [ state ?data uname ]
        (let [team (-> state :players (get uname) :team)
              plyr (->> team :private (filter #(= (:id %) (:id ?data))) first)]
          (if verbose? (prn uname "discard" ?data))
          (-> state
              (logaction uname "Discard one")
              (assoc-in [:players uname :team :private] (->> team :private (remove #(= (:id %) (:id ?data))) vec))
              (assoc-in [:players uname :team :discard] (-> team :private :discard (conj plyr))) 
              (update-in [:players uname] dissoc :response)
              (next-skill uname))))})

(defn- get-tackle-spp [ plyr src ]
  (let [frenzymod (if (and src (-> plyr :active set (contains? :frenzy))) 1 0)]
    (if (:prone? plyr)
        (-> plyr :spp last (+ frenzymod))
        (-> plyr :spp first (+ frenzymod)))))
(defn rolldice []
  (rand-nth [:tgtdown :tgtmiss :tackdown]))


(def waiting {
  :id :waiting
  :msg "Waiting for other Players to complete an action"
})

(defn- player-down [state hlid zone plyr coach]
  (if (:prone? plyr)
      (-> state
          (assoc-in [:highlights :public]
            (mapv (fn [hl]
              (if (= (:id hl) hlid)
                  (assoc-in hl [:zone zone] (->> hl :zone zone (remove #(= (:id %) (:id plyr))) vec))
                  hl)) (-> state :highlights :public)))
          (update-in [:players coach :team :injured] conj plyr))
      (assoc-in state [:highlights :public]
        (mapv (fn [hl]
          (if (= (:id hl) hlid)
              (assoc-in hl [:zone zone] 
                (mapv (fn [p]
                  (if (= (:id p) (:id plyr))
                      (assoc p :prone? true)
                      p)) (-> hl :zone zone)))
              hl)) (-> state :highlights :public) ))))

(def tackle-results {
  :id :tackle-results
  :msg "Select Result"
  :options :dice
  :cb (fn [ state ?data uname ] 
        (let [tgtzone   (:tgtzone state)
              srczone   (if (= :a tgtzone) :b :a) ;; won't work for cups
              tgtplayer (:tgtplayer state)
              tgtcoach  (:tgtcoach state)
              srcplayer (:srcplayer state)
              srccoach  (:srccoach state)]
          (if verbose? (prn uname "fn[] tackle-results" ?data))
          (next-skill 
            (case (:dice ?data)
              :tgtdown  (player-down state (:hlid state) tgtzone tgtplayer tgtcoach)
              :tackdown (player-down state (:hlid state) srczone srcplayer srccoach)
              state)
            srccoach)))
})

(defn- get-highlight [ state id ]
  (->> state :highlights :public (filter #(= (:id %) id)) first))
(defn- get-highlightplayer [ state hlid zone plid ]
  (->> (get-highlight state hlid)
       :zone zone
       (filter #(= (:id %) plid)) 
       first))

(def tackle-target {
  :id :tackle-target
  :msg "Choose Target"
  :options :matchup-opponent
  :cb (fn [ state ?data uname ]  ; ?data {:action :response :id n :hl n :zone k :gid k}
        (let [tgtplayer  (get-highlightplayer state (:hlid state) (:zone ?data) (:id ?data))
              tgtcoach   (:coach (reduce-kv (fn [m k v] (if (= (-> v :team :team) (:team tgtplayer)) (assoc m :coach k) m)) {:coach nil} (:players state)))
              srcspp     (get-tackle-spp (:srcplayer state) true)
              tgtspp     (get-tackle-spp tgtplayer false)
              plyr-state (if (< srcspp tgtspp)
                            (-> state 
                                (assoc-in [:players tgtcoach :response] tackle-results)
                                (assoc-in [:players uname :response] waiting))
                            (-> state 
                                (assoc-in [:players uname :response] tackle-results)))]
          (if verbose? (prn uname "fn[] tackle-target" ?data tgtplayer srcspp tgtspp))
          (assoc plyr-state :tgtplayer tgtplayer
                            :tgtzone (:zone ?data)
                            :tgtcoach tgtcoach
                            :srczone nil
                            :srccoach uname
                            :dice (if (= srcspp tgtspp) [ (rolldice) ] [ (rolldice) (rolldice) ]))))
})


(defn end-response [ state uname ]
  (if verbose? (prn "End Response" (-> state :players (get uname) :response :id)))
  (-> state 
      (dissoc :hlid :tgtplayer :tgtzone :tgtcoach :srczone :srccoach :dice)
      (update-in [:players (:tgtcoach state) ] dissoc :response)
      (update-in [:players uname ] dissoc :response)))

;; Skills


(defn- draw-card [ state uname ]
  (let [team (-> state :players (get uname) :team)
        deckempty? (= 0 (-> team :secret count))
        discard    (if deckempty? [] (:discard team))
        draw       (if deckempty? (-> team :discard shuffle) (-> team :secret))
        hand       (:private team)]
    (if verbose? (prn uname "Draw Card"))
    
    (-> state 
      (logaction uname "Draw 1")
      (assoc-in [:players uname :team :discard] discard)
      (assoc-in [:players uname :team :private] (apply conj hand (take 1 draw)))
      (assoc-in [:players uname :team :secret]  (rest draw)))))

(defn- cheat [ state plyr uname ]
  (if verbose? (prn uname "cheat with plyrid" (:id plyr) ))
  (-> state 
      (assoc-in [:highlights :public]
        (mapv
          (fn [hl] 
            (if (= (:id hl) (:hlid state)) 
                (assoc-in hl [:zone (:srczone state)] 
                  (mapv 
                    (fn [p]
                      (if (= (:id p) (:id plyr))
                          (update-in p [:cheat] conj (:cheatid state))
                          p))
                    (-> hl :zone (get (:srczone state)))))
                hl))
          (-> state :highlights :public)))
      (logaction uname "Cheat")
      (update :cheatid inc)
      (next-skill uname)))

(defn- passball [ state plyr uname ]
  (let [carrier (->> state :highlights :public (filter #(= (:id %) (:hlid state))) first :ballcarrier)]
    (if verbose? (prn uname "Pass Ball from" carrier " to " (:id plyr)))
    (-> (if (nil? carrier)
          (assoc-in state [:highlights :public]
            (mapv 
              #(if (= (:id %) (:hlid state))
                    (assoc % :ballcarrier {:zone (:srczone state) :id (:id plyr)})
                    %) (-> state :highlights :public)))
          (if (= (:zone carrier) (:srczone state))
              (assoc-in state [:highlights :public]
                (mapv
                  #(if (= (:id %) (:hlid state))
                        (assoc-in % [:ballcarrier :id] (:id plyr))
                        %) (-> state :highlights :public)))
              (assoc-in state [:highlights :public]
                (mapv
                  #(if (= (:id %) (:hlid state))
                        (dissoc % :ballcarrier)
                        %) (-> state :highlights :public)))))
        (logaction uname "Pass Ball")
        (next-skill uname))))
      
(defn- sprint [ state uname ] 
  (if verbose? (prn uname "sprintaction"))
  (-> state 
      (logaction uname "Sprint")
      (draw-card uname)
      (assoc-in [:players uname :response] discard)))

(defn- tackle [ state plyr uname ]
  (if verbose? (prn uname "tackleaction"))
  (-> state 
      (logaction uname "Tackle")
      (assoc-in [:players uname :response] tackle-target)))

(defn- skill-use [ state uname skill ]
  (let [plyr (-> state :srcplayer)]
    (if verbose? (prn uname skill))
    (-> (case skill
          :pass   (passball state plyr uname)
          :sprint (sprint state uname)
          :cheat  (cheat state plyr uname)
          :tackle (tackle state plyr uname)
          (next-skill state uname)))))

;; ACTIONS
(defn- do-ai-action [ state ap ]
  ;(prn "AI Action " (-> state :players (get ap) :response))
  (if-let [resp (-> state :players (get ap) :response)]
    (case (:id resp)
      :discard (-> state ((:cb resp) {:id (-> state :players (get ap) :team :private shuffle last :id)} ap) (end-response ap))
      ;:tackle-target (-> state ((:cb resp) ))
      (-> state 
          (update-in [:players ap] dissoc :response)
          (next-skill ap)))
    (if-let [srcplayer (:srcplayer state)]
      (skill-use state ap (-> srcplayer :skills (get (:activeskill state))))
      state
      )))

(defn commit-player [ state uname playerid highlightid zone ]
  (let [p (->> (-> state :players (get uname) :team :private) (filter #(= (:id %) playerid)) first)]
    (if verbose? (prn uname "Commit Player" (:id p)))
    (-> state 
      (logaction uname "Commit player to matchup")
      (assoc-in [:players uname :team :private] (remove #(= (:id %) playerid) (-> state :players (get uname) :team :private)))
      (assoc-in [:players uname :state] :skills)
      (assoc :srcplayer p :hlid highlightid :srczone zone :activeskill 0)
      (assoc-in [:highlights :public]
        (mapv 
          #(if (= (:id %) highlightid)
              (update-in % [:zone zone] conj p)
              %) (-> state :highlights :public))))))


(defn end-turn-cleanup [ state ap np pass ]
  (if verbose? (prn "End turn " ap np))
  (-> state
    (logaction ap "End Turn")
    (update :turn inc)
    (assoc :activeplyr np :loop 0)
    (update-in [:players ap] dissoc :state)
    (dissoc :srcplayer)
    (assoc-in [:players ap :passed?] pass)
    (assoc-in [:players np :state] :matchup)
    (logaction np "Start Turn")))


(defn game-loop 
  ([ state pass ]
    (let [ap    (-> state :activeplyr)
          to    (->> state :turnorder (repeat 2) (apply concat) (remove #(-> state :players (get %) :passed true?)) vec)
          apidx (.indexOf to ap)
          np    (get to (inc apidx))
          state (update-in state [:loop] inc)]
      (if verbose? 
        (prn (str ap " Loop - " (:loop state) " state " (-> state :players (get ap) :state) 
                " skill " (-> state :activeskill) "/" (-> state :srcplayer :skills count)
                 " resp " (-> state :players (get ap) :response :id) ) ))
        (if (-> state :loop (> 20))
            state
            (if (or 
                  (-> state :players (get ap) :response some?)
                  (and (-> state :players (get ap) :state (= :skills))
                      (< (-> state :activeskill) 
                          (-> state :srcplayer :skills count))))
                (if (isai? ap) (-> state (do-ai-action ap) game-loop) state)
                (let [et-state (end-turn-cleanup state ap np pass)
                      ap np]
                  (if (-> et-state :players (get ap) :passed?)
                      (-> et-state 
                          (assoc :activeplyr nil)
                          (assoc :status :scoreboard))
                      (if (isai? ap)
                          (game-loop (apply commit-player et-state ap (ai-commit-player et-state ap)))
                          et-state)))))))
  ([ state ] 
    (let [ap (-> state :activeplyr)]
      (game-loop state (-> state :players (get ap) :team :private empty?)))))


;; SETUP 

(defn- start-turn 
  "Start next players turn, trigger ai matchup"
    [ state ]
    (let [ap (:activeplyr state)]
      (if (isai? ap)
          (case (-> state :players (get ap) :state)
            :matchup (game-loop (apply commit-player state ap (ai-commit-player state ap)) false)
            state)
          state)))

(defn- make-team [ teams team ]
  (let [ teamplyrs (->> teams (filter #(= (:team %) team)) (mapv #(assoc % :cheat [])) shuffle) ]
    (hash-map
      :alliance (-> teamplyrs first :alliance)
      :team (-> teamplyrs first :team)
      :private (take 6 teamplyrs)
      :secret  (nthrest teamplyrs 6)
      :discard []
      :injured [] )))

(defn start-game [ state ]
  (let [nhl (-> state :turnorder count)]
    (-> state 
        (assoc :status     :started 
               :turn       0 
               :cheatid    0
               :activeplyr (-> state :turnorder first)
               :loop       0)
        (dissoc :teams)
        ; set player hands 
        (assoc :players 
          (reduce-kv 
            (fn [m k v]
              (assoc m k {:team (make-team (:teams state) (:team v))})) {} (:players state)))
        (assoc-in [:players (-> state :turnorder first) :state] :matchup)
        ; VV Replace with draw-highlights? VV ;
        (assoc-in [:highlights :public] (->> state :highlights :secret (take nhl) (map #(assoc % :zone {:a [] :b []}))))
        (assoc-in [:highlights :secret] (-> state :highlights :secret (nthrest nhl)))
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
  (if verbose? (prn uname ?data))
  (case (:action ?data)
    :chooseteam   (-> state (choose-team (:team ?data) uname) check-start)
    :commitplayer (-> state (commit-player uname (:plid ?data) (:hlid ?data) (:zone ?data)) game-loop)
    :pass         (-> state (game-loop true))
    :skill-use    (-> state (skill-use uname (:skill ?data)) game-loop)
    :skill-pass   (-> state (skill-use uname nil) game-loop)
    :response     (-> state ((-> state :players (get uname) :response :cb) ?data uname) game-loop)
    state))