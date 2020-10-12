{
  :radata {
    :counts {
      :artifacts 40
      :mages 10
      :monuments 10
      :placesofpower 10
      :magicitems 8
    }
    :mages [
      {:id "ma1"  :imgid 0 :name "Necromancer" :type "Mage" :collect [:death] :action {}}
      {:id "ma2"  :imgid 1 :name "Duelist" :type "Mage" :collect [:elan] :action {:desc "[turn]+[discard :death 1]>[place :gold]" :fn #((turn)(discard :death)(place :gold))}
      {:id "ma3"  :imgid 2 :name "Transmuter" :type "Mage" :collect [] :action {:desc "[turn]+[discard :any 2]>[gain :any 3 notgold]" :fn #((turn)(discard :any 2)(gain :any 3 :notgold))}}
      {:id "ma4"  :imgid 3 :name "Druid" :type "Mage" :collect [:life] :action {:desc "[turn]>[unturncreature]" :fn #(turn unturn :creature)}}
      {:id "ma5"  :imgid 4 :name "Artificer" :type "Mage" :collect [] :action []}
      {:id "ma6"  :imgid 5 :name "Healer" :type "Mage" :collect [] :action []}
      {:id "ma7"  :imgid 6 :name "Alchemist" :collect [] :action []}
      {:id "ma8"  :imgid 7 :name "Witch" :type "Mage" :collect [] :action []}
      {:id "ma9"  :imgid 8 :name "Scholar" :type "Mage" :collect [] :action []}
      {:id "ma10" :imgid 9 :name "Seer" :type "Mage" :collect [:elan] :action []}
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
      {:id "pop1"  :root 1 :name "Catacombs of the Dead" :type "Place of Power" :cost {:death 9} :action [] :vp nil}
      {:id "pop2"  :root 1 :name "Sacred Grove" :type "Place of Power" :cost {} :action [] :vp nil}
      {:id "pop3"  :root 2 :name "Cursed Forge" :type "Place of Power" :cost {} :action [] :vp nil}
      {:id "pop4"  :root 2 :name "Coral Castle" :type "Place of Power" :cost {} :action [] :vp nil}
      {:id "pop5"  :root 3 :name "Dragon's Lair" :type "Place of Power" :cost {} :action [] :vp nil}
      {:id "pop6"  :root 3 :name "Dragon's Lair" :type "Place of Power" :cost {} :action [] :vp nil}
      {:id "pop7"  :root 4 :name "Dragon's Lair" :type "Place of Power" :cost {} :action [] :vp nil}
      {:id "pop8"  :root 4 :name "Dragon's Lair" :type "Place of Power" :cost {} :action [] :vp nil}
      {:id "pop9"  :root 5 :name "Dragon's Lair" :type "Place of Power" :cost {} :action [] :vp nil}
      {:id "pop10" :root 5 :name "Dragon's Lair" :type "Place of Power" :cost {} :action [] :vp nil}
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
      {:id 0  :name "Athanor" :cost {:gold 1 :elan 1} :action []}
      {:id 1  :name "Bone Dragon" :type "Dragon" :cost {:death 4 :life 1} :action [] :vp 1}
      {:id 2  :name "Celestial Horse" :type "Creature" :cost {:calm 2 :elan 1} :collect []}
      {:id 3  :name "Chalice of Fire" :cost {:gold 1 :elan 1} :collect {:elan 2}}
      {:id 4  :name "Chalice of Life" :cost {:gold 1 :elan 1} :collect {:calm 1 :life 1}}
      {:id 5  :name "Corrupt Altar" :cost {:any 3 :death 2} :collect {:life 1 :death 1}}
      {:id 6  :name "Crypt" :cost {:any 3 :death 2} :action []}
      {:id 7  :name "Cursed Skull" :cost {:death 2} :action []}
      {:id 8  :name "Dancing Sword" :cost {:gold 1 :elan 1} :collect {:death 1 :elan 1} :action []}
      {:id 9  :name "Dragon Bridle" :cost {:elan 1 :life 1 :calm 1 :death 1} :action [] :react []}
      
      {:id 10 :name "Dragon Egg" :cost {:gold 1} :vp 1}
      {:id 11 :name "Dragon Teeth" :cost {:elan 1 :death 1}}
      {:id 12 :name "Dwarven Pickaxe" :cost {:elan 1}}
      {:id 13 :name "Earth Dragon" :type "Dragon" :cost {:elan 4 :life 3} :vp 1}
      {:id 14 :name "Elemental Spring" :cost {:elan 2 :life 1 :calm 1}}
      {:id 15 :name "Elvish Bow" :cost {:elan 2 :life 1}}
      {:id 16 :name "Fiery Whip" :cost {:elan 2 :death 2}}
      {:id 17 :name "Fire Dragon" :type "Dragon" :cost {:elan 6} :vp 1}
      {:id 18 :name "Flaming Pit" :cost {:elan 2}}
      {:id 19 :name "Fountain of Youth" :cost {:calm 1 :death 1}}
      
      {:id 20 :name "Guard Dog" :type "Creature" :cost {:elan 1}}
      {:id 21 :name "Hand of Glory" :cost {:life 1 :death 1}}
      {:id 22 :name "Hawk" :type "Creature" :cost {:life 1 :calm 1}}
      {:id 23 :name "Horn of Plenty" :cost {:gold 2}}
      {:id 24 :name "Hypnotic Basin"}
      {:id 25 :name "Magical Shard"}
      {:id 26 :name "Jeweled Statuette"}
      {:id 27 :name "Mermaid" :type "Creature"}
      {:id 28 :name "Nightingale" :type "Creature"}
      {:id 29 :name "Philosopher's Stone" :vp 1}
      
      {:id 30 :name "Prism"}
      {:id 31 :name "Ring of Midas"}
      {:id 32 :name "Sacrificial Dagger"}
      {:id 33 :name "Sea Serpent"}
      {:id 34 :name "Treant"}
      {:id 35 :name "Tree of Life"}
      {:id 36 :name "Vault"}
      {:id 37 :name "Water Dragon"}
      {:id 38 :name "Wind Dragon"}
      {:id 39 :name "Windup Man"}
      
    ]}}
    