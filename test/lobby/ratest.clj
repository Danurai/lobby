(ns lobby.ratest
  (:require
    [expectations :refer :all]
    [lobby.model :as model]
    [lobby.ramodel :as ramodel]
    [lobby.lobbytest :as lobbytest]))


(defn- start-test [ gs txt ]
  (println "\nStart Test: " txt )
  gs)

(defn- started2pgm []
"2 player game of Res Arcana, both players have selected start mages and magic items. Turn order fixed to [P1 P2]"
  (let [setup  (assoc (ramodel/setup ["p1" "p2"]) :plyr-to ["p1" "p2"])]
    (-> setup
        (ramodel/parseaction {:action :selectmage :card (-> setup :players (get "p1") :private :mages first :uid)} "p1")
        (ramodel/parseaction {:action :selectmage :card (-> setup :players (get "p2") :private :mages first :uid)} "p2")
        (ramodel/parseaction {:action :selectstartitem :card (-> setup :magicitems first :uid)} "p2")
        (ramodel/parseaction {:action :selectstartitem :card (-> setup :magicitems last :uid)} "p1")
        )))    
(defn- started2pgm-collected []
  (-> (started2pgm)
      (ramodel/parseaction {:action :collected} "p1")
      (ramodel/parseaction {:action :collected} "p2")))
    
;(defn- collected [ gs ]
;  (reduce (fn [ gs uname] (ramodel/parseaction gs {:action :collected} uname)) gs (:plyr-to gs)))

; Test started2pgm turn order
(expect [["p1" "p2"] :play :waiting]
  (let [gs (started2pgm)]
    [
      (-> gs :plyr-to)
      (-> gs :players (get "p1") :action)
      (-> gs :players (get "p2") :action)
    ]
    ))
; is always the same turn order
(expect 10
  (let [states (repeatedly 10 #(started2pgm))]
    (->>  states
          (mapv #(-> % :players (get "p1") :action))
          frequencies
          :play)))
(expect 10
  (let [states (repeatedly 10 #(started2pgm))]
    (get (->>  states
              (map #(-> % :plyr-to first))
              frequencies) "p1")))    
(expect :action
  (-> (started2pgm-collected)
      :phase))
; RA

;;;;; NEW GAME ;;;;;

;; New game has public list of all cards Name, Type, Id
(expect 78
  (->> (ramodel/setup ["p1" "p2"]) :allcards count))
;; New game has 5 distinct :base places of power, 2 public monumnets and 8 hidden
(expect 5
  (->> (ramodel/setup ["p1"]) :pops (map :base) distinct count))
(expect 2
  (-> (ramodel/setup ["p1"]) :monuments :public count))
(expect 8
  (-> (ramodel/setup ["p1"]) :monuments :secret count))
; Obfuscated monument deck for p1
(expect 8 
  (-> (ramodel/setup ["p1"]) (ramodel/obfuscate "p1") :monuments :secret))

; Each new player has 2 mages and 8 artifacts
(expect 2 
  (-> (ramodel/setup ["p1"]) :players (get "p1") :private :mages     count))
(expect 8 
  (-> (ramodel/setup ["p1"]) :players (get "p1") :private :artifacts count))

; 1 player game has 1 player etc
(expect 1 (-> (ramodel/setup ["p1"]) :players keys count))
(expect 2 (-> (ramodel/setup ["p1" "p2"]) :players keys count))
(expect 3 (-> (ramodel/setup ["p1" "p2" "p3"]) :players keys count))
(expect 4 (-> (ramodel/setup ["p1" "p2" "p3" "p4"]) :players keys count))
      
; new player :action is :selectmage
(expect :selectmage 
  (-> (ramodel/setup ["p1"]) :players (get "p1") :action))
(expect 2
  (->> (ramodel/setup ["p1" "p2"])
       :players 
       (filter (fn [[k v]] (= (:action v) :selectmage)))
       keys count))
        
; New AI Player has a Public selected Mage
(expect true
  (let [aip (-> "AI" gensym str)
        gs (ramodel/setup ["p1" aip])
        m1 (-> gs :players (get aip) :private :mages first)]
    (=  (:uid m1)
        (-> gs :players (get aip) :public :mage))))
    
; Player has a Public selected Mage after selection
;; Public = <card id>
(expect true
  (let [gs (ramodel/setup ["p1" "p2"])
        m1 (-> gs :players (get "p1") :private :mages first)]
    (=  (:uid m1)
        (-> gs 
            (ramodel/parseaction {:action :selectmage :card (:uid m1)} "p1")
            :players (get "p1") :public :mage))))

; ..even if they do it multiple times
(expect true
  (let [gs (ramodel/setup ["p1" "p2"])
        m1 (-> gs :players (get "p1") :private :mages first)
        m2 (-> gs :players (get "p1") :private :mages last)]
    (=  (:uid m1)
        (-> gs 
            (ramodel/parseaction {:action :selectmage :card (:uid m1)} "p1")
            (ramodel/parseaction {:action :selectmage :card (:uid m2)} "p1")
            (ramodel/parseaction {:action :selectmage :card (:uid m1)} "p1")
            :players (get "p1") :public :mage))))
          
; AI and P1 have selected a mage
(expect 2
  (let [gs (assoc (ramodel/setup ["p1" "AI123"]) :plyr-to ["p1" "AI123"])
        m1 (-> gs :players (get "p1") :private :mages first)]
    (->> (ramodel/parseaction gs {:action :selectmage :card (:uid m1)} "p1")
         :players
         (reduce-kv #(if (-> %3 :public :mage some?) (inc %1) %1) 0))))
                  
       
;; Select Magic Item
; reverse turn order
(expect :selectstartitem 
  (let [gs (ramodel/setup ["p1" "p2"])
        m1 (-> gs :players (get "p1") :private :mages first)
        m2 (-> gs :players (get "p2") :private :mages first)]
    (-> gs
        (ramodel/parseaction {:action :selectmage :card (:uid m1)} "p1")
        (ramodel/parseaction {:action :selectmage :card (:uid m2)} "p2")
        :players 
        (get (-> gs :plyr-to last))
        :action
        )))
; one at a time
(expect 1
  (let [gs (ramodel/setup ["p1" "p2"])
        m1 (-> gs :players (get "p1") :private :mages first)
        m2 (-> gs :players (get "p2") :private :mages first)]
    (count (filter 
      (fn [[k v]] (= (:action v) :selectstartitem))
      (-> gs
        (ramodel/parseaction {:action :selectmage :card (:uid m1)} "p1")
        (ramodel/parseaction {:action :selectmage :card (:uid m2)} "p2")
        :players)))))
; When all mages have been selected, make the Mage public
(expect #(some? (:name %))
  (let [gs (ramodel/setup ["p1" "p2"])
        m1 (-> gs :players (get "p1") :private :mages first)
        m2 (-> gs :players (get "p2") :private :mages first)
        mi1 (-> gs :magicitems first)]
    (-> gs
      (ramodel/parseaction {:action :selectmage :card (:uid m1)} "p1")
      (ramodel/parseaction {:action :selectmage :card (:uid m2)} "p2")
      :players (get "p1") :public :mage)))
; ... and turn off target?
(expect #(nil? (:target? %))
  (let [gs (ramodel/setup ["p1" "p2"])
        m1 (-> gs :players (get "p1") :private :mages first)
        m2 (-> gs :players (get "p2") :private :mages first)
        mi1 (-> gs :magicitems first)]
    (-> gs
      (ramodel/parseaction {:action :selectmage :card (:uid m1)} "p1")
      (ramodel/parseaction {:action :selectmage :card (:uid m2)} "p2")
      :players (get "p1") :public :mage)))

; Select Item    
(expect 1
  (let [gs (ramodel/setup ["p1" "p2"])
        m1 (-> gs :players (get "p1") :private :mages first)
        m2 (-> gs :players (get "p2") :private :mages first)
        mi1 (-> gs :magicitems first)]
    (count (filter :owner
        (-> gs
          (ramodel/parseaction {:action :selectmage :card (:uid m1)} "p1")
          (ramodel/parseaction {:action :selectmage :card (:uid m2)} "p2")
          (ramodel/parseaction {:action :selectstartitem :card (:uid mi1)} (-> gs :plyr-to last))
          :magicitems
          )))))
; one at a time
(expect 1
  (let [gs (ramodel/setup ["p1" "p2"])
        m1 (-> gs :players (get "p1") :private :mages first)
        m2 (-> gs :players (get "p2") :private :mages first)
        mi1 (-> gs :magicitems first)]
    (count (filter 
      (fn [[k v]] (= (:action v) :selectstartitem))
      (-> gs
        (ramodel/parseaction {:action :selectmage :card (:uid m1)} "p1")
        (ramodel/parseaction {:action :selectmage :card (:uid m2)} "p2")
        (ramodel/parseaction {:action :selectstartitem :card (:uid mi1)} (-> gs :plyr-to last))
        :players)))))
;; in reverse player order
(expect :selectstartitem 
  (let [gs (ramodel/setup ["p1" "p2"])
        m1 (-> gs :players (get "p1") :private :mages first)
        m2 (-> gs :players (get "p2") :private :mages first)
        mi1 (-> gs :magicitems first)]
    (-> gs
        (ramodel/parseaction {:action :selectmage :card (:uid m1)} "p1")
        (ramodel/parseaction {:action :selectmage :card (:uid m2)} "p2")
        (ramodel/parseaction {:action :selectstartitem :card (:uid mi1)} (-> gs :plyr-to last))
        :players  (get (-> gs :plyr-to first)) :action)))
; prevent selecting an item already taken
(expect "p1"
  (let [gs (ramodel/setup ["p1" "p2"])
        m1 (-> gs :players (get "p1") :private :mages first)
        m2 (-> gs :players (get "p2") :private :mages first)
        mi1 (-> gs :magicitems first)]
    (-> gs
      (ramodel/parseaction {:action :selectmage :card (:uid m1)} "p1")
      (ramodel/parseaction {:action :selectmage :card (:uid m2)} "p2")
      (ramodel/parseaction {:action :selectstartitem :card (:uid mi1)} "p1")
      (ramodel/parseaction {:action :selectstartitem :card (:uid mi1)} "p2")
      :magicitems first :owner)))

(expect :pass
  (let [gs (ramodel/setup ["p1" "p2"])
        m1 (-> gs :players (get "p1") :private :mages first)
        m2 (-> gs :players (get "p2") :private :mages first)
        mi1 (-> gs :magicitems first)]
    (-> gs
      (ramodel/parseaction {:action :selectmage :card (:uid m1)} "p1")
      (ramodel/parseaction {:action :selectmage :card (:uid m2)} "p2")
      (ramodel/parseaction {:action :selectstartitem :card (:uid mi1)} (-> gs :plyr-to last))
      :players (get (-> gs :plyr-to last)) :action)))

;; 1player INVALID Mage ID
(expect nil
  (let [gs (ramodel/setup ["p1"])
        m1 (-> gs :players (get "p1") :private :mages first)]
    (-> gs
        (ramodel/parseaction {:action :selectmage} "p1")
        (model/obfuscate-gm "p1")
        :players (get "p1") :public :mage)))
(expect :selectmage
  (let [gs (ramodel/setup ["p1"])
        m1 (-> gs :players (get "p1") :private :mages first)]
    (-> gs
        (ramodel/parseaction {:action :selectmage} "p1")
        (model/obfuscate-gm "p1")
        :players (get "p1") :action)))
        
        
; All players selected a mage (including AI setup), and a Magic Item game on!
(expect :play
  (-> (started2pgm) :status))
; With AI
(expect :play
  (let [aip "AI123"
        gs (ramodel/setup ["p1" aip])
        m1 (-> gs :players (get "p1") :private :mages first)
        mi1 (-> gs :magicitems last)]   
    (-> gs
        (ramodel/parseaction {:action :selectmage :card (:uid m1)} "p1")
        (ramodel/parseaction {:action :selectstartitem :card (:uid mi1)} "p1")
        :status)))
; All AI
(expect :play 
  (let [aip1 (-> "AI" gensym str)
        aip2 (-> "AI" gensym str)]
    (-> (ramodel/setup [aip1 aip2])
        :status)))
        
;; Start game sets Public Mage, private artiftacts = 3, secret artifacts = 5, private mages nil
(expect some?
  (let [aip (-> "AI" gensym str)]
    (-> (ramodel/setup [aip]) :players (get aip) :public :mage :name)))
(expect 0
  (let [aip (-> "AI" gensym str)]
    (-> (ramodel/setup [aip]) :players (get aip) :public :artifacts count)))
(expect 3
  (let [aip (-> "AI" gensym str)]
    (-> (ramodel/setup [aip]) :players (get aip) :private :artifacts count)))
(expect 5
  (let [aip (-> "AI" gensym str)]
    (-> (ramodel/setup [aip]) :players (get aip) :secret :artifacts count)))
(expect nil
  (let [aip (-> "AI" gensym str)]
    (-> (ramodel/setup [aip]) :players (get aip) :private :mages)))
(expect :play
  (let [aip (-> "AI" gensym str)]
    (-> (ramodel/setup [aip]) :players (get aip) :action)))

    
; New round places essence on cards
(expect true
  (let [gs  (started2pgm)]
    (=  (-> gs :players (get "p1") :public :mage :collect)
        (-> gs :players (get "p1") :public :mage :collect-essence))))
    
; Take essence from card
(expect 2
  (let [gs (started2pgm)]
    (-> gs 
        (ramodel/parseaction {:action :collect-essence :essence {:death 1} :card (-> gs :players (get "p1") :public :mage)} "p1")
        :players (get "p1") :public :essence :death)))
(expect nil
  (let [gs (started2pgm)]
    (-> gs 
        (ramodel/parseaction {:action :collect-essence :essence {:death 1} :card (-> gs :players (get "p1") :public :mage)} "p1")
        :players (get "p1") :public :mage :collectessence)))    
    
    
; essence Change
(expect {:life 1 :death 1 :elan 1 :calm 1 :gold 1}
  (-> (ramodel/setup ["p1"]) :players (get "p1") :public :essence))
      
(expect {:life 1 :death 1 :elan 1 :calm 1 :gold 1}
  (-> (ramodel/setup ["p1"]) 
      (ramodel/update-player-essence {:essence {}} "p1") :players (get "p1") :public :essence))
      
(expect {:life 0 :death 1 :elan 2 :calm 3 :gold 4}
  (-> (ramodel/setup ["p1"]) 
      (ramodel/update-player-essence {:essence {:life -1 :elan 1 :calm 2 :gold 3} :action :update-player-essence} "p1")
      :players (get "p1") :public :essence))
      

       
; Done
(expect  [:waiting :play] ; current player action = :waiting, next player action = :play
  (let [gs (ramodel/end-action (started2pgm) "p1")]
    [(-> gs :players (get "p1") :action)
     (-> gs :players (get "p2") :action)]))
     
(expect  [:play :waiting] ; current player action = :waiting, next player action = :play
  (let [gs (-> (started2pgm)
               (ramodel/end-action "p1")
               (ramodel/end-action "p2"))
        fp (-> gs :plyr-to first)
        sp (-> gs :plyr-to last)]
    [(-> gs :players (get fp) :action)
     (-> gs :players (get sp) :action)]))



; PASS     
;; p1 pass
(expect  [["p1" "p2"] ["p2"] :selectmagicitem :waiting] ; Pass Turn Order | New Turn Order | Pass Player :action | Next Player :action
  (let [gs (-> (started2pgm-collected)
               (ramodel/parseaction {:action :pass} "p1"))]
    [(-> gs :pass-to)
     (-> gs :plyr-to)
     (-> gs :players (get "p1") :action)
     (-> gs :players (get "p2") :action)]))
;; p1 pass, choose MagicItem
(expect  [["p1" "p2"] ["p2"] :pass :play 4 4] ; Pass Turn Order | New Turn Order | Pass player action | Next player action | Artifacts in Hand | Artifacts in Deck
  (let [s2pg  (started2pgm-collected)
        gs    (-> s2pg
                  (ramodel/parseaction {:action :pass} "p1")
                  (ramodel/parseaction {:action :selectmagicitem :card (-> s2pg :state :magicitems (nth 3))} "p1"))]
    [(-> gs :pass-to)
     (-> gs :plyr-to)
     (-> gs :players (get "p1") :action)
     (-> gs :players (get "p2") :action)
     (-> gs :players (get "p1") :private :artifacts count)
     (-> gs :players (get "p1") :secret :artifacts count)]))
;; p2 pass 
(expect  ["p2" ["p1"] :waiting :selectmagicitem] ; current player action = :waiting, next player action = :play
  (let [gs (-> (started2pgm-collected)
               (ramodel/end-action "p1")
               (ramodel/parseaction {:action :pass} "p2"))]
    [(-> gs :pass-to first)
     (:plyr-to gs)
     (-> gs :players (get "p1") :action)
     (-> gs :players (get "p2") :action)]))
;; p2 pass, choose mi
(expect  [["p2" "p1"] ["p1"] :play :pass] ; Empty pass-to Turnorder based on pass-to players set to :play or :waiting
  (let [s2pg  (started2pgm-collected)
        gs (-> s2pg
               (ramodel/end-action "p1")
               (ramodel/parseaction {:action :pass} "p2")
               (ramodel/parseaction {:action :selectmagicitem :card (-> s2pg :state :magicitems (nth 3) :uid)} "p2"))]
    [(-> gs :pass-to)
     (-> gs :plyr-to)
     (-> gs :players (get "p1") :action)
     (-> gs :players (get "p2") :action)]))
; P1 'done' P2 passed, P1 pass
(expect  [["p2" "p1"] [] :selectmagicitem :pass] ; Empty pass-to Turnorder based on pass-to players set to :play or :waiting
  (let [s2pg  (started2pgm-collected)
        gs (-> s2pg
               (ramodel/parseaction {:action :done} "p1")
               (ramodel/parseaction {:action :pass} "p2")
               (ramodel/parseaction {:action :selectmagicitem :card (-> s2pg :state :magicitems (nth 3) :uid)} "p2")
               (ramodel/parseaction {:action :pass} "p1")
               )]
    [(-> gs :pass-to)
     (-> gs :plyr-to)
     (-> gs :players (get "p1") :action)
     (-> gs :players (get "p2") :action)]))

; P1 'done' P2 passed, P1 pass & select MI
(expect  [[] ["p2" "p1"] :waiting :play] ; Empty pass-to | Turnorder based on pass-to players set to :play or :waiting
  (let [s2pg  (started2pgm-collected)
        gs (-> s2pg
               (ramodel/parseaction {:action :done} "p1")
               (ramodel/parseaction {:action :pass} "p2")
               (ramodel/parseaction {:action :selectmagicitem :card (-> s2pg :state :magicitems (nth 4) :uid)} "p2")
               (ramodel/parseaction {:action :pass} "p1")
               (ramodel/parseaction {:action :selectmagicitem :card (-> s2pg :state :magicitems (nth 3) :uid)} "p1"))]
    [(-> gs :pass-to)
     (-> gs :plyr-to)
     (-> gs :players (get "p1") :action)
     (-> gs :players (get "p2") :action)]))

    
; Obfuscation 
; Public = P1 can see P1 public data, P2 can see P1 public data
(expect true 
  (let [gs (ramodel/setup ["p1" "p2"])]
    (=
      (-> gs :players (get "p1") :public)
      (-> gs (ramodel/obfuscate "p1") :players (get "p1") :public))))
(expect true 
  (let [gs (ramodel/setup ["p1" "p2"])]
    (=
      (-> gs :players (get "p1") :public)
      (-> gs (ramodel/obfuscate "p2") :players (get "p1") :public))))
      
; Private = P1 can see P1 private data P2 can't see P1 private data, only counts
(expect true
  (let [gs (ramodel/setup ["p1" "p2"])]
    (=
      (-> gs :players (get "p1") :private)
      (-> gs (ramodel/obfuscate "p1") :players (get "p1") :private))))
      
(expect 8
  (-> (ramodel/setup ["p1" "p2"])
      (ramodel/obfuscate "p2") 
      :players 
      (get "p1")
      :private
      :artifacts))
      
; Secret = P1 can't see P1 Secret Data, P2 can't see P1 secret data
(expect 0
  (-> (ramodel/setup ["p1" "p2"])
      (ramodel/obfuscate "p1") 
      :players 
      (get "p1")
      :secret
      :artifacts))

(expect 0
  (-> (ramodel/setup ["p1" "p2"])
      (ramodel/obfuscate "p1") 
      :players 
      (get "p2")
      :secret
      :artifacts))

; ai collect generated essence test state
(def rstate {
  :chat []
  :status :play
  :phase :collect
  :plyr-to ["AI1" "P1"]
  :pass-to #{}
  :magicitems [
    {:name "TESTMAGICITEM" :type "magicitem" :collect-essence [{:elan 1} {:gold 1}] :owner "AI1"}
    {:name "TESTMAGICITEM" :type "magicitem" :collect-essence [{:elan 1} {:gold 1}] :owner "P1"}
  ]
  :players {
    "AI1" {
      :action :play
      :public {
        :mage {:type "mage" :name "TESTMAGE" :collect-essence [{:gold 1} {:death 1}]}
        :essence {
          :gold 1
          :calm 1
          :life 1
          :elan 1
          :death 1
        }
        :artifacts [
          {:id 4  :type "artifact" :name "Chalice of Life"  :cost {:gold 1 :elan 1} :collect [{:calm 1 :life 1}] :collect-essence [{:calm 1 :life 1}]}
        ]
      }
    }
    "P1" {
      :action :waiting
      :public {
        :essence {
          :calm 1
          :life 1
        }
        :mage {:type "mage" :name "TESTMAGE" :collect-essence [{:gold 1} {:death 1}]}
        :artifacts [
          {:id 4  :type "artifact" :name "Chalice of Life"  :cost {:gold 1 :elan 1} :collect [{:calm 1 :life 1}] :collect-essence [{:calm 1 :life 1}]}
        ]
      }
    }
  }
})

; Check ramodel/update-player-essence
(expect {:gold 2 :life 2}
  (-> {:chat [] :players {"AI1" {:public {:essence {:gold 1 :life 1}}}}}
    (ramodel/update-player-essence {:essence {:gold 1 :life 1} :card {:name "TESTCARD"}} "AI1")
    :players 
    (get "AI1")
    :public
    :essence))

; Check ramodel/ai-collect-essence works
(expect {:calm 2 :life 2 :gold 2 :elan 2 :death 1}
  (-> rstate ramodel/ai-collect-essence :players (get "AI1") :public :essence))
; Check ramodel/ai-collect-essence works
(expect true
  (-> rstate ramodel/ai-collect-essence :players (get "AI1") :collected?))

; remove from mage
(expect nil
  (-> rstate ramodel/ai-collect-essence :players (get "AI1") :public :mage :collect-essence))
; remove from magicitem
(expect nil
  (-> rstate ramodel/ai-collect-essence :magicitems first :collect-essence))
; remove from artifact
(expect nil
  (-> rstate ramodel/ai-collect-essence :players (get "AI1") :public :artifacts first :collect-essence))

; but not for non-ai (#"AI/d+")
(expect 1
  (-> rstate ramodel/ai-collect-essence :players (get "P1") :public :essence :calm))

; collect action
(expect true 
  (-> {:players {"P1" {} "P2" {}}}
      (ramodel/parseaction {:action :collected} "P2")
      :players (get "P2") :collected?
      ))
; collect action
(expect true 
  (-> {:players {"P1" {} "P2" {}}}
      (ramodel/parseaction {:action :collected} "P1")
      :players (get "P1") :collected?
      ))
; collect action toggle
(expect nil
  (-> {:players {"P1" {} "P2" {}}}
      (ramodel/parseaction {:action :collected} "P1")
      (ramodel/parseaction {:action :collected} "P1")
      :players (get "P1") :collected?
      ))
; All players have indicated ready, phase is action
(expect :action
  (-> {:phase :collect :players {"P1" {} "P2" {}}}
      (ramodel/parseaction {:action :collected} "P1")
      (ramodel/parseaction {:action :collected} "P2")
      :phase))
(expect nil
  (-> {:phase :collect :players {"P1" {} "P2" {}}}
      (ramodel/parseaction {:action :collected} "P1")
      (ramodel/parseaction {:action :collected} "P2")
      :players (get "P1") :collected?))
(expect :action ;; Needs a proper game setup to trigger all AI
  (-> rstate
      (assoc-in [:players "AI1" :collected?] true)
      (ramodel/parseaction {:action :collected} "P1")
      :phase))

; Collect-to-Action remove all :collect-essence from Mage, MagicItems and Artifacts
(expect nil
  (-> rstate
      (assoc-in [:players "AI1" :collected?] true)
      (ramodel/parseaction {:action :collected} "P1")
      :players (get "P1") :public :mage :collect-essence ))
(expect nil
  (-> rstate
      (assoc-in [:players "AI1" :collected?] true)
      (ramodel/parseaction {:action :collected} "P1")
      :players (get "P1") :public :artifacts first :collect-essence ))
(expect nil
  (-> rstate
      (assoc-in [:players "AI1" :collected?] true)
      (ramodel/parseaction {:action :collected} "P1")
      :magicitems first :collect-essence ))

; AI Action = Pass (Re-factor with AI Logic)
;; No automation for all ai players (Infinite loop)
(expect ["AI123" "AI456"]
  (-> {:status :play :plyr-to ["AI123" "AI456"] :pass-to #{} :players {"AI123" {:action :play} "AI456" {:action :waiting}}}
      ramodel/ai-action
      :plyr-to
    ))
;; Revised Pass Turn order | Turn Order | actions
(expect [["AI123" "P1"] ["P1"] :pass :play]
  (let [gs (-> {:status :play :plyr-to ["AI123" "P1"] :pass-to #{} :players {"AI123" {:action :play} "P1" {:action :waiting}} :magicitems [{:uid :mi01 :owner "AI123"} {:uid :mi02 :owner "P1"} {:uid :mi03 :owner nil}]}
               ramodel/ai-action)]
    [ (:pass-to gs)
      (:plyr-to gs)
      (-> gs :players (get "AI123") :action)
      (-> gs :players (get "P1") :action) ] 
  ))
; AI Pass and Player Pass
(expect nil
  (let [gs (-> {:status :play :plyr-to ["AI123" "P1"] :pass-to #{} :players {"AI123" {:action :play} "P1" {:action :waiting}} :magicitems [{:uid :mi01 :owner "AI123"} {:uid :mi02 :owner "P1"} {:uid :mi03 :owner nil}]}
                ramodel/ai-action)]
    (->> gs :magicitems (filter #(= (:uid %) :mi01)) first :owner)))

;; turn order is empty before player selects MI
(expect []
  (let [gs (-> {:status :play :plyr-to ["AI123" "P1"] :pass-to #{} :players {"AI123" {:action :play} "P1" {:action :waiting}} :magicitems [{:uid :mi01 :owner "AI123"} {:uid :mi02 :owner "P1"} {:uid :mi03 :owner nil}]}
                ramodel/ai-action
                (ramodel/parseaction {:action :pass} "P1"))]
    (-> gs :plyr-to)))
; AI Pass and Player Pass and select MI. New Round with AI first :collect phase
(expect [ [] ["AI123" "P1"] :collect]
  (let [gs (-> {:status :play :round 1 :plyr-to ["AI123" "P1"] :pass-to #{} :players {"AI123" {:action :play} "P1" {:action :waiting}} :magicitems [{:uid :mi01 :owner "AI123"} {:uid :mi02 :owner "P1"} {:uid :mi03 :owner nil}]}
                ramodel/ai-action
                (ramodel/parseaction {:action :pass} "P1")
                (ramodel/parseaction {:action :selectmagicitem :card :mi01} "P1"))]
    [(:pass-to gs) (:plyr-to gs) (:phase gs) ] ))

; Player Pass and select MI and AI Pass. New Round.
(expect [[] ["P1" "AI123"] :collect]
  (let [gs (-> {:status :play :phase :action :round 1 :plyr-to ["P1" "AI123"] :pass-to #{} :players {"AI123" {:action :waiting} "P1" {:action :play}} :magicitems [{:uid :mi01 :owner "AI123"} {:uid :mi02 :owner "P1"} {:uid :mi03 :owner nil}]}
                (ramodel/parseaction {:action :pass} "P1")
                (ramodel/parseaction {:action :selectmagicitem :card :mi03} "P1"))]
    [(:pass-to gs) (:plyr-to gs) (:phase gs) ] ))

; get-active-player tests
(expect nil  (ramodel/get-active-player {:players {"P1" {:action :pass} "P2" {:action :pass}}}))
(expect "P1" (ramodel/get-active-player {:players {"P1" {:action :play} "P2" {:action :pass}}}))
(expect "P1" (ramodel/get-active-player {:players {"P1" {:action :selectmagicitem} "P2" {:action :pass}}}))

; Don't allow Pass if it's not your turn
(expect :waiting 
  (-> {:status :play :phase :action :players {"P1" {:action :play} "P2" {:action :waiting}}}
      (ramodel/parseaction {:action :pass} "P2")
      :players (get "P2") :action))

; Don't allow Pass during collect phase
(expect :play 
  (-> {:status :play :phase :collect :players {"P1" {:action :play} "P2" {:action :waiting}}}
      (ramodel/parseaction {:action :pass} "P1")
      :players (get "P1") :action))

;; DISCARD ACTION
; data? {:action :discard :essence {k v k v} :card {<card>} }
; Gain the essence
(expect {:elan 2 :death 2}
  (let [s2pgc (started2pgm-collected)
        a1    (-> s2pgc :players (get "p1") :private :artifacts first)]
    (-> s2pgc
        (ramodel/parseaction {:action :discard :essence {:elan 1 :death 1} :card a1} "p1")
        :players (get "p1") :public :essence (select-keys [:elan :death]) )))
; 2 of the same essence
(expect {:elan 3}
  (let [s2pgc (started2pgm-collected)
        a1    (-> s2pgc :players (get "p1") :private :artifacts first)]
    (-> s2pgc
        (ramodel/parseaction {:action :discard :essence {:elan 2} :card a1} "p1")
        :players (get "p1") :public :essence (select-keys [:elan]) )))
; discard the card
(expect 1
  (let [s2pgc (started2pgm-collected)
        a1    (-> s2pgc :players (get "p1") :private :artifacts first)]
    (-> s2pgc
        (ramodel/parseaction {:action :discard :essence {:elan 1 :death 1} :card a1} "p1")
        :players (get "p1") :public :discard count)))
; and remove from hand
(expect 2
  (let [s2pgc (started2pgm-collected)
        a1    (-> s2pgc :players (get "p1") :private :artifacts first)]
    (-> s2pgc
        (ramodel/parseaction {:action :discard :essence {:elan 1 :death 1} :card a1} "p1")
        :players (get "p1") :private :artifacts count)))


; Play card from hand
(expect [1 2 {:life 1 :death 1 :elan 1 :calm 1 :gold 0}]
  (let [gsetup  (ramodel/setup ["p1"])
        m1      (-> gsetup :players (get "p1") :private :mages first)
        mi1     (-> gsetup :magicitems first)
        gs      (-> gsetup 
                    (ramodel/parseaction {:action :selectmage :card (:uid m1)} "p1")
                    (ramodel/parseaction {:action :selectstartitem :card (:uid mi1)} "p1"))
        art1    (-> gs :players (get "p1") :private :artifacts first)
        art2    (-> gs :players (get "p1") :private :artifacts last)
        afterplay (ramodel/playcard gs {:card art1 :essence {:gold 1}} "p1")
        ]
    [ (-> afterplay :players (get "p1") :public  :artifacts count)
      (-> afterplay :players (get "p1") :private :artifacts count)
      (-> afterplay :players (get "p1") :public  :essence) ]))

;; PLACE and CLAIM actions


; Card Target Tests
; Set by raview based on player status
; Block actions when it's not the active player's turn 
; PLACE Card
; CLAIM Card



; DISCARD Card
; should be p1's action
(expect 0
  (let [s2pgc (started2pgm-collected)
        a1    (-> s2pgc :players (get "p2") :private :artifacts first)]
    (-> s2pgc
        (ramodel/parseaction {:action :discard :essence {:elan 1 :death 1} :card a1} "p2")
        :players (get "p2") :public :discard count)))
(expect 3
  (let [s2pgc (started2pgm-collected)
        a1    (-> s2pgc :players (get "p2") :private :artifacts first)]
    (-> s2pgc
        (ramodel/parseaction {:action :discard :essence {:elan 1 :death 1} :card a1} "p2")
        :players (get "p2") :private :artifacts count)))
(expect {:elan 1 :death 1}
  (let [s2pgc (started2pgm-collected)
        a1    (-> s2pgc :players (get "p2") :private :artifacts first)]
    (-> s2pgc
        (ramodel/parseaction {:action :discard :essence {:elan 1 :death 1} :card a1} "p2")
        :players (get "p2") :public :essence (select-keys [:elan :death]) )))

;;;;; TESTING Games ;;;;;;
(def g1 (ramodel/parseaction {} {:action :swapgame :game 1} "p1"))
(def g2 (ramodel/parseaction {} {:action :swapgame :game 2} "p1"))
;2 players

(expect 2 (-> g1 :plyr-to count))
(expect "Duelist" (let [p1 (-> g1 :plyr-to first)] (-> g1 :players (get p1) :public :mage :name)))
(expect 3 (let [p1 (-> g1 :plyr-to first)] (-> g1 :players (get p1) :private :artifacts count)))
(expect 5 (let [p1 (-> g1 :plyr-to first)] (-> g1 :players (get p1) :secret :artifacts count)))
(expect 5 (-> g1 :pops count))
(expect 2 (-> g1 :monuments :public count))
(expect 8 (-> g1 :monuments :secret count))
(expect 78 (-> g1 :allcards count))

(defn- end-turn [ gs p1 ]
  (-> gs 
      (ramodel/parseaction {:action :pass} p1)
      (ramodel/parseaction {:action :selectmagicitem :card (->> gs :magicitems (remove :owner) first)} p1)
      (ramodel/parseaction {:action :collected} p1)))
;; PLACE a card  ;;
(expect 3
  (let [p1 (-> g1 :plyr-to first)
        artifacts (-> g1 :players (get p1) :private :artifacts)]
    (-> g1
        (ramodel/parseaction {:action :place :card (first artifacts)  :essence (-> artifacts first :cost)} p1)
        (ramodel/parseaction {:action :place :card (second artifacts) :essence (-> artifacts second :cost)} p1)
        (ramodel/parseaction {:action :place :card (last artifacts)   :essence (-> artifacts last :cost)} p1)
        (end-turn p1)
        :players (get p1) :public :artifacts count
        )))

;prevent placing same card twice
(expect 1
  (let [p1 (-> g1 :plyr-to first)
        artifacts (-> g1 :players (get p1) :private :artifacts)]
    (-> g1 
        (ramodel/parseaction {:action :place :card (first artifacts) :essence (-> artifacts first :cost)} p1)
        (ramodel/parseaction {:action :place :card (first artifacts) :essence (-> artifacts first :cost)} p1)
        :players (get p1) :public :artifacts count)))
;error code 2 - artifact not in hand
(expect 2
  (let [p1 (-> g1 :plyr-to first)
        artifacts (-> g1 :players (get p1) :private :artifacts)]
    (-> g1 
        (ramodel/parseaction {:action :place :card (first artifacts) :essence (-> artifacts first :cost)} p1)
        (ramodel/parseaction {:action :place :card (first artifacts) :essence (-> artifacts first :cost)} p1)
        :players (get p1) :err)))

; After cards have been placed, start new turn, cards are still in order
(expect "Dragon Teeth"
  (let [p1 (-> g1 :plyr-to first)
        artifacts (-> g1 :players (get p1) :private :artifacts)]
    (-> g1
        (ramodel/parseaction {:action :place :card (first artifacts)  :essence (-> artifacts first :cost)} p1)
        (ramodel/parseaction {:action :place :card (second artifacts) :essence (-> artifacts second :cost)} p1)
        (ramodel/parseaction {:action :place :card (last artifacts)   :essence (-> artifacts last :cost)} p1)
        (end-turn p1)
        :players (get p1) :public :artifacts first :name)))




;; USE a card
; start 99 death, pay 1 death 1 life to place
; use PAY cost (mage)
(expect 98
  (let [p1 (-> g1 :plyr-to first)  
        mage (-> g1 :players (get p1) :public :mage)] 
    (-> g1 
        (ramodel/parseaction {:action :usecard :useraction (-> mage :action first) :card mage} p1)
        :players (get p1) :public :essence :death)))
; placed
(expect {:death 98 :elan 98} 
  (let [p1 (-> g1 :plyr-to first)
        artifact (-> g1 :players (get p1) :private :artifacts first)] ; Dragon Teeth {:cost {:elan 1 :death 1}
    (-> g1 
        (ramodel/parseaction {:action :place :card artifact :essence (:cost artifact)} p1)
        :players (get p1) :public :essence (select-keys [:elan :death]))))
; used (start 98 death), pay 0 death, GAIN 2 death
(expect 100
  (let [p1 (-> g1 :plyr-to first)
        artifact (-> g1 :players (get p1) :private :artifacts last)] 
    (-> g1 
        (ramodel/parseaction {:action :place :card artifact :essence (:cost artifact)} p1)
        (ramodel/parseaction {:action :usecard :card artifact :useraction (-> artifact :action first)} p1)
        :players (get p1) :public :essence :death)))
; use (start 98 elan, PAY 2 elan, place 2 elan)
(expect 96
  (let [p1 (-> g1 :plyr-to first)
        artifact (-> g1 :players (get p1) :private :artifacts first)] ; Dragon teeth {:action [{:exhaust true, :cost {:elan 2} :place {:elan 3}}]}
    (-> g1 
        (ramodel/parseaction {:action :place :card artifact :essence (:cost artifact)} p1)
        (ramodel/parseaction {:action :usecard :card artifact :useraction (-> artifact :action first)} p1)
        :players (get p1) :public :essence :elan)))
; RIVALS GAIN
(expect 100
  (let [p1 (-> g1 :plyr-to first)  
        p2 (-> g1 :plyr-to last)
        artifact (-> g1 :players (get p1) :private :artifacts last)]  ; Hand of Glory
    (-> g1 
        (ramodel/parseaction {:action :place :card artifact :essence (:cost artifact)} p1)
        (ramodel/parseaction {:action :usecard :useraction (-> artifact :action first) :card artifact} p1)
        :players (get p2) :public :essence :death)))

;; Place an essence (mage)
(expect 1
  (let [p1 (-> g1 :plyr-to first)  
        mage (-> g1 :players (get p1) :public :mage)] 
    (-> g1 
        (ramodel/parseaction {:action :usecard :useraction (-> mage :action first) :card mage} p1)
        :players (get p1) :public :mage :take-essence :gold)))
;; repeat (mage)
(expect 2
  (let [p1 (-> g1 :plyr-to first)  
        mage (-> g1 :players (get p1) :public :mage)] 
    (-> g1 
        (ramodel/parseaction {:action :usecard :useraction (-> mage :action first) :card mage} p1)
        (end-turn p1)
        (ramodel/parseaction {:action :usecard :useraction (-> mage :action first) :card mage} p1)
        :players (get p1) :public :mage :take-essence :gold)))
;; Place an essence (Artifact)
(expect 3
  (let [p1 (-> g1 :plyr-to first)  
        artifact (-> g1 :players (get p1) :private :artifacts first)] ; Dragon Teeth 
    (-> g1 
        (ramodel/parseaction {:action :place   :card artifact :essence (:cost artifact)} p1)
        (ramodel/parseaction {:action :usecard :card artifact :useraction (-> artifact :action first)} p1)
        :players (get p1) :public :artifacts first :take-essence :elan)))
; repeat (artifact)
(expect 6
  (let [p1 (-> g1 :plyr-to first)  
        artifact (-> g1 :players (get p1) :private :artifacts first)] ; Dragon Teeth 
    (-> g1 
        (ramodel/parseaction {:action :place   :card artifact :essence (:cost artifact)} p1)
        (ramodel/parseaction {:action :usecard :card artifact :useraction (-> artifact :action first)} p1)
        (end-turn p1)
        (ramodel/parseaction {:action :usecard :card artifact :useraction (-> artifact :action first)} p1)
        :players (get p1) :public :artifacts first :take-essence :elan)))

; Exhaust Mage
(expect true
  (let [p1 (-> g1 :plyr-to first)  
        mage (-> g1 :players (get p1) :public :mage)] 
    (-> g1 
        (ramodel/parseaction {:action :usecard :useraction (-> mage :action first) :card mage} p1)
        :players (get p1) :public :mage :exhausted?)))
; Exhaust 'Artifact'
(expect true
  (let [p1 (-> g1 :plyr-to first)
        artifact (-> g1 :players (get p1) :private :artifacts last)]
    (-> g1
        (ramodel/parseaction {:action :place :card artifact :essence (:cost artifact)} p1)
        (ramodel/parseaction {:action :usecard :useraction (-> artifact :action first) :card artifact} p1)
        :players (get p1) :public :artifacts first :exhausted?)))
; Exhaust Magic Item
(expect true
  (let [p1 (-> g2 :plyr-to first)
        mi (->> g2 :magicitems (filter #(= (:owner %) p1)) first)]      ; Research 
    (->>  (ramodel/parseaction g2 {:action :usecard :useraction (-> mi :action first (assoc :cost {:death 1})) :card mi} p1)
          :magicitems
          (filter #(= (:owner %) p1)) first :exhausted?)))


; Unexhaust (mage)
(expect nil
  (let [p1 (-> g1 :plyr-to first)  
        mage (-> g1 :players (get p1) :public :mage)] 
    (-> g1 
        (ramodel/parseaction {:action :usecard :useraction {:exhaust true, :cost {:death -1}, :place {:gold 1}} :card mage} p1)
        (end-turn p1)
        :players (get p1) :public :mage :exhausted?)))
; Unexhaust 'Artifact'
(expect nil
  (let [p1 (-> g1 :plyr-to first)
        artifact (-> g1 :players (get p1) :private :artifacts last)]
    (-> g1
        (ramodel/parseaction {:action :place :card artifact :essence (:cost artifact)} p1)
        (ramodel/parseaction {:action :usecard :useraction (-> artifact :action first) :card artifact} p1)
        (end-turn p1)
        :players (get p1) :public :artifacts first :exhausted?)))
; Unexhaust MagicItem
(expect {nil 8}
  (let [p1 (-> g2 :plyr-to first)
        mi (->> g2 :magicitems (filter #(= (:owner %) p1)) first)]      ; Research 
    (->>  (-> g2 
              (ramodel/parseaction {:action :usecard :useraction (-> mi :action first (assoc :cost {:death 1})) :card mi} p1)
              (end-turn p1))
          :magicitems
          (map :exhausted?)
          frequencies
          )))

;;; Use card - Draw
(expect 4
  (let [p1 (-> g2 :plyr-to first)
        mi (->> g2 :magicitems (filter #(= (:owner %) p1)) first)]      ; Research 
    (-> g2
        (ramodel/parseaction {:action :usecard :useraction (-> mi :action first (assoc :cost {:death 1})) :card mi} p1)
        :players (get p1) :private :artifacts count)))
        
;;; Take Essences - hard-coded placed essence in ramodel/parseaction calls
; Take essence from card (mage)
(expect 100
  (let [p1 (-> g1 :plyr-to first)  
        mage (-> g1 :players (get p1) :public :mage)] ; Duelist :action [{:exhaust true :cost {:death 1} :place {:gold 1}}] 
    (-> g1 
        (ramodel/parseaction {:action :usecard :useraction (-> mage :action first) :card mage} p1)
        (ramodel/parseaction {:action :pass} p1)
        (ramodel/parseaction {:action :selectmagicitem :card (-> g1 :magicitems last)} p1)
        (ramodel/parseaction {:action :take-essence :card (-> g1 :players (get p1) :public :mage (assoc :take-essence {:gold 1}))} p1)
        :players (get p1) :public :essence :gold)))
(expect nil
  (let [p1 (-> g1 :plyr-to first)  
        mage (-> g1 :players (get p1) :public :mage)] ; Duelist :action [{:exhaust true :cost {:death 1} :place {:gold 1}}] 
    (-> g1 
        (ramodel/parseaction {:action :usecard :useraction (-> mage :action first) :card mage} p1)
        (ramodel/parseaction {:action :pass} p1)
        (ramodel/parseaction {:action :selectmagicitem :card (-> g1 :magicitems last)} p1)
        (ramodel/parseaction {:action :take-essence :card (-> g1 :players (get p1) :public :mage (assoc :take-essence {:gold 1}))} p1)
        :players (get p1) :public :mage :take-essence)))
;;; Take essence from card (artifact)
(expect 99
  (let [p1 (-> g1 :plyr-to first)  
        artifact (-> g1 :players (get p1) :private :artifacts first)] ; Dragon Teeth 
    (-> g1 
        (ramodel/parseaction {:action :place   :card artifact :essence (:cost artifact)} p1)                ; -1 elan - 1death
        (ramodel/parseaction {:action :usecard :card artifact :useraction (-> artifact :action first)} p1)  ; -2 elan 
        (end-turn p1)
        (ramodel/parseaction {:action :take-essence :card (assoc artifact :take-essence {:elan 3})} p1)
        :players (get p1) :public :essence :elan)))
;
(expect nil
  (let [p1 (-> g1 :plyr-to first)  
        artifact (-> g1 :players (get p1) :private :artifacts first)] ; Dragon Teeth 
    (-> g1 
        (ramodel/parseaction {:action :place   :card artifact :essence (:cost artifact)} p1)                ; -1 elan - 1death
        (ramodel/parseaction {:action :usecard :card artifact :useraction (-> artifact :action first)} p1)  ; -2 elan 
        (end-turn p1)
        (ramodel/parseaction {:action :take-essence :card (assoc artifact :take-essence {:elan 3})} p1)
        :players (get p1) :public :artifacts first :take-essence)))
;
;
;;;;;; Chat Commands ;;;;;
;;; Route chat commands through model.clj
;(expect "This is a test message"
;  (let [gid (lobbytest/newgamegid)]
;    (-> (model/addchat! gid "p1" "This is a test message") :games gid :state :chat first :msg)))
;
;(expect "This is a test message"
;  (let [p1 (-> g1 :plyr-to first)] (-> g1 (ramodel/chat-handler "This is a test message" p1) :chat second :msg)))
;  
;(expect "help: /essence <essence name> <new value>"
;  (let [p1 (-> g1 :plyr-to first)] (-> g1 (ramodel/chat-handler "/essence" p1) :chat last :msg)))
;
;;; Set essence
;(expect "/essence gold 200"
;  (let [p1 (-> g1 :plyr-to first)] (-> g1 (ramodel/chat-handler "/essence gold 200" p1) :chat second :msg)))
;(expect 200
;  (let [p1 (-> g1 :plyr-to first)] (-> g1 (ramodel/chat-handler "/essence gold 200" p1) :players (get p1) :public :essence :gold)))
;