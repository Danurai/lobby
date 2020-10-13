(ns lobby.radata)

(def data (atom {
  :mages [
    {:id 0 :name "Necromancer" :type "Mage"}
    {:id 1 :name "Duelist"     :type "Mage"}
    {:id 2 :name "Transmuter"  :type "Mage"}
    {:id 3 :name "Druid"       :type "Mage"}
    {:id 4 :name "Artificer"   :type "Mage"}
    {:id 5 :name "Healer"      :type "Mage"}
    {:id 6 :name "Alchemist"   :type "Mage"}
    {:id 7 :name "Witch"       :type "Mage"}
    {:id 8 :name "Scholar"     :type "Mage"}
    {:id 9 :name "Seer"        :type "Mage"}
  ]
  :monuments [
    {:id 0 :name "Colossus"         :type "Monument" :cost {:gold 4} :action [] :vp 2}
    {:id 1 :name "Golden Statue"    :type "Monument" :cost {:gold 4} :action [] :vp 1}
    {:id 2 :name "Great Pyramid"    :type "Monument" :cost {:gold 4} :vp 3}
    {:id 3 :name "Hanging Gardens"  :type "Monument" :cost {:gold 4} :collect {} :vp 1}
    {:id 4 :name "Library"          :type "Monument" :cost {:gold 4} :action [] :vp 1}
    {:id 5 :name "Mausoleum"        :type "Monument" :cost {:gold 4} :action [] :vp 2}
    {:id 6 :name "Obelisk"          :type "Monument" :cost {:gold 4} :bought [] :vp 1}
    {:id 7 :name "Oracle"           :type "Monument" :cost {:gold 4} :action [] :vp 1}
    {:id 8 :name "Solomon's Mine"   :type "Monument" :cost {:gold 4} :action [] :vp 1}
    {:id 9 :name "Temple"           :type "Monument" :cost {:gold 4} :action [] :vp 1}
  ]
  :magicitems [
    {:id 0 :name "Alchemy"        :action []}
    {:id 1 :name "Reanimate"      :action []}
    {:id 2 :name "Calm | Elan"    :collect "collect or :calm :elan"}
    {:id 3 :name "Death | Life"   :collect "collect or :death :life"}
    {:id 4 :name "Protection"     :action []}
    {:id 5 :name "Divination"     :action []}
    {:id 6 :name "Research"       :action []}
    {:id 7 :name "Transmutation"  :action []}
  ]
  :placesofpower [
    {:id 0 :root 3 :name "Alchemists Tower"       :type "Place of Power" :cost {:gold 3}  :action [] :vp nil}
    {:id 0 :root 1 :name "Catacombs of the Dead"  :type "Place of Power" :cost {:death 9} :action [] :vp nil}
    {:id 0 :root 2 :name "Cursed Forge"           :type "Place of Power" :cost {:elan 5 :life 5 :calm 5} :action [] :vp nil}
    {:id 0 :root 3 :name "Dragon's Lair"          :type "Place of Power" :cost {:elan 6 :death 3} :action [] :vp nil}
    {:id 0 :root 2 :name "Coral Castle"           :type "Place of Power" :cost {:elan 3 :life 3 :calm 3 :death 3} :action [] :vp nil}
    {:id 0 :root 4 :name "Swarven Mines"          :type "Place of Power" :cost {:elan 4 :life 2 :gold 1} :action [] :vp nil}
    {:id 0 :root 1 :name "Sacred Grove"           :type "Place of Power" :cost {:life 8 :calm 4} :action [] :vp nil}
    {:id 0 :root 4 :name "Sacrificial Pit"        :type "Place of Power" :cost {:elan 8 :death 4} :action [] :vp nil}
    {:id 0 :root 5 :name "Sorcerer's Bestiary"    :type "Place of Power" :cost {:life 4 :elan 2 :calm 2 :death 2} :action [] :vp nil}
    {:id 0 :root 5 :name "Sunken Reef"            :type "Place of Power" :cost {:calm 5 :elan 2 :life 2} :action [] :vp nil}
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
    
  ]
}))