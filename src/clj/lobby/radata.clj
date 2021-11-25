(ns lobby.radata)

(def data (atom {
  :mages [
    {:id 0 :name "Necromancer" :type "mage" :collect [{:death 1}]}
    {:id 1 :name "Duelist"     :type "mage" :collect [{:elan 1}]}
    {:id 2 :name "Transmuter"  :type "mage" :collect nil}
    {:id 3 :name "Druid"       :type "mage" :collect [{:life 1}]}
    {:id 4 :name "Artificer"   :type "mage" :collect nil}
    {:id 5 :name "Healer"      :type "mage" :collect [{:calm 1} {:life 1}]}
    {:id 6 :name "Alchemist"   :type "mage" :collect nil}
    {:id 7 :name "Witch"       :type "mage" :collect [{:life 1} {:death 1}]}
    {:id 8 :name "Scholar"     :type "mage" :collect nil}
    {:id 9 :name "Seer"        :type "mage" :collect [{:calm 1}]}
  ]
  :monuments [
    {:id 0 :name "Colossus"         :type "monument" :cost {:gold 4} :action [] :vp 2}
    {:id 1 :name "Golden Statue"    :type "monument" :cost {:gold 4} :action [] :vp 1}
    {:id 2 :name "Great Pyramid"    :type "monument" :cost {:gold 4} :vp 3}
    {:id 3 :name "Hanging Gardens"  :type "monument" :cost {:gold 4} :collect {} :vp 1}
    {:id 4 :name "Library"          :type "monument" :cost {:gold 4} :action [] :vp 1}
    {:id 5 :name "Mausoleum"        :type "monument" :cost {:gold 4} :action [] :vp 2}
    {:id 6 :name "Obelisk"          :type "monument" :cost {:gold 4} :bought [] :vp 1}
    {:id 7 :name "Oracle"           :type "monument" :cost {:gold 4} :action [] :vp 1}
    {:id 8 :name "Solomon's Mine"   :type "monument" :cost {:gold 4} :action [] :vp 1}
    {:id 9 :name "Temple"           :type "monument" :cost {:gold 4} :action [] :vp 1}
  ]
  :magicitems [
    {:id 0 :name "Alchemy"        :type "magicitem" :action []}
    {:id 1 :name "Calm Elan"      :type "magicitem" :collect [{:calm 1}{:elan 1}]}
    {:id 2 :name "Death Life"     :type "magicitem" :collect [{:death 1}{:life 1}]}
    {:id 3 :name "Divination"     :type "magicitem" :action []}
    {:id 4 :name "Protection"     :type "magicitem" :action []}
    {:id 5 :name "Reanimate"      :type "magicitem" :action []}
    {:id 6 :name "Research"       :type "magicitem" :action []}
    {:id 7 :name "Transmutation"  :type "magicitem" :action []}
  ]
  :placesofpower [
    {:id 6 :base 6 :name "Sacred Grove"          :type "pop" :cost {:life 8 :calm 4} :action [] :vp nil}
    {:id 0 :base 6 :name "Alchemists Tower"      :type "pop" :cost {:gold 3}  :action [] :vp nil}
                                                        
    {:id 1 :base 1 :name "Catacombs of the Dead" :type "pop" :cost {:death 9} :action [] :vp nil}
    {:id 7 :base 1 :name "Sacrificial Pit"       :type "pop" :cost {:elan 8 :death 4} :action [] :vp nil}
                                                        
    {:id 3 :base 3 :name "Cursed Forge"          :type "pop" :cost {:elan 5 :life 5 :calm 5} :action [] :vp nil}
    {:id 5 :base 3 :name "Dwarven Mines"         :type "pop" :cost {:elan 4 :life 2 :gold 1} :action [] :vp nil}
                                                        
    {:id 2 :base 2 :name "Coral Castle"          :type "pop" :cost {:elan 3 :life 3 :calm 3 :death 3} :action [] :vp nil}
    {:id 9 :base 2 :name "Sunken Reef"           :type "pop" :cost {:calm 5 :elan 2 :life 2} :action [] :vp nil}
                                                        
    {:id 4 :base 4 :name "Dragon's Lair"         :type "pop" :cost {:elan 6 :death 3} :action [] :vp nil}
    {:id 8 :base 4 :name "Sorcerer's Bestiary"   :type "pop" :cost {:life 4 :elan 2 :calm 2 :death 2} :action [] :vp nil}
  ]
  
  :artifacts [
    {:id 0  :type "artifact" :name "Athanor"              :cost {:gold 1 :elan 1} :action []}
    {:id 1  :type "artifact" :name "Bone Dragon"          :cost {:death 4 :life 1} :subtype "Dragon" :action [] :vp 1}
    {:id 2  :type "artifact" :name "Celestial Horse"      :cost {:calm 2 :elan 1} :subtype "Creature" :collect []}
    {:id 3  :type "artifact" :name "Chalice of Fire"      :cost {:gold 1 :elan 1} :collect {:elan 2}}
    {:id 4  :type "artifact" :name "Chalice of Life"      :cost {:gold 1 :elan 1} :collect {:calm 1 :life 1}}
    {:id 5  :type "artifact" :name "Corrupt Altar"        :cost {:any 3 :death 2} :collect {:life 1 :death 1}}
    {:id 6  :type "artifact" :name "Crypt"                :cost {:any 3 :death 2} :action []}
    {:id 7  :type "artifact" :name "Cursed Skull"         :cost {:death 2} :action []}
    {:id 8  :type "artifact" :name "Dancing Sword"        :cost {:gold 1 :elan 1} :collect {:death 1 :elan 1} :action []}
    {:id 9  :type "artifact" :name "Dragon Bridle"        :cost {:elan 1 :life 1 :calm 1 :death 1} :action [] :react []}
                
    {:id 10 :type "artifact" :name "Dragon Egg"           :cost {:gold 1} :vp 1}
    {:id 11 :type "artifact" :name "Dragon Teeth"         :cost {:elan 1 :death 1}}
    {:id 12 :type "artifact" :name "Dwarven Pickaxe"      :cost {:elan 1}}
    {:id 13 :type "artifact" :name "Earth Dragon"         :cost {:elan 4 :life 3} :subtype "Dragon" :vp 1}
    {:id 14 :type "artifact" :name "Elemental Spring"     :cost {:elan 2 :life 1 :calm 1}}
    {:id 15 :type "artifact" :name "Elvish Bow"           :cost {:elan 2 :life 1}}
    {:id 16 :type "artifact" :name "Fiery Whip"           :cost {:elan 2 :death 2}}
    {:id 17 :type "artifact" :name "Fire Dragon"          :cost {:elan 6} :subtype "Dragon" :vp 1}
    {:id 18 :type "artifact" :name "Flaming Pit"          :cost {:elan 2}}
    {:id 19 :type "artifact" :name "Fountain of Youth"    :cost {:calm 1 :death 1}}
      
    {:id 20 :type "artifact" :name "Guard Dog"            :cost {:elan 1}:subtype "Creature" }
    {:id 21 :type "artifact" :name "Hand of Glory"        :cost {:life 1 :death 1}}
    {:id 22 :type "artifact" :name "Hawk"                 :cost {:life 1 :calm 1}:subtype "Creature" }
    {:id 23 :type "artifact" :name "Horn of Plenty"       :cost {:gold 2}}
    {:id 24 :type "artifact" :name "Hypnotic Basin"       :cost {} }  
    {:id 25 :type "artifact" :name "Magical Shard"        :cost {} } 
    {:id 26 :type "artifact" :name "Jeweled Statuette"    :cost {} } 
    {:id 27 :type "artifact" :name "Mermaid"              :cost {} :subtype "Creature"}
    {:id 28 :type "artifact" :name "Nightingale"          :cost {} :subtype "Creature"}
    {:id 29 :type "artifact" :name "Philosopher's Stone"  :cost {} :vp 1}
    
    {:id 30 :type "artifact" :name "Prism"                :cost {} }
    {:id 31 :type "artifact" :name "Ring of Midas"        :cost {} }
    {:id 32 :type "artifact" :name "Sacrificial Dagger"   :cost {} }
    {:id 33 :type "artifact" :name "Sea Serpent"          :cost {} }
    {:id 34 :type "artifact" :name "Treant"               :cost {} :collect [{:life 2}]}
    {:id 35 :type "artifact" :name "Tree of Life"         :cost {} }
    {:id 36 :type "artifact" :name "Vault"                :cost {} }
    {:id 37 :type "artifact" :name "Water Dragon"         :cost {} }
    {:id 38 :type "artifact" :name "Wind Dragon"          :cost {} }
    {:id 39 :type "artifact" :name "Windup Man"           :cost {} }
  ]
}))
