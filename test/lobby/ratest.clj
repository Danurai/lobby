(ns lobby.ratest
  (:require
    [expectations :refer :all]
    [lobby.model :as model]
    [lobby.ramodel :as ramodel]
    [lobby.lobbytest :as lobbytest]))


(defn- start-test [gs txt ]
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
    
;(defn- collected [gs ]
;  (reduce (fn [gs uname] (ramodel/parseaction gs {:action :collected} uname)) gs (:plyr-to gs)))

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
    [(:pass-to gs)
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
(expect [[] ["AI123" "P1"] :collect]
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
(def g3 (ramodel/parseaction {} {:action :swapgame :game 3} "p1"))
(def g4 (ramodel/parseaction {} {:action :swapgame :game 4} "p1"))
(def g5 (ramodel/parseaction {} {:action :swapgame :game 5} "p1"))
(def p1 "p1")

; setup tests
(expect 2 (-> g1 :plyr-to count))
(expect "Duelist" (-> g1 :players (get p1) :public :mage :name))
(expect 3 (-> g1 :players (get p1) :private :artifacts count))
(expect 5 (-> g1 :players (get p1) :secret :artifacts count))
(expect 5 (-> g1 :pops count))
(expect 2 (-> g1 :monuments :public count))
(expect 8 (-> g1 :monuments :secret count))
(expect 78 (-> g1 :allcards count))
(expect 78 (-> g2 :allcards count))
(expect 78 (-> g3 :allcards count))
(expect 78 (-> g4 :allcards count))
(expect ["p1" "p2"] (-> g4 :plyr-to))
(expect ["p2" "p1"] (-> g4 :players keys vec))
(expect 5 (-> g4 :pops count))

(defn- end-turn [ gs pl ]
  (-> gs 
      (ramodel/parseaction {:action :pass} pl)
      (ramodel/parseaction {:action :selectmagicitem :card (->> gs :magicitems (remove :owner) first :uid)} pl)
      (ramodel/parseaction {:action :collected} pl)))

;; PLACE and CLAIM actions
;; PLACE a card  ;;
(expect 3
  (let [artifacts (-> g1 :players (get p1) :private :artifacts)]
    (-> g1
        (ramodel/parseaction {:action :place :card (first artifacts)  :essence (-> artifacts first :cost)} p1)
        (ramodel/parseaction {:action :place :card (second artifacts) :essence (-> artifacts second :cost)} p1)
        (ramodel/parseaction {:action :place :card (last artifacts)   :essence (-> artifacts last :cost)} p1)
        (end-turn p1)
        :players (get p1) :public :artifacts count
        )))

;prevent placing same card twice
(expect 1
  (let [artifacts (-> g1 :players (get p1) :private :artifacts)]
    (-> g1 
        (ramodel/parseaction {:action :place :card (first artifacts) :essence (-> artifacts first :cost)} p1)
        (ramodel/parseaction {:action :place :card (first artifacts) :essence (-> artifacts first :cost)} p1)
        :players (get p1) :public :artifacts count)))
;error code 2 - artifact not in hand
(expect 2
  (let [artifacts (-> g1 :players (get p1) :private :artifacts)]
    (-> g1 
        (ramodel/parseaction {:action :place :card (first artifacts) :essence (-> artifacts first :cost)} p1)
        (ramodel/parseaction {:action :place :card (first artifacts) :essence (-> artifacts first :cost)} p1)
        :players (get p1) :err)))

; After cards have been placed, start new turn, cards are still in order
(expect "Dragon Teeth"
  (let [artifacts (-> g1 :players (get p1) :private :artifacts)]
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
  (let [mage (-> g1 :players (get p1) :public :mage)] 
    (-> g1 
        (ramodel/parseaction {:action :usecard :useraction (-> mage :action first) :card mage} p1)
        :players (get p1) :public :essence :death)))
; placed
(expect {:death 98 :elan 98} 
  (let [artifact (-> g1 :players (get p1) :private :artifacts first)] ; Dragon Teeth {:cost {:elan 1 :death 1}
    (-> g1 
        (ramodel/parseaction {:action :place :card artifact :essence (:cost artifact)} p1)
        :players (get p1) :public :essence (select-keys [:elan :death]))))
; used (start 98 death), pay 0 death, GAIN 2 death
(expect 100
  (let [artifact (-> g1 :players (get p1) :private :artifacts last)] 
    (-> g1 
        (ramodel/parseaction {:action :place :card artifact :essence (:cost artifact)} p1)
        (ramodel/parseaction {:action :usecard :card artifact :useraction (-> artifact :action first)} p1)
        :players (get p1) :public :essence :death)))
; use (start 98 elan, PAY 2 elan, place 2 elan)
(expect 96
  (let [artifact (-> g1 :players (get p1) :private :artifacts first)] ; Dragon teeth {:action [{:turn true, :cost {:elan 2} :place {:elan 3}}]}
    (-> g1 
        (ramodel/parseaction {:action :place :card artifact :essence (:cost artifact)} p1)
        (ramodel/parseaction {:action :usecard :card artifact :useraction (-> artifact :action first)} p1)
        :players (get p1) :public :essence :elan)))
; RIVALS GAIN
(expect 100
  (let [p2 (-> g1 :plyr-to last)
        artifact (-> g1 :players (get p1) :private :artifacts last)]  ; Hand of Glory
    (-> g1 
        (ramodel/parseaction {:action :place :card artifact :essence (:cost artifact)} p1)
        (ramodel/parseaction {:action :usecard :useraction (-> artifact :action first) :card artifact} p1)
        :players (get p2) :public :essence :death)))

;; Place an essence (mage)
(expect 1
  (let [mage (-> g1 :players (get p1) :public :mage)] 
    (-> g1 
        (ramodel/parseaction {:action :usecard :useraction (-> mage :action first) :card mage} p1)
        :players (get p1) :public :mage :take-essence :gold)))
;; repeat (mage)
(expect 2
  (let [mage (-> g1 :players (get p1) :public :mage)] 
    (-> g1 
        (ramodel/parseaction {:action :usecard :useraction (-> mage :action first) :card mage} p1)
        (end-turn p1)
        (ramodel/parseaction {:action :usecard :useraction (-> mage :action first) :card mage} p1)
        :players (get p1) :public :mage :take-essence :gold)))
;; Place an essence (Artifact)
(expect 3
  (let [artifact (-> g1 :players (get p1) :private :artifacts first)] ; Dragon Teeth 
    (-> g1 
        (ramodel/parseaction {:action :place   :card artifact :essence (:cost artifact)} p1)
        (ramodel/parseaction {:action :usecard :card artifact :useraction (-> artifact :action first)} p1)
        :players (get p1) :public :artifacts first :take-essence :elan)))
; repeat (artifact)
(expect 6
  (let [artifact (-> g1 :players (get p1) :private :artifacts first)] ; Dragon Teeth 
    (-> g1 
        (ramodel/parseaction {:action :place   :card artifact :essence (:cost artifact)} p1)
        (ramodel/parseaction {:action :usecard :card artifact :useraction (-> artifact :action first)} p1)
        (end-turn p1)
        (ramodel/parseaction {:action :usecard :card artifact :useraction (-> artifact :action first)} p1)
        :players (get p1) :public :artifacts first :take-essence :elan)))

;; USE with custom cost and gain
(expect {:calm 1 :elan 1 :death 1}
  (let [g1x (ramodel/chat-handler g1 "/playcard Cursed Skull" p1) 
        cs (-> g1x :players (get p1) :public :artifacts first)]
    (-> g1x
        (ramodel/parseaction {:action :usecard :useraction {:turn true :cost {:life 1} :place {:calm 1, :elan 1, :death 1}} :card cs} p1)
        :players (get p1) :public :artifacts first :take-essence)))

;; Claim Pop
; Added to Artifacts
(expect "Sacred Grove"
  (let [pop1 (-> g1 :pops first)]
    (-> g1
        (ramodel/parseaction {:action :place :card pop1 :essence (:cost pop1)} p1)
        :players (get p1) :public :artifacts first :name)))
; removed from pop list
(expect "Catacombs of the Dead"
  (let [pop1 (-> g1 :pops first)]
    (-> g1
        (ramodel/parseaction {:action :place :card pop1 :essence (:cost pop1)} p1)
        :pops first :name)))
;; Claim Monument
; Added to Artifacts
(expect "Colossus"
  (let [mon1 (-> g1 :monuments :public first)]
    (-> g1
        (ramodel/parseaction {:action :place :card mon1 :essence (:cost mon1)} p1)
        :players (get p1) :public :artifacts first :name)))
; removed from monument list
(expect "Golden Statue"
  (let [mon1 (-> g1 :monuments :public first)]
    (-> g1
        (ramodel/parseaction {:action :place :card mon1 :essence (:cost mon1)} p1)
        :monuments :public first :name)))
; new monument draw
(expect 2
  (let [mon1 (-> g1 :monuments :public first)]
    (-> g1
        (ramodel/parseaction {:action :place :card mon1 :essence (:cost mon1)} p1)
        :monuments :public count)))
; new monument draw
(expect 7
  (let [mon1 (-> g1 :monuments :public first)]
    (-> g1
        (ramodel/parseaction {:action :place :card mon1 :essence (:cost mon1)} p1)
        :monuments :secret count)))

;; Place essence - Monument and Pop


; Turn Mage
(expect true
  (let [mage (-> g1 :players (get p1) :public :mage)] 
    (-> g1 
        (ramodel/parseaction {:action :usecard :useraction (-> mage :action first) :card mage} p1)
        :players (get p1) :public :mage :turned?)))
; Turn 'Artifact'
(expect true
  (let [artifact (-> g1 :players (get p1) :private :artifacts last)]
    (-> g1
        (ramodel/parseaction {:action :place :card artifact :essence (:cost artifact)} p1)
        (ramodel/parseaction {:action :usecard :useraction (-> artifact :action first) :card artifact} p1)
        :players (get p1) :public :artifacts first :turned?)))
; Turn Magic Item
(expect true
  (let [mi (->> g2 :magicitems (filter #(= (:owner %) p1)) first)]      ; Research 
    (->>  (ramodel/parseaction g2 {:action :usecard :useraction (-> mi :action first (assoc :cost {:death 1})) :card mi} p1)
          :magicitems
          (filter #(= (:owner %) p1)) first :turned?)))


; End Turn: Straighten (mage)
(expect nil
  (let [p1   (-> g1 :plyr-to first)  
        mage (-> g1 :players (get p1) :public :mage)] 
    (-> g1 
        (ramodel/parseaction {:action :usecard :card mage :useraction {:turn true, :cost {:death -1}, :place {:gold 1}} } p1)
        (end-turn p1)
        :players (get p1) :public :mage :turned?)))
; End Turn: Straighten 'Artifact'
(expect nil
  (let [artifact (-> g1 :players (get p1) :private :artifacts last)]
    (-> g1
        (ramodel/parseaction {:action :place :card artifact :essence (:cost artifact)} p1)
        (ramodel/parseaction {:action :usecard :useraction (-> artifact :action first) :card artifact} p1)
        (end-turn p1)
        :players (get p1) :public :artifacts first :turned?)))
; End Turn: Straighten MagicItem
(expect {nil 8}
  (let [mi (->> g2 :magicitems (filter #(= (:owner %) p1)) first)]      ; Research 
    (->>  (-> g2 
              (ramodel/parseaction {:action :usecard :useraction (-> mi :action first (assoc :cost {:death 1})) :card mi} p1)
              (end-turn p1))
          :magicitems
          (map :turned?)
          frequencies
          )))

; Straighten card through card power 'Mage'
(expect nil
  (let [p1   (-> g3 :plyr-to first)
        mage (-> g3 :players (get p1) :public :mage) ; Duelist
        mi   (->> g3 :magicitems (filter #(= (:owner %) p1)) first)]  ; Reanimate
    (-> g3
        (ramodel/parseaction {:action :usecard :useraction (-> mage :action first) :card mage} p1)
        (ramodel/parseaction {:action :usecard :useraction (assoc mi :cost {:elan 1} :straighten mage)} p1)
        :players (get p1) :public :mage :turned?)))          
; Straighten card through card power 'Magicitem'
(expect nil
  (let [p1   (-> g2 :plyr-to first)
        mage (-> g2 :players (get p1) :public :mage) ; Duelist
        mi   (->> g2 :magicitems (filter #(= (:owner %) p1)) first)]  ; Research
    (->> (-> g2
            (ramodel/parseaction {:action :usecard :useraction (-> mi :action first (assoc :cost {:death 1})) :card mi} p1) ; Use magicitem
            (ramodel/parseaction {:action :usecard :useraction {:turn true :cost {:death 2} :straighten mi} :card mage} p1)) ; Substitute Witch Power 
        :magicitems
        (filter #(= (:owner %) p1))
        first :turned?)))

; Straighten card through card power 'Artifact'
(expect nil
  (let [p1       (-> g3 :plyr-to first)
        artifact (-> g3 :players (get p1) :private :artifacts last)       ; Hand of Glory
        mi       (->> g3 :magicitems (filter #(= (:owner %) p1)) first)]  ; Reanimate
    (-> g3
        (ramodel/parseaction {:action :place :card artifact :essence (:cost artifact)} p1)
        (ramodel/parseaction {:action :usecard :useraction (-> artifact :action first) :card artifact} p1)
        (ramodel/parseaction {:action :usecard :useraction (assoc mi :cost {:elan 1} :straighten artifact)} p1)
        :players (get p1) :public :artifacts first :turned?)))          

;;; Use card - Draw
(expect 4
  (let [mi (->> g2 :magicitems (filter #(= (:owner %) p1)) first)]      ; Research 
    (-> g2
        (ramodel/parseaction {:action :usecard :useraction (-> mi :action first (assoc :cost {:death 1})) :card mi} p1)
        :players (get p1) :private :artifacts count)))
        
;;; Take Essences - hard-coded placed essence in ramodel/parseaction calls
; Take essence from card (mage)
(expect 100
  (let [mage (-> g1 :players (get p1) :public :mage)] ; Duelist :action [{:turn true :cost {:death 1} :place {:gold 1}}] 
    (-> g1 
        (ramodel/parseaction {:action :usecard :useraction (-> mage :action first) :card mage} p1)
        (ramodel/parseaction {:action :pass} p1)
        (ramodel/parseaction {:action :selectmagicitem :card (-> g1 :magicitems last)} p1)
        (ramodel/parseaction {:action :take-essence :card (-> g1 :players (get p1) :public :mage (assoc :take-essence {:gold 1}))} p1)
        :players (get p1) :public :essence :gold)))
(expect nil
  (let [mage (-> g1 :players (get p1) :public :mage)] ; Duelist :action [{:turn true :cost {:death 1} :place {:gold 1}}] 
    (-> g1 
        (ramodel/parseaction {:action :usecard :useraction (-> mage :action first) :card mage} p1)
        (ramodel/parseaction {:action :pass} p1)
        (ramodel/parseaction {:action :selectmagicitem :card (-> g1 :magicitems last)} p1)
        (ramodel/parseaction {:action :take-essence :card (-> g1 :players (get p1) :public :mage (assoc :take-essence {:gold 1}))} p1)
        :players (get p1) :public :mage :take-essence)))
;;; Take essence from card (artifact)
(expect 99
  (let [artifact (-> g1 :players (get p1) :private :artifacts first)] ; Dragon Teeth 
    (-> g1 
        (ramodel/parseaction {:action :place   :card artifact :essence (:cost artifact)} p1)                ; -1 elan - 1death
        (ramodel/parseaction {:action :usecard :card artifact :useraction (-> artifact :action first)} p1)  ; -2 elan 
        (end-turn p1)
        (ramodel/parseaction {:action :take-essence :card (assoc artifact :take-essence {:elan 3})} p1)
        :players (get p1) :public :essence :elan)))
;
(expect nil
  (let [artifact (-> g1 :players (get p1) :private :artifacts first)] ; Dragon Teeth 
    (-> g1 
        (ramodel/parseaction {:action :place   :card artifact :essence (:cost artifact)} p1)                ; -1 elan - 1death
        (ramodel/parseaction {:action :usecard :card artifact :useraction (-> artifact :action first)} p1)  ; -2 elan 
        (end-turn p1)
        (ramodel/parseaction {:action :take-essence :card (assoc artifact :take-essence {:elan 3})} p1)
        :players (get p1) :public :artifacts first :take-essence)))

;;; VP Check
; Basic functionality - 
(expect {"Colossus" 2}
    (let [mon1 (-> g1 :monuments :public first)]
      (-> g1
          (ramodel/parseaction {:action :place :card mon1 :essence (:cost mon1)} p1)
          (end-turn p1)
          :players (get p1) :vp)))
; Extended 1 - calculated VP
(expect {"Colossus" 2 "Catacombs of the Dead" 1}
  (let [pop1 (-> g1 :pops second)
        mon1 (-> g1 :monuments :public first)]
    (-> g1
        (ramodel/parseaction {:action :place :card mon1 :essence (:cost mon1)} p1)
        (ramodel/parseaction {:action :place :card pop1 :essence (:cost pop1)} p1)
        (ramodel/parseaction {:action :usecard :card pop1 :useraction (-> pop1 :action first)} p1)
        (end-turn p1)
        :players (get p1) :vp)))

; First player
(expect {"First Player" 1}
  (let [mi (->> g1 :magicitems (remove :owner) first)]
    (-> g1 
        (ramodel/parseaction {:action :pass} p1)
        (ramodel/parseaction {:action :selectmagicitem :card (:uid mi)} p1)
        :players (get p1) :vp)))



;;;; Chat Commands ;;;;;
;; Route chat commands through model.clj
(expect "This is a test message"
  (let [gid         (lobbytest/newgamegid)
        start-state (-> (model/startgame! gid) :games gid :state)
        p1          (-> start-state :plyr-to first)
        m1          (-> start-state :players (get p1) :private :mages first)
        mi1         (-> start-state :magicitems first)]
    (model/updategame! {:gid gid :action :selectmage :card (:uid m1)} p1)
    (model/updategame! {:gid gid :action :selectstartitem :card (:uid mi1)} p1)
    (model/updategame! {:gid gid :action :collected} p1)
    (model/addchat! gid p1 "This is a test message")
    (-> @model/appstate :games gid :state :chat last :msg)))
  
(expect "This is a test message"
   (-> g1 (ramodel/chat-handler "This is a test message" p1) :chat second :msg))
  
;(expect "help: /essence <essence name> <new value>"
;   (-> g1 (ramodel/chat-handler "/essence" p1) :chat last :msg))
;
;(expect :usercmdhelp
;   (-> g1 (ramodel/chat-handler "/essence" p1) :chat last :event))

(expect :usercmd
   (-> g1 (ramodel/chat-handler "/essence gold 200" p1) :chat second :event))

; Set essence
(expect "/essence gold 200"
   (-> g1 (ramodel/chat-handler "/essence gold 200" p1) :chat second :msg))
(expect 299
   (-> g1 (ramodel/chat-handler "/essence gold 200" p1) :players (get p1) :public :essence :gold))

(expect "Celestial Horse"
   (-> g1 (ramodel/chat-handler "/playcard Celestial Horse" p1) :players (get p1) :public :artifacts first :name))
(expect "Celestial Horse"
   (-> g1 (ramodel/chat-handler "/playcard celestial horse" p1) :players (get p1) :public :artifacts first :name))

(expect 1
  (let [ar (-> g1 :players (get p1) :private :artifacts first)] 
    (-> g1 
        (ramodel/chat-handler (str "/discard " (:name ar)) p1)
        :players (get p1) :public :discard count)))

(expect {:gold 99}
  (let [gs (ramodel/chat-handler g1 (str "/playcard Celestial Horse") p1)]
    (-> gs
        (ramodel/chat-handler (str "/setessence Celestial Horse gold 99") p1)
        :players (get p1) :public :artifacts first :take-essence)))
;;; Bug Tests ;;;
;; Remove essence from Place of Power / Monument
; Fixed in defn- remove-card-essence

;; BUG -> 
; 1. End Turn, Select Research Magic Item, 
; 2. Use Research Magic Item, 
; 3. Play card from hand - BUG: lose a card from hand
; FIX - g1 deck did nit generate :uid for (-> players p1 private artifacts)
; (1)
;(expect "Research"
;  (let [;        research (->> g1 :magicitems (filter #(= (:name %) "Research")) first)]
;    (->> (-> g1 
;            (ramodel/parseaction {:action :pass} p1)
;            (ramodel/parseaction {:action :selectmagicitem :card (:uid research) } p1)
;            (ramodel/parseaction {:action :collected} p1))
;        :magicitems
;        (filter #(= (:owner %) p1))
;        first :name)))
;; (2)
;(expect 5
;  (let [;        research (->> g1 :magicitems (filter #(= (:name %) "Research")) first)]
;    (-> g1 
;        (ramodel/parseaction {:action :pass} p1)
;        (ramodel/parseaction {:action :selectmagicitem :card (:uid research) } p1)
;        (ramodel/parseaction {:action :collected} p1)
;        (ramodel/parseaction {:action :usecard :card research :useraction (-> research :action first (assoc :cost {:calm 1}))} p1)
;        :players (get p1) :private :artifacts count)))
;(expect 4
;  (let [;        research (->> g1 :magicitems (filter #(= (:name %) "Research")) first)
;        r1state (-> g1 
;                    (ramodel/parseaction {:action :pass} p1)
;                    (ramodel/parseaction {:action :selectmagicitem :card (:uid research) } p1)
;                    (ramodel/parseaction {:action :collected} p1)
;                    (ramodel/parseaction {:action :usecard :card research :useraction (-> research :action first (assoc :cost {:calm 1}))} p1))
;        a2        (-> r1state :players (get p1) :private :artifacts second)]
;      (-> r1state 
;        ;(start-test "Play Card Bug")
;        (ramodel/parseaction {:action :place :card a2  :essence (:cost a2)} p1)
;        :players (get p1) :private :artifacts count)))

; CURSED FORGE - Turn in collect phase
(expect true
  (let [cf (-> g1 :pops (nth 2))]
      (-> g1
          (ramodel/parseaction {:action :place :card cf :essence (:cost cf)} p1)
          (end-turn p1)
          (ramodel/parseaction {:action :collect-essence :card cf :essence {:turn true}} p1)
          :players (get p1) :public :artifacts first :turned?)))
; CURSED FORGE - remove collect-essence
(expect nil
  (let [cf (-> g1 :pops (nth 2))]
      (-> g1
          (ramodel/parseaction {:action :place :card cf :essence (:cost cf)} p1)
          (end-turn p1)
          (ramodel/parseaction {:action :collect-essence :card cf :essence {:turn true}} p1)
          :players (get p1) :public :artifacts first :collect-essence)))

;;;;; LOSE LIFE ;;;;;

; Manual trigger for testing
(expect "Bone Dragon"
  (let [p1  (-> g4 :plyr-to first) 
        p1a (-> g4 :players (get p1) :private :artifacts last)
        p2  (-> g4 :plyr-to last)
        gs  (ramodel/chat-handler g4 "/playcard Bone Dragon" p1)] ; P1 starts with Bone Dragon
    (-> gs :players (get p1) :public :artifacts first :name)))
; Use BD Exhausts it
(expect true
  (let [p1  (-> g4 :plyr-to first) 
        p1a (-> g4 :players (get p1) :private :artifacts last)
        p2  (-> g4 :plyr-to last)
        gs  (ramodel/chat-handler g4 "/playcard Bone Dragon" p1)
        bd  (-> gs :players (get p1) :public :artifacts first)] ; P1 starts with Bone Dragon
    (-> gs 
        (ramodel/parseaction {:action :usecard :card bd :useraction (-> bd :action first)} p1)
        :players (get p1) :public :artifacts first :turned?)))
; use BD triggers :loselife effect
(expect {:turn true :loselife 2 :ignore {:death 1} :source :dragon :name "Bone Dragon" :type "artifact" :id 1 :plyr "p1"}
  (let [p1  (-> g4 :plyr-to first) 
        p2  (-> g4 :plyr-to last)
        gs  (ramodel/chat-handler g4 "/playcard Bone Dragon" p1)
        bd  (-> gs :players (get p1) :public :artifacts first)] ; P1 starts with Bone Dragon
    (-> gs 
        (ramodel/parseaction {:action :usecard :card bd :useraction (-> bd :action first)} p1)
        :players (get p2) :loselife)))
; immune to loselife if passed
(expect nil
  (let [p1  (-> g4 :plyr-to first) 
        p2  (-> g4 :plyr-to last)
        gs  (ramodel/chat-handler g4 "/playcard Bone Dragon" p2)
        bd  (-> gs :players (get p2) :public :artifacts first)] ; P2 starts with Bone Dragon
    (-> gs 
        (ramodel/parseaction {:action :pass} p1)
        (ramodel/parseaction {:action :usecard :card bd :useraction (-> bd :action first)} p2)
        :players (get p1) :loselife)))

; trigger same effect using chat commands 
(expect {:loselife 2 :ignore {:death 1} :name "User Action" :plyr "p1"}
  (let [p2 (-> g1 :plyr-to last)]
    (-> g1 
        (ramodel/chat-handler "/loselife life 2 death 1" p1)
        :players (get p2) :loselife)))

(expect :play 
  (let [p2 (-> g1 :plyr-to last)]
    (-> g1
        (ramodel/chat-handler "/endturn" p1)
        :players (get p2) :action)))
      
; trigger same effect using chat commands 
(expect {:loselife 2 :ignore {:gold 1} :name "User Action" :plyr "AI123"}
  (let [p2 (-> g1 :plyr-to last)]
    (-> g1
        (ramodel/chat-handler "/endturn" p1)
        (ramodel/chat-handler "/loselife life 2 gold 1 AI123" p1)
        :players (get p1) :loselife)))

(expect {:loselife 2 :ignore {:discard 1} :name "User Action" :plyr "AI123"}
  (let [p2 (-> g1 :plyr-to last)]
    (-> g1
        (ramodel/chat-handler "/endturn" p1)
        (ramodel/chat-handler "/loselife life 2 discard 1 AI123" p1)
        :players (get p1) :loselife)))

;; Discard response test
(expect 2
  (let [p1  (-> g4 :plyr-to first) 
        p2  (-> g4 :plyr-to last)
        gs  (ramodel/chat-handler g4 "/playcard Wind Dragon" p1)
        wd  (-> gs :players (get p1) :public :artifacts first)] ; P1 starts with Sea serpent
    (-> gs 
        (ramodel/parseaction {:action :usecard :card wd :useraction (-> wd :action first)} p1)
        (ramodel/parseaction {:action :react :useraction {:discard  (-> gs :players (get p2) :private :artifacts first)}} p2)
        :players (get p2) :private :artifacts count)))

;; Destroy response test
(expect 0
  (let [p1  (-> g4 :plyr-to first) 
        p2  (-> g4 :plyr-to last)
        gs  (-> g4 (ramodel/chat-handler "/playcard Sea Serpent" p1) (ramodel/chat-handler "/playcard Athanor" p2))
        ss  (-> gs :players (get p1) :public :artifacts first)] ; P1 starts with Sea serpent
    (-> gs 
        (ramodel/parseaction {:action :usecard :card ss :useraction (-> ss :action first)} p1)
        (ramodel/parseaction {:action :react :useraction {:destroy true :destroycard (-> gs :players (get p2) :public :artifacts first)}} p2)
        :players (get p2) :public :artifacts count)))

; do not end turn until all :loselife responses are completed
(expect :play
  (let [gs (ramodel/chat-handler g4 "/playcard Elvish Bow" p1)
        eb (-> gs :players (get p1) :public :artifacts first)]
    (-> g4
        (ramodel/parseaction {:action :usecard :card eb :useraction (-> eb :action first)} p1)
        :players (get p1) :action)))
(expect :waiting
  (let [p2 (-> g4 :plyr-to last)
        gs (ramodel/chat-handler g4 "/playcard Elvish Bow" p1)
        eb (-> gs :players (get p1) :public :artifacts first)]
    (-> g4
        (ramodel/parseaction {:action :usecard :card eb :useraction (-> eb :action first)} p1)
        (ramodel/parseaction {:action :react} p2)
        :players (get p1) :action)))


; react to life loss by paying cost
(expect 97 
  (let [p2 (-> g4 :plyr-to last)]
    (-> g4
        (ramodel/chat-handler "/endturn" p1)
        (ramodel/chat-handler "/loselife 2 death" p2)
        (ramodel/parseaction {:action :react :card {:type "reaction" :name "pay essence"} :useraction {:cost {:life 2}}} p1)
        :players (get p1) :public :essence :life)))
; react to life loss by paying cost
(expect 95
  (let [p2 (-> g4 :plyr-to last)]
    (-> g4
        (ramodel/chat-handler "/endturn" p1)
        (ramodel/chat-handler "/loselife 2 death" p2)
        (ramodel/parseaction {:action :react :card {:type "reaction" :name "pay essence"} :useraction {:cost {:elan 4}}} p1)
        :players (get p1) :public :essence :elan)))
(expect 98
  (let [p2 (-> g4 :plyr-to last)]
    (-> g4
        (ramodel/chat-handler "/endturn" p1)
        (ramodel/chat-handler "/loselife 2 death" p2)
        (ramodel/parseaction {:action :react :card {:type "reaction" :name "pay essence"} :useraction {:cost {:death 1}}} p1)
        :players (get p1) :public :essence :death)))
; use a component and turn it
(expect true 
  (let [p2 (-> g4 :plyr-to last)
        gs (-> g4 (ramodel/chat-handler "/playcard Chalice of Life" p1))
        col (-> gs :players (get p1) :public :artifacts first)]
    (-> gs
        (ramodel/chat-handler "/endturn" p1)
        (ramodel/chat-handler "/loselife 2 death" p2)
        (ramodel/parseaction {:action :react :card col :useraction (-> col :action last)} p1)
        :players (get p1) :public :artifacts first :turned?)))
; Not enough essence to pay total cost - take all remaining essence (Implement OK in raview)
(expect :play 
  (let [p2 (-> g4 :plyr-to last)
        gs (-> g4 (ramodel/chat-handler "/playcard Chalice of Life" p1))
        col (-> gs :players (get p1) :public :artifacts first)]
    (-> gs
        (ramodel/chat-handler "/endturn" p1)
        (ramodel/chat-handler "/loselife 2 death" p2)
        (ramodel/chat-handler "/essence life -99 elan -98 death -99 gold -99 calm -99" p1)
        (ramodel/parseaction {:action :react :card {:type "reaction" :name "pay essence"} :useraction {:cost {:elan 1}}} p1)
        :players (get p1) :action)))

;; Card Specific Tests ;;
; Collect {:special 36} - Vault - if gold left on card, {:any 2 :exclude #{:gold}}
(expect [{:any 2 :exclude #{:gold}}]
  (let [gs (ramodel/chat-handler g3 "/playcard Vault" p1)
        vault (-> gs :players (get p1) :public :artifacts first)]
      (-> gs
          (ramodel/parseaction {:action :usecard :card vault :useraction (-> vault :action first)} p1)
          (ramodel/parseaction {:action :pass} p1)
          (ramodel/parseaction {:action :selectmagicitem :card (->> gs :magicitems (remove :owner) first :uid)} p1)
          :players (get p1) :public :artifacts first :collect-essence )))
; Collect {:special 36} - Vault - if NO gold left on card, {:any 2 :exclude #{:gold}}
(expect nil
  (let [gs (ramodel/chat-handler g3 "/playcard Vault" p1)
        vault (-> gs :players (get p1) :public :artifacts first)]
      (-> gs
          ;(ramodel/parseaction {:action :usecard :card vault :useraction (-> vault :action first)} p1)
          (ramodel/parseaction {:action :pass} p1)
          (ramodel/parseaction {:action :selectmagicitem :card (->> gs :magicitems (remove :owner) first :uid)} p1)
          :players (get p1) :public :artifacts first :collect-essence )))
; Windup Man :place {:cost true}
(expect {:elan 1}
  (let [gs (ramodel/chat-handler g3 "/playcard Windup Man" p1)
        wm (-> gs :players (get p1) :public :artifacts first)]
      (-> gs
          (ramodel/parseaction {:action :usecard :card wm :useraction {:turn true :cost {:elan 1} :place {:cost true}}} p1)
          :players (get p1) :public :artifacts first :take-essence)))
; Collect {:special 39} - Windup Man - if any essence left on card, add 2 of it
(expect {:elan 3 :death 3}
  (let [gs (ramodel/chat-handler g3 "/playcard Windup Man" p1)
        wm (-> gs :players (get p1) :public :artifacts first)]
      (-> gs
          (ramodel/parseaction {:action :usecard :card wm :useraction {:turn true :cost {:elan 1 :death 1} :place {:cost true}}} p1)
          (end-turn p1)
          :players (get p1) :public :artifacts first :take-essence)))
; Collect {:special 39} - Windup Man - if NO essence left on card
(expect nil
  (let [gs (ramodel/chat-handler g3 "/playcard Windup Man" p1)
        wm (-> gs :players (get p1) :public :artifacts first)]
      (-> gs
          ;(ramodel/parseaction {:action :usecard :card wm :useraction {:turn true :cost {:elan 1 :death 1} :place {:cost true}}} p1)
          (end-turn p1)
          :players (get p1) :public :artifacts first :take-essence)))

;; Mermaid -> place calm on mage
(expect {:calm 1}
  (let [gs (-> g1 (ramodel/chat-handler "/playcard Mermaid" p1))
        mg (-> gs :players (get p1) :public :mage)
        mm (-> gs :players (get p1) :public :artifacts first)]
    (-> gs 
        (ramodel/parseaction {:action :usecard :card mm :useraction (-> mm :action first (assoc :targetany mg :cost {:calm 1}))} p1)
        :players (get p1) :public :mage :take-essence))) 
(expect nil
  (let [gs (-> g1 (ramodel/chat-handler "/playcard Mermaid" p1))
        mg (-> gs :players (get p1) :public :mage)
        mm (-> gs :players (get p1) :public :artifacts first)]
    (-> gs 
        (ramodel/parseaction {:action :usecard :card mm :useraction {:turn true, :cost {:calm 1}, :place {:cost true}, :targetany mg :gain nil}} p1)
        :players (get p1) :public :artifacts first :take-essence))) 

;; Revised chat commands
(expect {:command "essence" :essence {:gold 1 :death -2} :cardname nil :player "AI123"}
  (ramodel/regexp-result g1 "/essence gold 1 death -2 AI123" (-> g1 :plyr-to first)))

(expect {:command "playcard" :cardname "Chalice of Life" :essence nil :player "p1"}
  (ramodel/regexp-result g1 "/playcard Chalice of Life"  (-> g1 :plyr-to first)))

(expect true
  (let [gx (ramodel/chat-handler g1 "/playcard Mermaid" p1)]
    (-> gx
        (ramodel/chat-handler "/turn Mermaid" p1)
        :players (get p1) :public :artifacts first :turned?)))
(expect nil
  (let [gx (ramodel/chat-handler g1 "/playcard Mermaid" p1)]
    (-> gx
        (ramodel/chat-handler "/turn Mermaid" p1)
        (ramodel/chat-handler "/turn Mermaid" p1)
        :players (get p1) :public :artifacts first :turned?)))
(expect true
  (-> g1
      (ramodel/chat-handler "/turn Duelist" p1)
      :players (get p1) :public :mage :turned?))
(expect nil
  (-> g1
      (ramodel/chat-handler "/turn Duelist" p1)
      (ramodel/chat-handler "/turn Duelist" p1)
      :players (get p1) :public :mage :turned?))
(expect true
    (->>  (ramodel/chat-handler g2 "/turn Research" p1)
          :magicitems (filter #(= (:name %) "Research")) first :turned?))
(expect nil
  (->>  (-> g2
            (ramodel/chat-handler "/turn Research" p1)
            (ramodel/chat-handler "/turn Research" p1))
        :magicitems (filter #(= (:name %) "Research")) first :turned?))

(expect 4
  (-> g1 
      (ramodel/chat-handler "/draw" p1)
      :players (get p1) :private :artifacts count))
(expect 5
  (-> g1 
      (ramodel/chat-handler "/draw 2" p1)
      :players (get p1) :private :artifacts count))
(expect 8
  (-> g1 
      (ramodel/chat-handler "/draw 100" p1)
      :players (get p1) :private :artifacts count))
(expect 2
  (let [c1 (-> g1 :players (get p1) :private :artifacts first)]
    (-> g1 
        (ramodel/parseaction {:action :discard :card c1 :essence {:gold 1}} p1)
        :players (get p1) :private :artifacts count)))
(expect 8
  (let [c1 (-> g1 :players (get p1) :private :artifacts first)]
    (-> g1 
        (ramodel/parseaction {:action :discard :card c1 :essence {:gold 1}} p1)
        (ramodel/chat-handler "/draw 100" p1)
        :players (get p1) :private :artifacts count)))
(expect 0
  (let [c1 (-> g1 :players (get p1) :private :artifacts first)]
    (-> g1 
        (ramodel/parseaction {:action :discard :card c1 :essence {:gold 1}} p1)
        (ramodel/chat-handler "/draw 100" p1)
        :players (get p1) :public :discard count)))


;; Artifact order after end turn

;; draw card
(expect true
  (-> g1 
      (ramodel/chat-handler "/draw" p1)
      :players (get p1) :public :artifacts vector?))

;; end turn
(expect true
  (let [artifacts (-> g1 :players (get p1) :private :artifacts)]
    (-> g1
        (ramodel/parseaction {:action :place :card (first artifacts)  :essence (-> artifacts first :cost)} p1)
        (ramodel/parseaction {:action :place :card (second artifacts) :essence (-> artifacts second :cost)} p1)
        (ramodel/parseaction {:action :place :card (last artifacts)   :essence (-> artifacts last :cost)} p1)
        (ramodel/parseaction {:action :pass} p1)
        (ramodel/parseaction {:action :selectmagicitem :card (->> g1 :magicitems (remove :owner) first :uid)} p1)
        (ramodel/parseaction {:action :collected} p1)
        :players (get p1) :public :artifacts vector?)))

;; USE draw3
(expect true
  (let [m1 (-> g5 :players (get p1) :public :mage )]  ; Seer
    (-> g5
        (ramodel/parseaction {:action :usecard :card m1 :useraction (-> m1 :action first)} p1)
        :players (get p1) :draw3)))
;; USE draw3
(expect :play
  (let [m1 (-> g5 :players (get p1) :public :mage )]  ; Seer
    (-> g5
        (ramodel/parseaction {:action :usecard :card m1 :useraction (-> m1 :action first)} p1)
        :players (get p1) :action)))

;; Step1.1.0 CANCEL
;(expect nil
;  (let [;        m1 (-> g5 :players (get p1) :public :mage )]  ; Seer
;    (-> g5
;        (ramodel/parseaction {:action :usecard :card m1 :useraction (-> m1 :action first)} p1)
;        (ramodel/parseaction {:action :draw3   :cancel? true} p1)
;        :players (get p1) :draw3)))
;; Step1.1.1 - select deck {"player" {:draw3 <deck>} "MONUMENT"
(expect "monument"
  (let [m1 (-> g5 :players (get p1) :public :mage )]  ; Seer
    (-> g5
        (ramodel/parseaction {:action :usecard :card m1 :useraction (-> m1 :action first)} p1)
        (ramodel/parseaction {:action :draw3   :deck "monument"} p1)
        :players (get p1) :draw3)))
;; Step1.1.2 - 3 cards in {"player" {:private {:draw3 []}}}
(expect 3
  (let [m1 (-> g5 :players (get p1) :public :mage )]  ; Seer
    (-> g5
        (ramodel/parseaction {:action :usecard :card m1 :useraction (-> m1 :action first)} p1)
        (ramodel/parseaction {:action :draw3   :deck "monument"} p1)
        :players (get p1) :private :draw3 count)))
;; Step1.1.3 - 3 cards removed from {"player" {:secret }} ; TODO - do you really want ot 'MOVE' the cards here?
(expect 5
  (let [m1 (-> g5 :players (get p1) :public :mage )]  ; Seer
    (-> g5
        (ramodel/parseaction {:action :usecard :card m1 :useraction (-> m1 :action first)} p1)
        (ramodel/parseaction {:action :draw3   :deck "monument"} p1)
        (ramodel/obfuscate p1) :monuments :secret)))

;; Step1.2.1 - select deck {"player" {:draw3 <deck>} @uname
(expect "artifact"
  (let [m1 (-> g5 :players (get p1) :public :mage )]  ; Seer
    (-> g5
        (ramodel/parseaction {:action :usecard :card m1 :useraction (-> m1 :action first)} p1)
        (ramodel/parseaction {:action :draw3   :deck "artifact"} p1)
        :players (get p1) :draw3)))
;; Step1.2.2 - 3 cards in {"player" {:private {:draw3 []}}}
(expect 3
  (let [m1 (-> g5 :players (get p1) :public :mage )]  ; Seer
    (-> g5
        (ramodel/parseaction {:action :usecard :card m1 :useraction (-> m1 :action first)} p1)
        (ramodel/parseaction {:action :draw3   :deck "artifact"} p1)
        :players (get p1) :private :draw3 count)))
;; Step1.2.3 - 3 cards removed from {"player" {:secret }} ; TODO - do you really want ot 'MOVE' the cards here?
(expect 2
  (let [m1 (-> g5 :players (get p1) :public :mage )]  ; Seer
    (-> g5
        (ramodel/parseaction {:action :usecard :card m1 :useraction (-> m1 :action first)} p1)
        (ramodel/parseaction {:action :draw3   :deck "artifact"} p1)
        (ramodel/obfuscate p1) :players (get p1) :secret :artifacts)))
;; Step1.2.2 - 3 cards in {"player" {:private {:draw3 []}}} - Populate from Discard
;;; a) 2 cards in deck, 2 cards in discard
(expect [2 2]
  (let [m1 (-> g5 :players (get p1) :public :mage )  ; Seer
        ar (-> g5 :players (get p1) :private :artifacts)
        gs (-> g5 (ramodel/chat-handler "/draw 3" p1) 
                  (ramodel/chat-handler (str "/discard " (-> ar first :name)) p1) 
                  (ramodel/chat-handler (str "/discard " (-> ar last :name)) p1) 
                  )]
    [
      (-> gs :players (get p1) :public :discard count)
      (-> gs :players (get p1) :secret :artifacts count)
    ]))
;;; b) draw deck populated from discard
(expect 3
  (let [m1 (-> g5 :players (get p1) :public :mage )  ; Seer
        ar (-> g5 :players (get p1) :private :artifacts)
        gs (-> g5 (ramodel/chat-handler "/draw 3" p1) 
                  (ramodel/chat-handler (str "/discard " (-> ar first :name)) p1) 
                  (ramodel/chat-handler (str "/discard " (-> ar last :name)) p1) 
                  )]
    (-> gs
        (ramodel/parseaction {:action :usecard :card m1 :useraction (-> m1 :action first)} p1)
        (ramodel/parseaction {:action :draw3   :deck "artifact"} p1)
        :players (get p1) :private :draw3 count)))
;;; c) discard emptied
(expect 0
  (let [m1 (-> g5 :players (get p1) :public :mage )  ; Seer
        ar (-> g5 :players (get p1) :private :artifacts)
        gs (-> g5 (ramodel/chat-handler "/draw 3" p1) 
                  (ramodel/chat-handler (str "/discard " (-> ar first :name)) p1) 
                  (ramodel/chat-handler (str "/discard " (-> ar last :name)) p1) 
                  )]
    (-> gs
        (ramodel/parseaction {:action :usecard :card m1 :useraction (-> m1 :action first)} p1)
        (ramodel/parseaction {:action :draw3   :deck "artifact"} p1)
        :players (get p1) :public :discard count)))
;; Step 3.1 replace cards - monument 
(expect ["Library" "Great Pyramid" "Hanging Gardens"]
  (let [m1 (-> g5 :players (get p1) :public :mage )
        mons (-> g5 :monuments :secret)]  ; Seer
    (->>  (-> g5
              (ramodel/parseaction {:action :usecard :card m1 :useraction (-> m1 :action first)} p1)
              (ramodel/parseaction {:action :draw3   :deck "monument"} p1)
              (ramodel/parseaction {:action :draw3   :useraction [(nth mons 2) (first mons) (second mons)]} p1)
              :monuments :secret)
          (map :name)
          (take 3))))

;; Step 3.2 replace cards - artifact
(expect ["Dragon Egg" "Dragon Bridle" "Cursed Skull"]
  (let [m1 (-> g5 :players (get p1) :public :mage )
        arts (-> g5 :players (get p1) :secret :artifacts)]  ; Seer
    (->>  (-> g5
              (ramodel/parseaction {:action :usecard :card m1 :useraction (-> m1 :action first)} p1)
              (ramodel/parseaction {:action :draw3   :deck "artifact"} p1)
              (ramodel/parseaction {:action :draw3   :useraction [(nth arts 2) (second arts) (first arts)]} p1)
              :players (get p1) :secret :artifacts)
          (map :name)
          (take 3))))
;; remove :draw3 attributes
(expect nil
  (let [m1 (-> g5 :players (get p1) :public :mage )
        arts (-> g5 :players (get p1) :secret :artifacts)]  ; Seer
    (-> g5
        (ramodel/parseaction {:action :usecard :card m1 :useraction (-> m1 :action first)} p1)
        (ramodel/parseaction {:action :draw3   :deck "artifact"} p1)
        (ramodel/parseaction {:action :draw3   :useraction [(nth arts 2) (second arts) (first arts)]} p1)
        :players (get p1) :draw3)))
(expect nil
  (let [m1 (-> g5 :players (get p1) :public :mage )
        arts (-> g5 :players (get p1) :secret :artifacts)]  ; Seer
    (-> g5
        (ramodel/parseaction {:action :usecard :card m1 :useraction (-> m1 :action first)} p1)
        (ramodel/parseaction {:action :draw3   :deck "artifact"} p1)
        (ramodel/parseaction {:action :draw3   :useraction [(nth arts 2) (second arts) (first arts)]} p1)
        :players (get p1) :private :draw3)))
;; and end user turn
(expect :waiting
  (let [m1 (-> g5 :players (get p1) :public :mage )
        arts (-> g5 :players (get p1) :secret :artifacts)]  ; Seer
    (-> g5
        (ramodel/parseaction {:action :usecard :card m1 :useraction (-> m1 :action first)} p1)
        (ramodel/parseaction {:action :draw3   :deck "artifact"} p1)
        (ramodel/parseaction {:action :draw3   :useraction [(nth arts 2) (second arts) (first arts)]} p1)
        :players (get p1) :action)))
; Turnextra
(expect true
  (let [sg (-> g1 :pops first)
        gs (-> g1 (ramodel/chat-handler "/playcard Mermaid" p1))
        mm (-> gs :players (get p1) :public :artifacts first)]
    (-> gs
        (ramodel/parseaction {:action :place :card sg :cost (:cost sg)} p1)
        (ramodel/parseaction {:action :usecard :card sg :useraction {:turnextra mm :turn true :place {:life 1}}} p1)
        :players (get p1) :public :artifacts first :turned?)))

; destroy as part of card action (Jeweled Statuette)
(expect []
  (let [gs (ramodel/chat-handler g1 "/playcard Jeweled Statuette" p1)
        js (-> gs :players (get p1) :public :artifacts first)]
    (-> gs
        (ramodel/parseaction {:action :usecard :card js :useraction (assoc (-> js :action last) :destroycard js)} p1)
        :players (get p1) :public :artifacts)))

; gain-equal Athanor
; 0 set up an athanor with 6 elan
(expect 6
  (let [gs (-> g1 
              (ramodel/chat-handler "/playcard Athanor" p1)
              (assoc-in [:players p1 :public :artifacts 0 :take-essence :elan] 6))
        ath (-> g1 :players (get p1) :public :artifacts first)]
    (-> gs
        :players (get p1) :public :artifacts first :take-essence :elan)))
; 1 convert = use elan
(expect nil
  (let [gs (-> g1 
              (ramodel/chat-handler "/playcard Athanor" p1)
              (assoc-in [:players p1 :public :artifacts 0 :take-essence :elan] 6))
        ath (-> gs :players (get p1) :public :artifacts first)]
    (-> gs
        (ramodel/parseaction {:action :usecard :card ath :useraction (assoc (-> ath :action last) :remove {:elan 6} :convertfrom {:death 10} :convertto {:gold :equal})} p1)
        :players (get p1) :public :artifacts first :take-essence :elan)))
(expect 89
  (let [gs (-> g1 
              (ramodel/chat-handler "/playcard Athanor" p1)
              (assoc-in [:players p1 :public :artifacts 0 :take-essence :elan] 6))
        ath (-> gs :players (get p1) :public :artifacts first)]
    (-> gs
        (ramodel/parseaction {:action :usecard :card ath :useraction (assoc (-> ath :action last) :remove {:elan 6} :convertfrom {:death 10} :convertto {:gold :equal})} p1)
        :players (get p1) :public :essence :death)))
(expect 109
  (let [gs (-> g1 
              (ramodel/chat-handler "/playcard Athanor" p1)
              (assoc-in [:players p1 :public :artifacts 0 :take-essence :elan] 6))
        ath (-> gs :players (get p1) :public :artifacts first)]
    (-> gs
        (ramodel/parseaction {:action :usecard :card ath :useraction (assoc (-> ath :action last) :remove {:elan 6} :convertfrom {:death 10} :convertto {:gold :equal})} p1)
        :players (get p1) :public :essence :gold)))
  
; Buy Obelisk 
(expect "Obelisk"
  (let [ob (-> g2 :monuments :public first)]
    (:name ob)))
(expect "Obelisk"
  (let [ob (-> g2 :monuments :public first)]
    (-> g2
        (ramodel/parseaction {:action :place :card ob :gain {:life 6}} p1)
        :players (get p1) :public :artifacts first :name)))
(expect 105
  (let [ob (-> g2 :monuments :public first)]
    (-> g2
        (ramodel/parseaction {:action :place :card ob :gain {:life 6}} p1)
        :players (get p1) :public :essence :life)))
  
; Sorcerer's bestiary VP
(expect {"Bone Dragon" 1 "Sorcerer's Bestiary" 3} ; g3
  (let [sb (->> g3 :pops (filter #(= (:name %) "Sorcerer's Bestiary")) first)
        gs (->  g3 
                (ramodel/chat-handler "/playcard Mermaid" p1)     ; Creature
                (ramodel/chat-handler "/playcard Bone Dragon" p1) ; Dragon
            )]
    (-> gs
        (ramodel/parseaction {:action :place :card sb} p1)
        ;(end-turn p1)
        :players (get p1) :vp)))

; Elvish Bow
(expect #(some? %)
  (let [p2  (-> g5 :plyr-to last)
        gs  (-> g5 (ramodel/chat-handler "/playcard Elvish Bow" p1))
        eb  (-> gs :players (get p1) :public :artifacts first)]
    (-> gs 
        (ramodel/parseaction {:action :usecard :card eb :useraction (-> eb :action first)} p1)
        :players (get p2) :loselife)))

; Victory Check
(expect :gameover
  (let [sg (-> g1 :pops second)]
    (-> g1
        (ramodel/parseaction {:action :place :card sg} p1)
        (ramodel/parseaction {:action :usecard :card sg :useraction {:place {:death 10}}} p1)
        (end-turn p1)
        :status)))
(expect (+ (* 4 99) 91 95)
  (let [sg (-> g1 :pops second)]
    (-> g1
        (ramodel/parseaction {:action :place :card sg :essence {:life 8 :calm 4}} p1)
        (ramodel/parseaction {:action :usecard :card sg :useraction {:place {:death 10}}} p1)
        (end-turn p1)
        :scores first :tiebreaker)))

;; Card action: play card with reduced (0) cost
(expect "Earth Dragon"
  (let [gs (-> g1 
              (ramodel/chat-handler "/playcard Dragon Teeth" p1)
              (ramodel/chat-handler "/draw 10" p1))
        dt (-> gs :players (get p1) :public :artifacts first)
        ed (-> gs :players (get p1) :private :artifacts first)]
    (-> gs
        (ramodel/parseaction {:action :usecard :useraction (-> dt :action last (assoc :playcard ed))} p1) 
        :players (get p1) :public :artifacts last :name)))
(expect {:gold 99 :calm 99 :elan 96 :life 99 :death 99}
  (let [gs (-> g1 
              (ramodel/chat-handler "/playcard Dragon Teeth" p1)
              (ramodel/chat-handler "/draw 10" p1))
        dt (-> gs :players (get p1) :public :artifacts first)
        ed (-> gs :players (get p1) :private :artifacts first)]
    (-> gs
        (ramodel/parseaction {:action :usecard :useraction (-> dt :action last (assoc :playcard ed))} p1) 
        :players (get p1) :public :essence)))

;; Card Action, play card with reduced (:any x) cost
(expect {:gold 99 :elan 96 :calm 99 :life 99 :death 90}
  (let [p2  (-> g5 :plyr-to last)
        a1  (-> g5 :players (get p1) :private :artifacts first)
        gs  (-> g5 (ramodel/parseaction "/playcard Crypt" p2))
        cr  (-> gs :players (get p2) :public :artifacts first)]
    (-> gs
        (ramodel/parseaction {:action :discard :card a1 :essence {:gold 1}} p1)
        (ramodel/parseaction {:action :usecard :card cr :useraction (-> cr :action last (assoc :playcard a1 :cost {:elan 3 :death 9}))} p2)
        :players (get p2) :public :essence)))

;; Card Action, play card from opponents discard
(expect 0
  (let [p2  (-> g5 :plyr-to last)
        a1  (-> g5 :players (get p1) :private :artifacts first)
        gs  (-> g5 (ramodel/parseaction "/playcard Crypt" p2))
        cr  (-> gs :players (get p2) :public :artifacts first)]
    (-> gs
        (ramodel/parseaction {:action :discard :card a1 :essence {:gold 1}} p1)
        (ramodel/parseaction {:action :usecard :card cr :useraction (-> cr :action last (assoc :playcard a1))} p2)
        :players (get p1) :public :discard count)))
(expect true
  (let [p2  (-> g5 :plyr-to last)
        a1  (-> g5 :players (get p1) :private :artifacts first)
        gs  (-> g5 (ramodel/parseaction "/playcard Crypt" p2))
        cr  (-> gs :players (get p2) :public :artifacts first)]
    (-> gs
        (ramodel/parseaction {:action :discard :card a1 :essence {:gold 1}} p1)
        (ramodel/parseaction {:action :usecard :card cr :useraction (-> cr :action last (assoc :playcard a1))} p2)
        :players (get p2) :public :artifacts last (= a1))))

;; Divination draw3 - action discard
(expect 6
  (let [div (->> g5 :magicitems (filter #(= (:name %) "Divination")) first)]
    (-> g5 
        (ramodel/parseaction {:action :usecard :card div :useraction (-> div :action first)} p1)
        :players (get p1) :private :artifacts count)))
(expect :play
  (let [div (->> g5 :magicitems (filter #(= (:name %) "Divination")) first)]
    (-> g5 
        (ramodel/parseaction {:action :usecard :card div :useraction (-> div :action first)} p1)
        :players (get p1) :action)))
;; discard 3
(expect 3
  (let [div (->> g5 :magicitems (filter #(= (:name %) "Divination")) first)
        gs  (ramodel/parseaction g5 {:action :usecard :card div :useraction (-> div :action first)} p1)
        dc  (->> (-> gs :players (get p1) :private :artifacts) (take 3) (map :uid) set)]
    (-> gs
        (ramodel/parseaction {:action :usecard :card div :useraction (-> div :action first (assoc :divine-discard dc))} p1)
        :players (get p1) :private :artifacts count)))
(expect 3
  (let [div (->> g5 :magicitems (filter #(= (:name %) "Divination")) first)
        gs  (ramodel/parseaction g5 {:action :usecard :card div :useraction (-> div :action first)} p1)
        dc  (->> (-> gs :players (get p1) :private :artifacts) (take 3) (map :uid) set)]
    (-> gs
        (ramodel/parseaction {:action :usecard :card div :useraction (-> div :action first (assoc :divine-discard dc))} p1)
        :players (get p1) :public :discard count)))
(expect :waiting
  (let [div (->> g5 :magicitems (filter #(= (:name %) "Divination")) first)
        gs  (ramodel/parseaction g5 {:action :usecard :card div :useraction (-> div :action first)} p1)
        dc  (->> (-> gs :players (get p1) :private :artifacts) (take 3) (map :uid) set)]
    (-> gs
        (ramodel/parseaction {:action :usecard :card div :useraction (-> div :action first (assoc :divine-discard dc))} p1)
        :players (get p1) :action)))

;; Coral Castle - Check Victory
(expect "Coral Castle"
  (let [cc (-> g1 :pops (nth 3))]
    (-> g1 
        (ramodel/parseaction {:action :place :card cc :essence (:cost cc)} p1)
        :players (get p1) :public :artifacts first :name)))
(expect 7
  (let [cc    (-> g1 :pops (nth 3))
        cotd  (-> g1 :pops (nth 1))]
    (-> g1 
        (ramodel/parseaction {:action :place :card cc   :essence (:cost   cc)} p1)
        (ramodel/parseaction {:action :place :card cotd :essence (:cost cotd)} p1)
        (ramodel/chat-handler "/setessence Catacombs of the Dead death 7" p1)
        :players (get p1) :public :artifacts last :take-essence :death)))
(expect 7
  (let [cotd  (-> g1 :pops (nth 1))]
    (-> g1 
        (ramodel/parseaction {:action :place :card cotd :essence (:cost cotd)} p1)
        (ramodel/chat-handler "/setessence Catacombs of the Dead death 7" p1)
        :players (get p1) :vp (get "Catacombs of the Dead"))))

(expect 10
  (let [cc    (-> g1 :pops (nth 3))
        cotd  (-> g1 :pops (nth 1))]
    (-> g1 
        (ramodel/parseaction {:action :place :card cc   :essence (:cost   cc)} p1)
        (ramodel/parseaction {:action :place :card cotd :essence (:cost cotd)} p1)
        (ramodel/parseaction {:action :usecard :card cotd :useraction {:cost {:death 5} :place {:death 1}}} p1)
        (ramodel/parseaction {:action :usecard :card cotd :useraction {:cost {:death 5} :place {:death 1}}} p1)
        (ramodel/parseaction {:action :usecard :card cotd :useraction {:cost {:death 5} :place {:death 1}}} p1)
        (ramodel/parseaction {:action :usecard :card cotd :useraction {:cost {:death 5} :place {:death 1}}} p1)
        (ramodel/parseaction {:action :usecard :card cotd :useraction {:cost {:death 5} :place {:death 1}}} p1)
        (ramodel/parseaction {:action :usecard :card cotd :useraction {:cost {:death 5} :place {:death 1}}} p1)
        (ramodel/parseaction {:action :usecard :card cotd :useraction {:cost {:death 5} :place {:death 1}}} p1)
        (ramodel/parseaction {:action :usecard :card cc   :useraction (-> cc :action first)} p1)
        ;(end-turn p1)
        :scores first :score)))
