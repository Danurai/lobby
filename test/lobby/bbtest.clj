(ns lobby.bbtest
  (:require
    [expectations :refer :all]
    [lobby.model :as model]
    [lobby.bbdata :as data]
    [lobby.bbmodel :as bbmodel]
    [lobby.lobbytest :refer :all]))


          
(def skill-test-state {
  :turn 0
  :loop 0
  :status :started
  :cheatid 0
  :activeplyr "p1"
  :turnorder ["p1" "p2"]
  :players {
    "p1" {:state :skills :team {:team "Human" :private (->> data/players (take 6) vec) :discard [] :injured [] :secret (->  (take 12 data/players) (nthrest 6) vec)  }}
    "p2" {:state :skills :team {:team "WoodElf" :private (->> (nthrest data/players 12) (take 6) vec) :discard [] :injured [] :secret (-> (take 12 (nthrest data/players 12)) (nthrest 6) vec)  }}
  }
  :highlights {
    :public (->> data/highlights (take 2) (mapv #(assoc % :zone {:a [] :b []} :ballcarrier nil)) )
  }
})

(def ai-skill-test-state {
  :turn 0
  :status :started
  :cheatid 0
  :loop 0
  :activeplyr "AI1234"
  :turnorder ["AI1234" "p2"]
  :players {
    "AI1234" {:state :matchup :team {:team "Human" :private (->> data/players (take 6) vec) :discard [] :secret (-> data/players (nthrest 6) vec)  }}
  }
  :highlights {
    :public (->> data/highlights (take 2) (mapv #(assoc % :zone {:a [] :b []} )) )
  }
})
    
;(defn- twoplyrgame []
;  (-> ["p1" "p2"]
;      bbmodel/setup 
;      (bbmodel/parseaction {:action :chooseteam :team "Human"} "p1")
;      (bbmodel/parseaction {:action :chooseteam :team "Orc"} "p2"))) 
;
;(defn- start-test [ state ] 
;  ;(prn "- Test -") 
;  state)
;
;; Create Game, select teams.
;;; two players
;(expect ["p1" "p2"] (-> twoplyrgame :players keys))
;;; two teams
;(expect 2 (->> twoplyrgame :players vals (map :team) count))
;;; Turn = 0
;(expect 0 (-> twoplyrgame :turn))
;;; remove all other team info
;(expect nil (-> twoplyrgame :teams))
;;; Freebooters
;;;
;;; Draw highlights !TODO 4x for 2p game!
;(expect #(= (-> % :turnorder count) (-> :highlights :public count) ) (twoplyrgame))
;;; 6 private players and 6 secret players each
;(expect 6 (-> twoplyrgame :players (get "p1") :team :private count))
;(expect 6 (-> twoplyrgame :players (get "p1") :team :secret count))
;(expect 6 (-> twoplyrgame :players (get "p2") :team :private count))
;(expect 6 (-> twoplyrgame :players (get "p2") :team :secret count))
;
;;; set team alliance
;(expect ["Human" "OWA"]
;  (let [gs twoplyrgame
;        team (-> gs :players (get "p1") :team)]
;    [ (:team team) (:alliance team) ]))
;;; set team alliance
;(expect ["Orc" "CWC"]
;  (let [gs twoplyrgame
;        team (-> gs :players (get "p2") :team)]
;    [ (:team team) (:alliance team) ]))
;
;; :matchup status on first players turn
;;; tested multiple times later in skill tests
;(expect :matchup
;  (let [gs twoplyrgame
;        p1 (-> gs :turnorder first)]
;    (-> gs :players (get p1) :state)))
;
;; Start Game, commit player to matchup - 1 player in zone
;(expect 1
;  (let [gs (assoc twoplyrgame :turnorder ["p1" "p2"])
;        h1 (-> gs :highlights :public first)
;        p1 (-> gs :players (get "p1") :team :private first)]
;    (-> gs 
;        (bbmodel/parseaction {:action :commitplayer :plid (:id p1) :hlid (:id h1) :zone :a} "p1")
;        :highlights :public first :zone :a count)))
;; Start Game, commit player to matchup - 5 players remaining
;(expect 5
;  (let [gs (assoc twoplyrgame :turnorder ["p1" "p2"])
;        h1 (-> gs :highlights :public first)
;        p1 (-> gs :players (get "p1") :team :private first)]
;    (-> gs 
;        (bbmodel/parseaction {:action :commitplayer :plid (:id p1) :hlid (:id h1) :zone :a} "p1")
;        :players (get "p1") :team :private count)))
;
;; Pass
;(expect true
;  (let [gs twoplyrgame
;        p1 (-> gs :turnorder first)]
;    (-> gs
;        (bbmodel/parseaction {:action :pass} p1)
;        :players (get p1) :passed?)))
;; Both Pass
;(expect :scoreboard
;  (let [gs twoplyrgame
;        p1 (-> gs :turnorder first)
;        p2 (-> gs :turnorder last)]
;    (-> gs
;        (bbmodel/parseaction {:action :pass} p1)
;        (bbmodel/parseaction {:action :pass} p2)
;        :status)))
;
;; Both out of players
;;(expect :scoreboard
;;  (let [gs twoplyrgame
;;        p1 (-> gs :turnorder first)
;;        p2 (-> gs :turnorder last)
;;        hl (-> gs :highlights :public first)]
;;    (-> 
;;      (reduce 
;;        (fn [state plyr]
;;          (-> state
;;              (bbmodel/parseaction {:action :commitplayer :plid (-> state :players (get p1) :team :private first :id) :hlid (:id hl) :zone :a} p1)
;;              (bbmodel/parseaction {:action :commitplayer :plid (-> state :players (get p2) :team :private first :id) :hlid (:id hl) :zone :b} p2)))
;;        gs
;;        (range 6))
;;      :status))) 
;
;
;
;
;; AI Tests
;
;; AI-CHOOSE-TEAM
;(expect "Orc"
;  (let [teststate {
;              :teams [ {:team "Human"} {:team "Orc"} ]
;              :players {
;                "AI1" {:team "Human"}
;                "AI2" {} } }]
;    (bbmodel/ai-choose-team teststate)))
;
;; AI-COMMIT-PLAYER [highlightid playerid zone]
;;; identify zones
;
;(expect true
;  (let [teststate twoplyrgame 
;        players (map :id (-> teststate :players (get "p1") :team :private)) 
;        highlights (->> teststate :highlights (map) :id)
;        ai-commit (-> teststate (bbmodel/ai-commit-player "p1"))]
;      (and (-> ai-commit first number?)
;           (-> ai-commit second number?)
;           (or (-> ai-commit last (= :a))(-> ai-commit last (= :b))))))
;
;; SKILL TESTS
;
;;; commit a player 
;;; - state is now :skills
;(expect :skills
;  (-> skill-test-state
;      start-test
;      (bbmodel/parseaction {:action :commitplayer :plid 4 :hlid 0 :zone :a} "p1")
;      :players (get "p1") :state))
;;; - p1 is still active player
;(expect "p1"
;  (-> skill-test-state
;      start-test
;      (bbmodel/parseaction {:action :commitplayer :plid 4 :hlid 0 :zone :a} "p1")
;      :activeplyr
;      ))
;
;
;;; CHEAT
;;; - sklll # increased
;(expect 1
;  (-> skill-test-state
;      start-test
;      (update-in [:players "p1" :team :private 0 :skills] conj :cheat) ; Force CHEAT + SPRINT
;      (update-in [:players "p1" :team :private 0 :skills] conj :sprint) ; Force CHEAT + SPRINT
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (bbmodel/parseaction {:action :skill-use :skill :cheat} "p1")
;      :activeskill))
;;; - next cheat token id increased
;(expect 1
;  (-> skill-test-state
;      start-test
;      (update-in [:players "p1" :team :private 0 :skills] conj :cheat) ; Force CHEAT 
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (bbmodel/parseaction {:action :skill-use :skill :cheat} "p1")
;      :cheatid))
;;; - cheating player has a cheat token
;(expect [0]
;  (-> skill-test-state
;      start-test
;      (update-in [:players "p1" :team :private 0 :skills] conj :cheat) ; Force CHEAT
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (bbmodel/parseaction {:action :skill-use :skill :cheat} "p1")
;      :highlights :public first :zone :a first :cheat ))
;;; - skill use completed, P2 is now active
;(expect :matchup
;  (-> skill-test-state
;      start-test
;      (update-in [:players "p1" :team :private 0 :skills] conj :cheat) ; Force CHEAT
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (bbmodel/parseaction {:action :skill-use :skill :cheat} "p1")
;      :players (get "p2") :state))
;;; - skill use completed, P1 cleanup
;(expect {}
;  (-> skill-test-state
;      start-test
;      (update-in [:players "p1" :team :private 0 :skills] conj :cheat) ; Force CHEAT
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (bbmodel/parseaction {:action :skill-use :skill :cheat} "p1")
;      :players (get "p1") (select-keys [:state :committed])))
;
;;; PASS
;;; - ball at mid-field, player is ball owner
;(expect {:zone :a :id 0}
;  (-> skill-test-state
;      start-test
;      (update-in [:players "p1" :team :private 0 :skills] conj :pass) ; Force PASS
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (bbmodel/parseaction {:action :skill-use :skill :pass} "p1")
;      :highlights :public first :ballcarrier ))
;
;;; - oppponent is ball carrier, ball is at mid-field
;(expect nil
;  (-> skill-test-state
;      start-test
;      (update-in [:players "p1" :team :private 0 :skills] conj :pass) ; Force PASS
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (bbmodel/parseaction {:action :skill-use :skill :pass} "p1")
;      (update-in [:players "p2" :team :private 0 :skills] conj :pass) ; Force PASS
;      (bbmodel/parseaction {:action :commitplayer :plid 12 :hlid 0 :zone :b} "p2")
;      (bbmodel/parseaction {:action :skill-use :skill :pass} "p2")
;      :highlights :public first :ballcarrier ))
;;; - teammate is ball carrier, ball moves to passing player
;(expect {:zone :a :id 0}
;  (-> skill-test-state
;      start-test
;      (assoc-in [:highlights :public 0 :ballcarrier] {:zone :a :id 5})  ; Dummy teammmate
;      (update-in [:players "p1" :team :private 0 :skills] conj :pass) ; Force PASS
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (bbmodel/parseaction {:action :skill-use :skill :pass} "p1")
;      :highlights :public first :ballcarrier ))
;;;; - pass complete - end of player turn
;(expect nil
;  (-> skill-test-state
;      start-test
;      (update-in [:players "p1" :team :private 0 :skills] conj :pass) ; Force PASS
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (bbmodel/parseaction {:action :skill-use :skill :pass} "p1")
;      :players (get "p1") :state ))
;;;; - skill use completed, P2 is now active
;(expect :matchup
;  (-> skill-test-state
;      start-test
;      (update-in [:players "p1" :team :private 0 :skills] conj :pass) ; Force PASS
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (bbmodel/parseaction {:action :skill-use :skill :pass} "p1")
;      :players (get "p2") :state ))
;
;;; SPRINT 
;;; - sprint started, :skills state
;(expect :skills
;  (-> skill-test-state
;      start-test
;      (update-in [:players "p1" :team :private 0 :skills] conj :sprint) ; Force SPRINT
;      (bbmodel/parseaction {:action :committed :plid 0 :hl 0 :zone :a} "p1")
;      :players (get "p1") :state))
;;; - sprint started - player draws 1
;(expect 6
;  (-> skill-test-state
;      start-test
;      (update-in [:players "p1" :team :private 0 :skills] conj :sprint) ; Force SPRINT
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hl 0 :zone :a} "p1")
;      (bbmodel/parseaction {:action :skill-use :skill :sprint} "p1")
;      :players (get "p1") :team :private count))
;;; - Sprint - Draw with forced empty deck
;(expect 6
;  (-> skill-test-state
;      start-test
;      (assoc-in [:players "p1" :team :discard] (-> skill-test-state :players (get "p1") :team :secret) ) ;put deck in discard
;      (assoc-in [:players "p1" :team :secret] [] ) ;Empty Deck (:secret)
;      (update-in [:players "p1" :team :private 0 :skills] conj :sprint) ; Force SPRINT
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hl 0 :zone :a} "p1")
;      (bbmodel/parseaction {:action :skill-use :skill :sprint} "p1")
;      :players (get "p1") :team :private count))
;;; - sprint started - player action required
;(expect true
;  (-> skill-test-state
;      start-test
;      (assoc-in [:players "p1" :team :discard] (-> skill-test-state :players (get "p1") :team :secret) ) ;put deck in discard
;      (assoc-in [:players "p1" :team :secret] [] ) ;Empty Deck (:secret)
;      (update-in [:players "p1" :team :private 0 :skills] conj :sprint) ; Force SPRINT
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hl 0 :zone :a} "p1")
;      (bbmodel/parseaction {:action :skill-use :skill :sprint} "p1")
;      :players (get "p1") :response some?))
;;; - sprint started - discard required
;(expect :discard
;  (-> skill-test-state
;      start-test
;      (update-in [:players "p1" :team :private 0 :skills] conj :sprint) ; Force SPRINT
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hl 0 :zone :a} "p1")
;      (bbmodel/parseaction {:action :skill-use :skill :sprint} "p1")
;      :players (get "p1") :response :id))
;;; - discard a card - 5 in hand
;(expect 5
;  (-> skill-test-state
;      start-test
;      (update-in [:players "p1" :team :private 0 :skills] conj :sprint) ; Force SPRINT
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hl 0 :zone :a} "p1")
;      (bbmodel/parseaction {:action :skill-use :skill :sprint} "p1")
;      (bbmodel/parseaction {:action :response :id 1} "p1")
;      :players (get "p1") :team :private count ))
;;; - discard a card - 1 in discard
;(expect 1
;  (-> skill-test-state
;      start-test
;      (update-in [:players "p1" :team :private 0 :skills] conj :sprint) ; Force SPRINT
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hl 0 :zone :a} "p1")
;      (bbmodel/parseaction {:action :skill-use :skill :sprint} "p1")
;      (bbmodel/parseaction {:action :response :id 1} "p1")
;      :players (get "p1") :team :discard count ))
;;; - sprint / discard a card - actions complete
;(expect nil
;  (-> skill-test-state
;      start-test
;      (update-in [:players "p1" :team :private 0 :skills] conj :sprint) ; Force SPRINT
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hl 0 :zone :a} "p1")
;      (bbmodel/parseaction {:action :skill-use :skill :sprint} "p1")
;      (bbmodel/parseaction {:action :response :id 1} "p1")
;      :players (get "p1") :action ))
;;; - sprint / discard a card - P2 turn
;(expect :matchup
;  (-> skill-test-state
;      start-test
;      (update-in [:players "p1" :team :private 0 :skills] conj :sprint) ; Force SPRINT
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hl 0 :zone :a} "p1")
;      (bbmodel/parseaction {:action :skill-use :skill :sprint} "p1")
;      (bbmodel/parseaction {:action :response :id 1} "p1")
;      :players (get "p2") :state ))
;
;
;;; AI Commits
;
;;; Commit a player with no skills to a matchup - turn over
;(expect nil 
;  (-> ai-skill-test-state
;      (bbmodel/commit-player  "AI1234" 0 0 :a)
;      (bbmodel/game-loop  false)
;      :players (get "AI1234") :state))
;;; Commit a player with no skills to a matchup - turn over
;(expect :matchup
;  (-> ai-skill-test-state
;      (bbmodel/commit-player  "AI1234" 0 0 :a)
;      (bbmodel/game-loop  false)
;      :players (get "p2") :state))
;
;;; Commit a player with :cheat to a matchup - turn over
;(expect nil
;  (-> ai-skill-test-state
;      (update-in [:players "AI1234" :team :private 0 :skills] conj :cheat)  ; FORCE CHEAT
;      (bbmodel/commit-player  "AI1234" 0 0 :a)
;      (bbmodel/game-loop false)
;      :players (get "AI1234") :state))
;;; Commit a player with :cheat to a matchup - turn over
;(expect :matchup
;  (-> ai-skill-test-state
;      (update-in [:players "AI1234" :team :private 0 :skills] conj :cheat)  ; FORCE CHEAT
;      (bbmodel/commit-player  "AI1234" 0 0 :a)
;      (bbmodel/game-loop  false)
;      :players (get "p2") :state))
;; Commit a player with :cheat to a matchup - has a cheat token
;(expect [0]
;  (-> ai-skill-test-state
;      (update-in [:players "AI1234" :team :private 0 :skills] conj :cheat)  ; FORCE CHEAT
;      (bbmodel/commit-player  "AI1234" 0 0 :a)
;      (bbmodel/game-loop  false)
;      :highlights :public first :zone :a first :cheat))
;
;;; Commit a player with :pass to a matchup - turn over
;(expect nil
;  (-> ai-skill-test-state
;      (bbmodel/commit-player  "AI1234" 4 0 :a)
;      (bbmodel/game-loop  false)
;      :players (get "AI1234") :state))
;;; Commit a player with :pass to a matchup - turn over
;(expect :matchup
;  (-> ai-skill-test-state
;      (bbmodel/commit-player  "AI1234" 4 0 :a)
;      (bbmodel/game-loop  false)
;      :players (get "p2") :state))
;;; Commit a player with :pass to a matchup - ball taken
;(expect {:zone :a :id 4}
;  (-> ai-skill-test-state
;      (bbmodel/commit-player  "AI1234" 4 0 :a)
;      (bbmodel/game-loop  false)
;      :highlights :public first :ballcarrier))
;;; Commit a player with :pass to a matchup - ball moved??
;
;;; Commit a player with :sprint to a matchup - turn over
;(expect nil
;  (-> ai-skill-test-state
;      (update-in [:players "AI1234" :team :private 0 :skills] conj :sprint)  ; FORCE SPRINT
;      (bbmodel/commit-player  "AI1234" 0 0 :a)
;      (bbmodel/game-loop  false)
;      :players (get "AI1234") :state))
;;; Commit a player with :sprint to a matchup - turn over
;(expect :matchup
;  (-> ai-skill-test-state
;      (update-in [:players "AI1234" :team :private 0 :skills] conj :sprint)  ; FORCE SPRINT
;      (bbmodel/commit-player  "AI1234" 0 0 :a)
;      (bbmodel/game-loop  false)
;      :players (get "p2") :state))
;;; Commit a player with :sprint to a matchup - hand size = 5
;(expect 5
;  (-> ai-skill-test-state
;      (update-in [:players "AI1234" :team :private 0 :skills] conj :sprint)  ; FORCE SPRINT
;      (bbmodel/commit-player  "AI1234" 0 0 :a)
;      (bbmodel/game-loop  false)
;      :players (get "AI1234") :team :private count))
;;; Commit a player with :sprint to a matchup - discard size = 1
;(expect 1
;  (-> ai-skill-test-state
;      (update-in [:players "AI1234" :team :private 0 :skills] conj :sprint)  ; FORCE SPRINT
;      (bbmodel/commit-player  "AI1234" 0 0 :a)
;      (bbmodel/game-loop  false)
;      :players (get "AI1234") :team :discard count))
;    
;
;
;; OBFUSCATE 
;;; Removes Callback Function from :response
;(expect nil
;  (-> skill-test-state
;      start-test
;      (update-in [:players "p1" :team :private 0 :skills] conj :cheat) ; Force CHEAT
;      (update-in [:players "p1" :team :private 0 :skills] conj :tackle) ; Force TACKLE
;      (update-in [:players "p1" :team :private 0 :skills] conj :sprint) ; Force SPRINT
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (bbmodel/parseaction {:action :skill-use :skill :cheat} "p1")
;      (bbmodel/parseaction {:action :skill-pass} "p1")
;      (bbmodel/parseaction {:action :skill-use :skill :sprint} "p1")
;      (bbmodel/obfuscate "p1")
;      :players (get "p1") :response :cb))
;
;;; TACKLE
;
;; Tackle Response - :tackle
;(expect :tackle-target
;  (-> skill-test-state
;      start-test
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (update-in [:players "p2" :team :private 0 :skills] conj :tackle) ; Force TACKLE
;      (bbmodel/parseaction {:action :commitplayer :plid 12 :hlid 0 :zone :b} "p2")
;      (bbmodel/parseaction {:action :skill-use :skill :tackle} "p2")
;      :players (get "p2") :response :id ))
;;; Tackle Response - No Target? - Prevent from client? 
;;; Tackle Response - Choose Target (SPP=)
;(expect :tackle-results
;  (-> skill-test-state
;      start-test
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (update-in [:players "p2" :team :private 0 :skills] conj :tackle) ; Force TACKLE
;      (bbmodel/parseaction {:action :commitplayer :plid 12 :hlid 0 :zone :b} "p2")
;      (bbmodel/parseaction {:action :skill-use :skill :tackle} "p2")
;      (bbmodel/parseaction {:action :response :id 0 :hlid 0 :zone :a} "p2")
;      :players (get "p2") :response :id ))
;;; Tackle Response - SPP> 2 dice
;(expect 2
;  (-> skill-test-state
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (update-in [:players "p2" :team :private 0 :skills] conj :tackle) ; Force TACKLE
;      (assoc-in [:players "p2" :team :private 0 :spp ] [ 99 99 ]) ; Force SPP
;      (bbmodel/parseaction {:action :commitplayer :plid 12 :hlid 0 :zone :b} "p2")
;      (bbmodel/parseaction {:action :skill-use :skill :tackle} "p2")
;      (bbmodel/parseaction {:action :response :id 0 :hlid 0 :zone :a} "p2")
;      :dice count))
;;; Tackle Response - SPP> 2 p1 response
;;; Tackle Response - SPP< 2 dice  
;(expect 2
;  (-> skill-test-state
;      (assoc-in [:players "p1" :team :private 0 :spp ] [ 99 99 ]) ; Force SPP
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (update-in [:players "p2" :team :private 0 :skills] conj :tackle) ; Force TACKLE
;      (bbmodel/parseaction {:action :commitplayer :plid 12 :hlid 0 :zone :b} "p2")
;      (bbmodel/parseaction {:action :skill-use :skill :tackle} "p2")
;      (bbmodel/parseaction {:action :response :id 0 :hlid 0 :zone :a} "p2")
;      :dice count))
;;; Tackle Response - SPP< 2 dice player waiting
;(expect :waiting
;  (-> skill-test-state
;      (assoc-in [:players "p1" :team :private 0 :spp ] [ 99 99 ]) ; Force SPP
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (update-in [:players "p2" :team :private 0 :skills] conj :tackle) ; Force TACKLE
;      (bbmodel/parseaction {:action :commitplayer :plid 12 :hlid 0 :zone :b} "p2")
;      (bbmodel/parseaction {:action :skill-use :skill :tackle} "p2")
;      (bbmodel/parseaction {:action :response :id 0 :hlid 0 :zone :a} "p2")
;      :players (get "p2") :response :id))
;;; Tackle Response - SPP< 2 p2 reponse
;
;;; Tackle Response - SPP= 1 dice  
;(expect 1
;  (-> skill-test-state
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (update-in [:players "p2" :team :private 0 :skills] conj :tackle) ; Force TACKLE
;      (bbmodel/parseaction {:action :commitplayer :plid 12 :hlid 0 :zone :b} "p2")
;      (bbmodel/parseaction {:action :skill-use :skill :tackle} "p2")
;      (bbmodel/parseaction {:action :response :id 0 :hlid 0 :zone :a} "p2")
;      :dice count))
;;; Tackle Response - SPP= p1 response
;
;;;; Tackle Result - Target Down (prone)
;(expect true
;  (-> skill-test-state
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (update-in [:players "p2" :team :private 0 :skills] conj :tackle) ; Force TACKLE
;      (bbmodel/parseaction {:action :commitplayer :plid 12 :hlid 0 :zone :b} "p2")
;      (bbmodel/parseaction {:action :skill-use :skill :tackle} "p2")
;      (bbmodel/parseaction {:action :response :id 0 :hlid 0 :zone :a} "p2")
;      (bbmodel/parseaction {:action :response :dice :tgtdown} "p2")
;      :highlights :public first :zone :a first :prone? ))
;;;; Tackle Result - Target Down (injured)
;(expect nil
;  (-> skill-test-state
;      start-test
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (assoc-in [:highlights :public 0 :zone :a 0 :prone?] true) ; Force PRONE
;      (update-in [:players "p2" :team :private 0 :skills] conj :tackle) ; Force TACKLE
;      (bbmodel/parseaction {:action :commitplayer :plid 12 :hlid 0 :zone :b} "p2")
;      (bbmodel/parseaction {:action :skill-use :skill :tackle} "p2")
;      (bbmodel/parseaction {:action :response :id 0 :hlid 0 :zone :a} "p2")
;      (bbmodel/parseaction {:action :response :dice :tgtdown} "p2")
;      :highlights :public first :zone :a first))
;;; Tackle Result - Target Down (injured)
;(expect 1
;  (-> skill-test-state
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (assoc-in [:highlights :public 0 :zone :a 0 :prone?] true) ; Force PRONE
;      (update-in [:players "p2" :team :private 0 :skills] conj :tackle) ; Force TACKLE
;      (bbmodel/parseaction {:action :commitplayer :plid 12 :hlid 0 :zone :b} "p2")
;      (bbmodel/parseaction {:action :skill-use :skill :tackle} "p2")
;      (bbmodel/parseaction {:action :response :id 0 :hlid 0 :zone :a} "p2")
;      (bbmodel/parseaction {:action :response :dice :tgtdown} "p2")
;      :players (get "p1") :team :injured count))
;
;;; Tackle Result - Tackler Down (Prone)
;(expect true
;  (-> skill-test-state
;      (assoc-in [:players "p1" :team :private 0 :spp ] [ 99 99 ]) ; Force SPP
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (update-in [:players "p2" :team :private 0 :skills] conj :tackle) ; Force TACKLE
;      (bbmodel/parseaction {:action :commitplayer :plid 12 :hlid 0 :zone :b} "p2")
;      (bbmodel/parseaction {:action :skill-use :skill :tackle} "p2")
;      (bbmodel/parseaction {:action :response :id 0 :hlid 0 :zone :a} "p2")
;      (bbmodel/parseaction {:action :response :dice :tackdown} "p1")
;      :highlights :public first :zone :b first :prone? ))
;;; Tackle Result - Tackler Down (Injured) ## EDGE CASE WHEN A PRONE PLAYER CAN TACKLE - MINOTAUR?
;(expect nil
;  (-> skill-test-state
;      (assoc-in [:players "p1" :team :private 0 :spp ] [ 99 99 ]) ; Force SPP
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (update-in [:players "p2" :team :private 0 :skills] conj :tackle) ; Force TACKLE
;      (assoc-in [:players "p2" :team :private 0 :prone?] true) ; Force PRONE
;      (bbmodel/parseaction {:action :commitplayer :plid 12 :hlid 0 :zone :b} "p2")
;      (bbmodel/parseaction {:action :skill-use :skill :tackle} "p2")
;      (bbmodel/parseaction {:action :response :id 0 :hlid 0 :zone :a} "p2")
;      (bbmodel/parseaction {:action :response :dice :tackdown} "p1")
;      :highlights :public first :zone :b first))
;;; Tackle Result - Tackler Down (Injured) ## EDGE CASE WHEN A PRONE PLAYER CAN TACKLE - MINOTAUR?
;(expect 1
;  (-> skill-test-state
;      (assoc-in [:players "p1" :team :private 0 :spp ] [ 99 99 ]) ; Force SPP
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (update-in [:players "p2" :team :private 0 :skills] conj :tackle) ; Force TACKLE
;      (assoc-in [:players "p2" :team :private 0 :prone?] true) ; Force PRONE
;      (bbmodel/parseaction {:action :commitplayer :plid 12 :hlid 0 :zone :b} "p2")
;      (bbmodel/parseaction {:action :skill-use :skill :tackle} "p2")
;      (bbmodel/parseaction {:action :response :id 0 :hlid 0 :zone :a} "p2")
;      (bbmodel/parseaction {:action :response :dice :tackdown} "p1")
;      :players (get "p2") :team :injured count))
;
;;; Tackle Result - Target Missed
;(expect nil 
;  (-> skill-test-state
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (update-in [:players "p2" :team :private 0 :skills] conj :tackle) ; Force TACKLE
;      (bbmodel/parseaction {:action :commitplayer :plid 12 :hlid 0 :zone :b} "p2")
;      (bbmodel/parseaction {:action :skill-use :skill :tackle} "p2")
;      (bbmodel/parseaction {:action :response :id 0 :hlid 0 :zone :a} "p2")
;      (bbmodel/parseaction {:action :response :dice :tgtmiss} "p2")
;      :highlights :public first :zone :a first :prone?))
;
;;; Ball Carrier Down
;
;
;
;
;
;;; Dauntless SPP< 1 dice
;
;;; Dirty Player - +1 
;
;;; Dodge - Force Reroll
;
;;; Dump off - pass the ball instead of dropping 
;
;;; Fend - Stand another player if downed
;
;;; Frenzy - When this player attempts a tackle, increase his Star Power by one during the tackle attempt.
;(expect 2
;  (-> skill-test-state
;      (update-in [:players "p2" :team :private 0 :skills] conj :tackle) ; Force TACKLE
;      (update-in [:players "p2" :team :private 0 :active] conj :frenzy) ; Force FRENZY
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (bbmodel/parseaction {:action :commitplayer :plid 12 :hlid 0 :zone :b} "p2")
;      (bbmodel/parseaction {:action :skill-use :skill :tackle} "p2")
;      (bbmodel/parseaction {:action :response :id 0 :hlid 0 :zone :a} "p2")
;      :dice count))
;;;; Frenzy doesn't count for target
;(expect 1
;  (-> skill-test-state
;      (update-in [:players "p1" :team :private 0 :active] conj :frenzy) ; Force FRENZY
;      (bbmodel/parseaction {:action :commitplayer :plid 0 :hlid 0 :zone :a} "p1")
;      (update-in [:players "p2" :team :private 0 :skills] conj :tackle) ; Force TACKLE
;      (bbmodel/parseaction {:action :commitplayer :plid 12 :hlid 0 :zone :b} "p2")
;      (bbmodel/parseaction {:action :skill-use :skill :tackle} "p2")
;      (bbmodel/parseaction {:action :response :id 0 :hlid 0 :zone :a} "p2")
;      :dice count))
;      
;;; Guard - downed instead of another player
;
;;; Juggerneaut - cannot be GUARD ed
;
;;; Nerves of Steel - +1 SPP when ball carrier
;
;;; Piling On - +1 Tackle when [target down, target down]
;
;;; Stand Firm - Can't be tackled if ball carrier
;
;;; Strip Ball - Place ball at midfield instead of Tackle - (Client Option)
;
;;; Sure Hands - don't drop the ball if downed
;
;;; Throw Team Mate
;
;
;
;;; Wardancer - successful tackle - sprint (Dirty Player
;;; Rat Ogre - Cheat Filter 1)
;;; Minotaur - Tackle Back
;
;
;;; AI Tackle
;(expect nil
;    (-> ai-skill-test-state
;        start-test
;        (update-in [:players "AI1234" :team :private 0 :skills] conj :tackle)  ; FORCE TACKLE
;        (bbmodel/commit-player  "AI1234" 0 0 :a)
;        (bbmodel/game-loop false)
;        :players (get "AI1234") :state))
;
;
;;; Scoreboard Phase
;
;
;