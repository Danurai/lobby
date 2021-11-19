(ns lobby.ratest
  (:require
    [expectations :refer :all]
    [lobby.model :as model]
    [lobby.ramodel :as ramodel]
    [lobby.lobbytest :refer :all]))
    
    
; RA
;; parse action
(expect {}
  (ramodel/parseaction {} {:uname "testname"} "p1"))
(expect {}
  (ramodel/parseaction {} {} "p1"))

 
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
            (ramodel/selectmage {:card (:uid m1)} "p1")
            :players (get "p1") :public :mage))))

; ..even if they do it multiple times
(expect true
  (let [gs (ramodel/setup ["p1" "p2"])
        m1 (-> gs :players (get "p1") :private :mages first)
        m2 (-> gs :players (get "p1") :private :mages last)]
    (=  (:uid m1)
        (-> gs 
            (ramodel/selectmage {:card (:uid m1)} "p1")
            (ramodel/selectmage {:card (:uid m2)} "p1")
            (ramodel/selectmage {:card (:uid m1)} "p1")
            :players (get "p1") :public :mage))))
          
; AI and P1 have selected a mage
(expect 2
  (let [aip (-> "AI" gensym str)
        gs (ramodel/setup ["p1" aip])
        m1 (-> gs :players (get "p1") :private :mages first)]
    (->> (ramodel/selectmage gs {:card (:uid m1)} "p1")
         :players
         (reduce-kv #(if (-> %3 :public :mage some?) (inc %1) %1) 0))))
                  
                  
;; Select Magic Item
; reverse turn order
(expect :selectstartitem 
  (let [gs (ramodel/setup ["p1" "p2"])
        m1 (-> gs :players (get "p1") :private :mages first)
        m2 (-> gs :players (get "p2") :private :mages first)]
    (-> gs
        (ramodel/selectmage {:card (:uid m1)} "p1")
        (ramodel/selectmage {:card (:uid m2)} "p2")
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
        (ramodel/selectmage {:card (:uid m1)} "p1")
        (ramodel/selectmage {:card (:uid m2)} "p2")
        :players)))))
; Select Item    
(expect 1
  (let [gs (ramodel/setup ["p1" "p2"])
        m1 (-> gs :players (get "p1") :private :mages first)
        m2 (-> gs :players (get "p2") :private :mages first)
        mi1 (-> gs :magicitems first)]
    (count (filter :owner
        (-> gs
          (ramodel/selectmage {:card (:uid m1)} "p1")
          (ramodel/selectmage {:card (:uid m2)} "p2")
          (ramodel/selectstartitem {:card (:uid mi1)} (-> gs :plyr-to last))
          :magicitems
          )))))
; prevent selecting an item already taken
(expect "p1"
  (let [gs (ramodel/setup ["p1" "p2"])
        m1 (-> gs :players (get "p1") :private :mages first)
        m2 (-> gs :players (get "p2") :private :mages first)
        mi1 (-> gs :magicitems first)]
    (-> gs
      (ramodel/selectmage {:card (:uid m1)} "p1")
      (ramodel/selectmage {:card (:uid m2)} "p2")
      (ramodel/selectstartitem {:card (:uid mi1)} "p1")
      (ramodel/selectstartitem {:card (:uid mi1)} "p2")
      :magicitems first :owner
      )))

(expect :pass
  (let [gs (ramodel/setup ["p1" "p2"])
        m1 (-> gs :players (get "p1") :private :mages first)
        m2 (-> gs :players (get "p2") :private :mages first)
        mi1 (-> gs :magicitems first)]
    (-> gs
      (ramodel/selectmage {:card (:uid m1)} "p1")
      (ramodel/selectmage {:card (:uid m2)} "p2")
      (ramodel/selectstartitem {:card (:uid mi1)} (-> gs :plyr-to last))
      :players (get (-> gs :plyr-to last)) :action)))
      
;; 1player INVALID Mage ID
(expect nil
  (let [gs (ramodel/setup ["p1"])
        m1 (-> gs :players (get "p1") :private :mages first)]
    (-> gs
        (ramodel/selectmage {} "p1")
        (model/obfuscate-gm "p1")
        :players (get "p1") :public :mage)))
(expect :selectmage
  (let [gs (ramodel/setup ["p1"])
        m1 (-> gs :players (get "p1") :private :mages first)]
    (-> gs
        (ramodel/selectmage {} "p1")
        (model/obfuscate-gm "p1")
        :players (get "p1") :action)))
        
;; select mage - set public 
        
      
; one at a time
(expect 1
  (let [gs (ramodel/setup ["p1" "p2"])
        m1 (-> gs :players (get "p1") :private :mages first)
        m2 (-> gs :players (get "p2") :private :mages first)
        mi1 (-> gs :magicitems first)]
    (count (filter 
      (fn [[k v]] (= (:action v) :selectstartitem))
      (-> gs
        (ramodel/selectmage {:card (:uid m1)} "p1")
        (ramodel/selectmage {:card (:uid m2)} "p2")
        (ramodel/selectstartitem {:card (:uid mi1)} (-> gs :plyr-to last))
        :players)))))
;; in reverse player order
(expect :selectstartitem 
  (let [gs (ramodel/setup ["p1" "p2"])
        m1 (-> gs :players (get "p1") :private :mages first)
        m2 (-> gs :players (get "p2") :private :mages first)
        mi1 (-> gs :magicitems first)]
    (-> gs
        (ramodel/selectmage {:card (:uid m1)} "p1")
        (ramodel/selectmage {:card (:uid m2)} "p2")
        (ramodel/selectstartitem {:card (:uid mi1)} (-> gs :plyr-to last))
        :players  (get (-> gs :plyr-to first)) :action)))
      
(defn started2pgm []
  (let [setup  (assoc (ramodel/setup ["p1" "p2"]) :plyr-to (sorted-set "p1" "p2"))]
    (-> setup
        (ramodel/selectmage {:card (-> setup :players (get "p1") :private :mages first :uid)} "p1")
        (ramodel/selectmage {:card (-> setup :players (get "p2") :private :mages first :uid)} "p1")
        (ramodel/selectstartitem {:card (-> setup :magicitems first :uid)} "p2")
        (ramodel/selectstartitem {:card (-> setup :magicitems last :uid)} "p1"))))
        
; All players selected a mage (including AI setup), and a Magic Item game on!
(expect :play
  (-> (started2pgm) :status))
  
;; With AI
(expect :play
  (let [aip "AI123"
        gs (ramodel/setup ["p1" aip])
        m1 (-> gs :players (get "p1") :private :mages first)
        mi1 (-> gs :magicitems first)]   
    (-> gs
        (ramodel/selectmage {:card (:uid m1)} "p1")
        (ramodel/selectstartitem {:card (:uid mi1)} "p1")
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

    
; New round places resources on cards
(expect true
  (let [gs  (started2pgm)]
    (=  (-> gs :players (get "p1") :public :mage :collect)
        (-> gs :players (get "p1") :public :mage :collect-resource))))
    
; Take Resource from card
(expect 2
  (let [gs (started2pgm)]
    (-> gs 
        (ramodel/collect-resource {:resources {:death 1} :card (-> gs :players (get "p1") :public :mage)} "p1")
        :players (get "p1") :public :resources :death)))

(expect nil
  (let [gs (started2pgm)]
    (-> gs 
        (ramodel/collect-resource {:resources {:death 1} :card (-> gs :players (get "p1") :public :mage)} "p1")
        :players (get "p1") :public :mage :collectresource)))    
    
    
; Resource Change

(expect {:life 1 :death 1 :elan 1 :calm 1 :gold 1}
  (-> (ramodel/setup ["p1"]) :players (get "p1") :public :resources))
      
(expect {:life 1 :death 1 :elan 1 :calm 1 :gold 1}
  (-> (ramodel/setup ["p1"]) 
      (ramodel/amendresource {:resources {}} "p1") :players (get "p1") :public :resources))
      
(expect {:life 0 :death 1 :elan 2 :calm 3 :gold 4}
  (-> (ramodel/setup ["p1"]) 
      (ramodel/amendresource {:resources {:life -1 :elan 1 :calm 2 :gold 3} :action :amendresource} "p1")
      :players (get "p1") :public :resources))
      

; Play card from hand
(expect [1 2 {:life 1 :death 1 :elan 1 :calm 1 :gold 0}]
  (let [gsetup  (ramodel/setup ["p1"])
        m1      (-> gsetup :players (get "p1") :private :mages first)
        mi1     (-> gsetup :magicitems first)
        gs      (-> gsetup 
                    (ramodel/selectmage {:card (:uid m1)} "p1")
                    (ramodel/selectstartitem {:card (:uid mi1)} "p1"))
        art1    (-> gs :players (get "p1") :private :artifacts first)
        art2    (-> gs :players (get "p1") :private :artifacts last)
        afterplay (ramodel/playcard gs {:card art1 :resources {:gold 1}} "p1")
        ]
    [ (-> afterplay :players (get "p1") :public  :artifacts count)
      (-> afterplay :players (get "p1") :private :artifacts count)
      (-> afterplay :players (get "p1") :public  :resources) ]))

       
; Done
(expect  [:waiting :play] ; current player action = :waiting, next player action = :play
  (let [gs (ramodel/done (started2pgm) "p1")]
    [(-> gs :players (get "p1") :action)
     (-> gs :players (get "p2") :action)]))
     
(expect  [:play :waiting] ; current player action = :waiting, next player action = :play
  (let [gs (-> (started2pgm)
               (ramodel/done "p1")
               (ramodel/done "p2"))]
    [(-> gs :players (get "p1") :action)
     (-> gs :players (get "p2") :action)]))

; PASS     
;; p1 pass
(expect  ["p1" ["p2"] :selectmagicitem :waitingforpass] ; Pass Turn Order; New Turn Order; Pass Player :action; Other Player :action
  (let [gs (-> (started2pgm)
               (ramodel/pass "p1"))]
    [(-> gs :pass-to first)
     (-> gs :plyr-to)
     (-> gs :players (get "p1") :action)
     (-> gs :players (get "p2") :action)]))
;; p1 pass, choose MagicItem
(expect  [["p1" "p2"] ["p2"] :pass :play 4 4] ; Pass Turn Order; New Turn Order; Pass player action; Other player action; Artifacts in Hand; Artifacts in Deck
  (let [s2pg  (started2pgm)
        gs (-> s2pg
               (ramodel/pass "p1")
               (ramodel/selectmagicitem {:card (-> s2pg :state :magicitems (nth 3))} "p1"))]
    [(-> gs :pass-to)
     (-> gs :plyr-to)
     (-> gs :players (get "p1") :action)
     (-> gs :players (get "p2") :action)
     (-> gs :players (get "p1") :private :artifacts count)
     (-> gs :players (get "p1") :secret :artifacts count)]))
;;; p2 pass 
(expect  ["p2" ["p1"] :waitingforpass :selectmagicitem] ; current player action = :waiting, next player action = :play
  (let [gs (-> (started2pgm)
               (ramodel/done "p1")
               (ramodel/pass "p2"))]
    [(-> gs :pass-to first)
     (:plyr-to gs)
     (-> gs :players (get "p1") :action)
     (-> gs :players (get "p2") :action)]))
;; p2 pass, choose mi
(expect  [["p2" "p1"] ["p1"] :play :pass] ; Empty pass-to Turnorder based on pass-to players set to :play or :waiting
  (let [s2pg  (started2pgm)
        gs (-> s2pg
               (ramodel/done "p1")
               (ramodel/pass "p2")
               (ramodel/selectmagicitem {:card (-> s2pg :state :magicitems (nth 3) :uid)} "p2"))]
    [(-> gs :pass-to)
     (-> gs :plyr-to)
     (-> gs :players (get "p1") :action)
     (-> gs :players (get "p2") :action)]))
;; P2 passed, P1 pass
(expect  [["p2" "p1"] [] :selectmagicitem :pass] ; Empty pass-to Turnorder based on pass-to players set to :play or :waiting
  (let [s2pg  (started2pgm)
        gs (-> s2pg
               (ramodel/done "p1")
               (ramodel/pass "p2")
               (ramodel/selectmagicitem {:card (-> s2pg :state :magicitems (nth 3) :uid)} "p2")
               (ramodel/pass "p1")
               )]
    [(-> gs :pass-to)
     (-> gs :plyr-to)
     (-> gs :players (get "p1") :action)
     (-> gs :players (get "p2") :action)]))

;; P2 passed, P1 pass & select MI
(expect  [[] ["p2" "p1"] :waiting :play] ; Empty pass-to Turnorder based on pass-to players set to :play or :waiting
  (let [s2pg  (started2pgm)
        gs (-> s2pg
               (ramodel/done "p1")
               (ramodel/pass "p2")
               (ramodel/selectmagicitem {:card (-> s2pg :state :magicitems (nth 3) :uid)} "p2")
               (ramodel/pass "p1")
               (ramodel/selectmagicitem {:card (-> s2pg :state :magicitems (nth 4) :uid)} "p1")
               )]
    [(-> gs :pass-to)
     (-> gs :plyr-to)
     (-> gs :players (get "p1") :action)
     (-> gs :players (get "p2") :action)]))

      
; Toggle card exhausted
;; Magic Item
(expect true
  (let [gsetup  (ramodel/setup ["p1"])
        m1      (-> gsetup :players (get "p1") :private :mages first)
        mi1     (-> gsetup :magicitems first)
        gs      (-> gsetup 
                    (ramodel/selectmage {:card (:uid m1)} "p1")
                    (ramodel/selectstartitem {:card (:uid mi1)} "p1"))]
    (->> (ramodel/exhausttoggle gs {:card mi1} "p1") :magicitems
         (filter #(= (:uid %) (:uid mi1))) first :exhausted)))
;; Magic Item toggle
(expect nil
  (let [gsetup  (ramodel/setup ["p1"])
        m1      (-> gsetup :players (get "p1") :private :mages first)
        mi1     (-> gsetup :magicitems first)
        gs      (-> gsetup 
                    (ramodel/selectmage {:card (:uid m1)} "p1")
                    (ramodel/selectstartitem {:card (:uid mi1)} "p1"))]
    (->> (-> gs (ramodel/exhausttoggle {:card mi1} "p1") (ramodel/exhausttoggle {:card mi1} "p1"))
         :magicitems
         (filter #(= (:uid %) (:uid mi1))) first :exhausted)))
;; Mage
(expect true
  (let [gsetup  (ramodel/setup ["p1"])
        m1      (-> gsetup :players (get "p1") :private :mages first)
        mi1     (-> gsetup :magicitems first)
        gs      (-> gsetup 
                    (ramodel/selectmage {:card (:uid m1)} "p1")
                    (ramodel/selectstartitem {:card (:uid mi1)} "p1"))]
    (-> gs (ramodel/exhausttoggle {:card m1} "p1") :players (get "p1") :public :mage :exhausted)))
;; Mage toggle
(expect nil
  (let [gsetup  (ramodel/setup ["p1"])
        m1      (-> gsetup :players (get "p1") :private :mages first)
        mi1     (-> gsetup :magicitems first)
        gs      (-> gsetup 
                    (ramodel/selectmage {:card (:uid m1)} "p1")
                    (ramodel/selectstartitem {:card (:uid mi1)} "p1"))]
    (-> gs 
       (ramodel/exhausttoggle {:card m1} "p1")
       (ramodel/exhausttoggle {:card m1} "p1")
       :players (get "p1") :public :mage :exhausted)))
;; Artifact
(expect true
  (let [gsetup  (ramodel/setup ["p1"])
        m1      (-> gsetup :players (get "p1") :private :mages first)
        mi1     (-> gsetup :magicitems first)
        gs      (-> gsetup 
                    (ramodel/selectmage {:card (:uid m1)} "p1")
                    (ramodel/selectstartitem {:card (:uid mi1)} "p1")
                    )
        art1    (-> gs :players (get "p1") :private :artifacts first)]
    (-> gs 
       (ramodel/playcard {:card art1} "p1")
       (ramodel/exhausttoggle {:card art1} "p1")
       :players (get "p1") :public :artifacts first :exhausted)))
;; Artifact - toggle
(expect nil
  (let [gsetup  (ramodel/setup ["p1"])
        m1      (-> gsetup :players (get "p1") :private :mages first)
        mi1     (-> gsetup :magicitems first)
        gs      (-> gsetup 
                    (ramodel/selectmage {:card (:uid m1)} "p1")
                    (ramodel/selectstartitem {:card (:uid mi1)} "p1"))
        art1    (-> gs :players (get "p1") :private :artifacts first)]
    (-> gs 
       (ramodel/playcard {:card art1} "p1")
       (ramodel/exhausttoggle {:card art1} "p1")
       (ramodel/exhausttoggle {:card art1} "p1")
       :players (get "p1") :public :artifacts first :exhausted)))

    
    
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
      :discard))

(expect 0
  (-> (ramodel/setup ["p1" "p2"])
      (ramodel/obfuscate "p1") 
      :players 
      (get "p2")
      :secret
      :discard))

; ai collect generated resources test state
(def rstate {
  :chat []
  :status :play
  :phase :collect
  :magicitems [
    {:name "TESTMAGICITEM" :type "magicitem" :collect-resource [{:elan 1} {:gold 1}] :owner "AI1"}
    {:name "TESTMAGICITEM" :type "magicitem" :collect-resource [{:elan 1} {:gold 1}] :owner "P1"}
  ]
  :players {
    "AI1" {
      :public {
        :mage {:type "mage" :name "TESTMAGE" :collect-resource [{:gold 1} {:death 1}]}
        :resources {
          :gold 1
          :calm 1
          :life 1
          :elan 1
          :death 1
        }
        :artifacts [
          {:id 4  :type "artifact" :name "Chalice of Life"  :cost {:gold 1 :elan 1} :collect [{:calm 1 :life 1}] :collect-resource [{:calm 1 :life 1}]}
        ]
      }
    }
    "P1" {
      :public {
        :resources {
          :calm 1
          :life 1
        }
        :mage {:type "mage" :name "TESTMAGE" :collect-resource [{:gold 1} {:death 1}]}
        :artifacts [
          {:id 4  :type "artifact" :name "Chalice of Life"  :cost {:gold 1 :elan 1} :collect [{:calm 1 :life 1}] :collect-resource [{:calm 1 :life 1}]}
        ]
      }
    }
  }
})

; Check ramodel/amendresource
(expect {:gold 2 :life 2}
  (-> {:chat [] :players {"AI1" {:public {:resources {:gold 1 :life 1}}}}}
    (ramodel/amendresource {:resources {:gold 1 :life 1} :card {:name "TESTCARD"}} "AI1")
    :players 
    (get "AI1")
    :public
    :resources))

; Check ramodel/ai-collect-resources works
(expect {:calm 2 :life 2 :gold 2 :elan 2 :death 1}
  (-> rstate ramodel/ai-collect-resources :players (get "AI1") :public :resources))
; Check ramodel/ai-collect-resources works
(expect true
  (-> rstate ramodel/ai-collect-resources :players (get "AI1") :collected?))

; remove from mage
(expect nil
  (-> rstate ramodel/ai-collect-resources :players (get "AI1") :public :mage :collect-resource))
; remove from magicitem
(expect nil
  (-> rstate ramodel/ai-collect-resources :magicitems first :collect-resource))
; remove from artifact
(expect nil
  (-> rstate ramodel/ai-collect-resources :players (get "AI1") :public :artifacts first :collect-resource))

; but not for non-ai (#"AI/d+")
(expect 1
  (-> rstate ramodel/ai-collect-resources :players (get "P1") :public :resources :calm))

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

; Collect-to-Action remove all :collect-resource from Mage, MagicItems and Artifacts
(expect nil
  (-> rstate
      (assoc-in [:players "AI1" :collected?] true)
      (ramodel/parseaction {:action :collected} "P1")
      :players (get "P1") :public :mage :collect-resource ))
(expect nil
  (-> rstate
      (assoc-in [:players "AI1" :collected?] true)
      (ramodel/parseaction {:action :collected} "P1")
      :players (get "P1") :public :artifacts first :collect-resource ))
(expect nil
  (-> rstate
      (assoc-in [:players "AI1" :collected?] true)
      (ramodel/parseaction {:action :collected} "P1")
      :magicitems first :collect-resource ))