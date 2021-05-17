(ns lobby.bbtest
  (:require
    [expectations :refer :all]
    [lobby.model :as model]
    [lobby.bbmodel :as bbmodel]
    [lobby.lobbytest :refer :all]))

(def twoplyrgamewithai   
  (let [gs (->> (bbmodel/setup ["p1" "AI1234"]))
        p1team (if (= (-> gs :players (get "AI1234") :team) "Orc") "Human" "Orc")]
    (bbmodel/parseaction gs {:action :chooseteam :team p1team} "p1") ))

; create 2p game
(expect ["p1" "AI1234"]
  (->> (bbmodel/setup ["p1" "AI1234"])
       :players
       keys))

(expect 2
  (let [gs (->> (bbmodel/setup ["p1" "AI1234"]))
        p1team (if (= (-> gs :players (get "AI1234") :team) "Orcs") "Human" "Orcs")]
    (->> 
         (bbmodel/choose-team gs p1team "p1")  
         :players vals
         (map :team)
         count)))

(expect 2
  (->> twoplyrgamewithai
      :players vals
      (map :team)
      count))

; Start Game. Turn = 0
; remove all team info
;; create freebooter deck
; draw 2 highlights 
; each player draws 6

(expect 0 (-> twoplyrgamewithai :turn))
(expect nil (-> twoplyrgamewithai :teams))
(expect #(= (-> % :turnorder count) (-> :highlights :public count) ) (twoplyrgamewithai))
(expect 6 (-> twoplyrgamewithai :players (get "p1") :team :private count))
(expect 6 (-> twoplyrgamewithai :players (get "p1") :team :secret count))
(expect 6 (-> twoplyrgamewithai :players (get "AI1234") :team :private count))
(expect 6 (-> twoplyrgamewithai :players (get "AI1234") :team :secret count))

(expect true
  (let [gs (->> (bbmodel/setup ["p1" "AI1234"]))
        p1team (if (= (-> gs :players (get "AI1234") :team) "Orc") "Human" "Orc")]
    (=
      (-> (bbmodel/parseaction gs {:action :chooseteam :team p1team} "p1") 
        :players 
        (get "p1")
        :team :alliance)
      (->> gs :teams (filter #(= (:team %) p1team)) first :alliance))))