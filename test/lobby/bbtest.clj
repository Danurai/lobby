(ns lobby.bbtest
  (:require
    [expectations :refer :all]
    [lobby.model :as model]
    [lobby.bbmodel :as bbmodel]
    [lobby.lobbytest :refer :all]))

(def twoplyrgame 
  (-> ["p1" "p2"]
      bbmodel/setup 
      (bbmodel/parseaction {:action :chooseteam :team "Human"} "p1")
      (bbmodel/parseaction {:action :chooseteam :team "Orc"} "p2"))) 

; Create Game, select teams.
;; two players
(expect ["p1" "p2"] (-> twoplyrgame :players keys))
;; two teams
(expect 2 (->> twoplyrgame :players vals (map :team) count))
;; Turn = 0
(expect 0 (-> twoplyrgame :turn))
;; remove all other team info
(expect nil (-> twoplyrgame :teams))
;; Freebooters
;;
;; Draw highlights !TODO 4x for 2p game!
(expect #(= (-> % :turnorder count) (-> :highlights :public count) ) (twoplyrgame))
;; 6 private players and 6 secret players each
(expect 6 (-> twoplyrgame :players (get "p1") :team :private count))
(expect 6 (-> twoplyrgame :players (get "p1") :team :secret count))
(expect 6 (-> twoplyrgame :players (get "p2") :team :private count))
(expect 6 (-> twoplyrgame :players (get "p2") :team :secret count))

;; set team alliance
(expect ["Human" "OWA"]
  (let [gs twoplyrgame
        team (-> gs :players (get "p1") :team)]
    [ (:team team) (:alliance team) ]))
;; set team alliance
(expect ["Orc" "CWC"]
  (let [gs twoplyrgame
        team (-> gs :players (get "p2") :team)]
    [ (:team team) (:alliance team) ]))

; :matchup status on players turn
(expect :matchup
  (let [gs twoplyrgame
        p1 (-> gs :turnorder first)]
    (-> gs :players (get p1) :state)))

(expect :matchup
  (let [gs twoplyrgame
        p1 (-> gs :turnorder first)
        p2 (-> gs :turnorder second)
        h1 (-> gs :highlights :public first)
        p1 (-> gs :players (get p1) :team :private first)]
    (-> gs 
        (bbmodel/commit-player p1 (:id p1) (:id h1) :a)
        :players (get p2) :state)))

; Start Game, commit player to matchup - 1 player in zone
(expect 1
  (let [gs (assoc twoplyrgame :turnorder ["p1" "p2"])
        h1 (-> gs :highlights :public first)
        p1 (-> gs :players (get "p1") :team :private first)]
    (-> gs 
        (bbmodel/parseaction {:action :commitplayer :plid (:id p1) :hlid (:id h1) :zone :a} "p1")
        :highlights :public first :zone :a count)))
; Start Game, commit player to matchup - 5 players remaining
(expect 5
  (let [gs (assoc twoplyrgame :turnorder ["p1" "p2"])
        h1 (-> gs :highlights :public first)
        p1 (-> gs :players (get "p1") :team :private first)]
    (-> gs 
        (bbmodel/parseaction {:action :commitplayer :plid (:id p1) :hlid (:id h1) :zone :a} "p1")
        :players (get "p1") :team :private count)))


; Pass
(expect true
  (let [gs twoplyrgame
        p1 (-> gs :turnorder first)]
    (-> gs
        (bbmodel/parseaction {:action :pass} p1)
        :players (get p1) :passed?)))

(expect :scoreboard
  (let [gs twoplyrgame
        p1 (-> gs :turnorder first)
        p2 (-> gs :turnorder last)
        hl (-> gs :highlights :public first)]
    (-> 
      (reduce 
        (fn [state plyr]
          (-> state
              (bbmodel/parseaction {:action :commitplayer :plid (-> state :players (get p1) :team :private first :id) :hlid (:id hl) :zone :a} p1)
              (bbmodel/parseaction {:action :commitplayer :plid (-> state :players (get p2) :team :private first :id) :hlid (:id hl) :zone :b} p2)))
        gs
        (range 6))
      :status))) 

; Commit-Player

; AI Tests
;(def twoplyrgamewithai   
;  (let [gs (->> (bbmodel/setup ["p1" "AI1234"]))
;        p1team (if (= (-> gs :players (get "AI1234") :team) "Orc") "Human" "Orc")]
;    (bbmodel/parseaction gs {:action :chooseteam :team p1team} "p1") ))

; AI-CHOOSE-TEAM
(expect "Orc"
  (let [teststate {
              :teams [ {:team "Human"} {:team "Orc"} ]
              :players {
                "AI1" {:team "Human"}
                "AI2" {} } }]
    (bbmodel/ai-choose-team teststate)))

; AI-COMMIT-PLAYER [highlightid playerid zone]
;; identify zones


(expect true
  (let [teststate twoplyrgame 
        players (map :id (-> teststate :players (get "p1") :team :private)) 
        highlights (->> teststate :highlights (map) :id)
        ai-commit (-> teststate (bbmodel/ai-commit-player "p1"))]
      (and (-> ai-commit first number?)
           (-> ai-commit second number?)
           (or (-> ai-commit last (= :a))(-> ai-commit last (= :b))))))