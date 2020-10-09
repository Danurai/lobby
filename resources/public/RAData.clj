{
  :radata {
    :counts {
      :artifacts 40
      :mages 10
      :monuments 10
      :placesofpower "5*2"
      :magicitems 8
    }
    :mages [
      {:id "ma1"  :name "Druid" :type "Mage" :collect [:life] :action {:desc "[turn]>[unturncreature]" :fn #(turn unturn :creature)}}
      {:id "ma2"  :name "Duelist" :collect [:elan] :action {:desc "[turn]+[discard :death 1]>[place :gold]" :fn #((turn)(discard :death)(place :gold))}
      {:id "ma3"  :name "Transmuter" :collect [] :action {:desc "[turn]+[discard :any 2]>[gain :any 3 notgold]" :fn #((turn)(discard :any 2)(gain :any 3 :notgold))}}
      {:id "ma4"  :name "" :collect [] :action []}
      {:id "ma5"  :name "" :collect [] :action []}
      {:id "ma6"  :name "" :collect [] :action []}
      {:id "ma7"  :name "" :collect [] :action []}
      {:id "ma8"  :name "" :collect [] :action []}
      {:id "ma9"  :name "" :collect [] :action []}
      {:id "ma10" :name "" :collect [] :action []}
    ]
    :monuments [
      {:id "mo1"  :name "Great Pyramid" :type "Monument" :cost {:gold 4} :action [] :vp 3}
      {:id "mo2"  :name "Colossus" :type "Monument" :cost {:gold 4} :action [] :vp 1}
      {:id "mo3"  :name "" :type "Monument" :cost {:gold 4} :action {:desc "[turn]+[discard :any]>" :fn #()} :vp 1}
      {:id "mo4"  :name "" :type "Monument" :cost {:gold 4} :action {:desc "" :fn #()} :vp 1}
      {:id "mo5"  :name "" :type "Monument" :cost {:gold 4} :action {:desc "" :fn #()} :vp 1}
      {:id "mo6"  :name "" :type "Monument" :cost {:gold 4} :action {:desc "" :fn #()} :vp 1}
      {:id "mo7"  :name "" :type "Monument" :cost {:gold 4} :action {:desc "" :fn #()} :vp 1}
      {:id "mo8"  :name "" :type "Monument" :cost {:gold 4} :action {:desc "" :fn #()} :vp 1}
      {:id "mo9"  :name "" :type "Monument" :cost {:gold 4} :action {:desc "" :fn #()} :vp 1}
      {:id "mo10" :name "" :type "Monument" :cost {:gold 4} :action {:desc "" :fn #()} :vp 1}
    ]
    :placesofpower [
      {:id "pop1" :name "Catacombs of the Dead" :type "Place of Power" :cost {:death 9} :action [] :vp nil}
      {:id "pop2" :name "Sacred Grove" :type "Place of Power" :cost {} :action [] :vp nil}
      {:id "pop3" :name "Cursed Forge" :type "Place of Power" :cost {} :action [] :vp nil}
      {:id "pop4" :name "Coral Castle" :type "Place of Power" :cost {} :action [] :vp nil}
      {:id "pop5" :name "Dragon's Lair" :type "Place of Power" :cost {} :action [] :vp nil}
    ]
    :magicitems [
      {:id "mi1" :name "Alchemy" :action {:desc "[turn]+[discard :any 4]>[gain :gold 2]" :fn #()}}
      {:id "mi2" :name "Reanimate" :action {:desc "[turn]+[discard :any 1]>[unturn]" :fn #()}
      {:id "mi3" :name "Calm | Elan" :collect "collect or :calm :elan"}
      {:id "mi4" :name "Death | Life" :collect "collect or :death :life"}
      {:id "mi5" :name "Protection" :action {:desc "[react][lose :life :any][turn]>[ignore]" :fn #()}}
      {:id "mi6" :name "Divination" :action {:desc "[turn]>[draw 3][discard 3]"}
      {:id "mi7" :name "Research" :action {:desc "[turn]+[discard :any 1]>[draw]" :fn #()}
      {:id "mi8" :name "Transmutation" :action {:desc "[turn]+[discard :any 3]>[gain :any 3][not :gold]" :fn #()}}
    ]
    :artifacts [
      {:id "ar1"  :name "Ring of Midas" :cost {:life 1 :gold 1} :collect nil :action [{:desc "[discard :life 2]>[place :gold]" :fn #()}{:desc "[tap]>[place gold]" :fn #()}] :vp 1}
      {:id "ar2"  :name "Hawk" :type "Creature" :cost {:life 1 :calm 1} :collect nil :action [] :vp nil}
      {:id "ar3"  :name "Fire Dragon" :type "Dragon" :cost {:elan 6} :collect nil :action [] :vp nil}
      {:id "ar4"  :name "Guard Dog" :type "Creature" :cost {:elan 1} :collect nil :action [] :vp nil}
      {:id "ar5"  :name "Fiery Whip" :type nil :cost [] :collect [] :action [] :vp nil}
      {:id "ar6"  :name "Sea Serpent" :type ["Dragon" "Creature"] :cost {} :collect nil :action [] :vp 1}
      {:id "ar7"  :name "Nightingale" :type "Creature" :cost {:life 1 :calm 1} :collect nil :action [] :vp nil}
      {:id "ar8"  :name "Celestial Horse" :type "Creature" :cost {:calm 2 :elan 1} :collect nil :action [] :vp nil}
      {:id "ar9"  :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar10" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar11" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar12" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar13" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar14" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar15" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar16" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar17" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar18" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar19" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar20" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar21" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar22" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar23" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar24" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar25" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar26" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar27" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar28" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar29" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar30" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar31" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar32" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar33" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar34" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar35" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar36" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar37" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar38" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar39" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
      {:id "ar40" :name "" :type "" :cost [] :collect [] :action [] :vp nil}
    ]}}
    