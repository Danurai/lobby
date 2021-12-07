(ns lobby.radata)

(def data (atom {
  :mages [
    {:id 0 :name "Necromancer" :type "mage"       :collect [{:death 1}]     :action [{:turn true :cost {:life 2} :place {:death 3}}]}
    {:id 1 :name "Duelist"     :type "mage" :fg 1 :collect [{:elan 1}]      :action [{:turn true :cost {:death 1} :place {:gold 1}}] }
    {:id 2 :name "Transmuter"  :type "mage" :fg 4                           :action [{:turn true :cost {:any 2} :gain {:any 3} :gainexclude #{:gold}}]}
    {:id 3 :name "Druid"       :type "mage"       :collect [{:life 1}]      :action [{:turn true :straighten #{"creature"}}]}
    {:id 4 :name "Artificer"   :type "mage"                                 :action [{:reducer true :reduction {:any 1} :reducerestrict #{"artifact"} :reduceexclude #{:gold}}]}
    {:id 5 :name "Healer"      :type "mage"       :collect [{:calm 1} {:life 1}] :action [{:react true :turn true :ignore :loselife}]}
    {:id 6 :name "Alchemist"   :type "mage" :fg 3                           :action [{:turn true :cost {:any 4} :gain {:gold 2}} {:turn true :gain {:any 1} :gainexclude #{:gold}}]}
    {:id 7 :name "Witch"       :type "mage"       :collect [{:life 1} {:death 1}] :action [{:turn true :cost {:any 2} :straighten :any}]}
    {:id 8 :name "Scholar"     :type "mage"                                 :action [{:turn true :cost {:any 1} :draw 1}]}
    {:id 9 :name "Seer"        :type "mage" :fg 2 :collect [{:calm 1}]      :action [{:turn true :special :seer}]}
  ]
  :magicitems [
    {:id 0 :name "Alchemy"        :type "magicitem" :action [{:turn true :cost {:any 4} :gain {:gold 2}}]}
    {:id 1 :name "Calm Elan"      :type "magicitem" :collect [{:calm 1}{:elan 1}]}
    {:id 2 :name "Death Life"     :type "magicitem" :collect [{:death 1}{:life 1}]}
    {:id 3 :name "Divination"     :type "magicitem" :action [{:turn true :special :divination}]}
    {:id 4 :name "Protection"     :type "magicitem" :action [{:react true :turn true :ignore :loselife}]}
    {:id 5 :name "Reanimate"      :type "magicitem" :action [{:turn true :cost {:any 1} :straighten :any}]}
    {:id 6 :name "Research"       :type "magicitem" :action [{:turn true :cost {:any 1} :draw 1}]}
    {:id 7 :name "Transmutation"  :type "magicitem" :action [{:turn true :cost {:any 3} :gain {:any 3} :gainexclude #{:gold}}]}
  ]
  :monuments [
    {:id 0 :name "Colossus"         :type "monument" :cost {:gold 4} :vp 2 :action [{:turn true :cost {:any 1} :place {:gold 1}}]}
    {:id 1 :name "Golden Statue"    :type "monument" :cost {:gold 4} :vp 1 :action [{:react :victory :turn true :cost {:gold 3} :vp 3}]}
    {:id 2 :name "Great Pyramid"    :type "monument" :cost {:gold 4} :vp 3}
    {:id 3 :name "Hanging Gardens"  :type "monument" :cost {:gold 4} :vp 1 :collect [{:any_ng 3}]}
    {:id 4 :name "Library"          :type "monument" :cost {:gold 4} :vp 1 :action [{:turn true :draw 1}]}
    {:id 5 :name "Mausoleum"        :type "monument" :cost {:gold 4} :vp 2 :action [{:cost {:any 1} :place {:death 1}}]}
    {:id 6 :name "Obelisk"          :type "monument" :cost {:gold 4} :vp 1 :bought {:collect {:any_ng 6}}}
    {:id 7 :name "Oracle"           :type "monument" :cost {:gold 4} :vp 2 :action [{:turn true :special :seer}]}
    {:id 8 :name "Solomon's Mine"   :type "monument" :cost {:gold 4} :vp 1 :action [{:turn true :place {:gold 1}}]}
    {:id 9 :name "Temple"           :type "monument" :cost {:gold 4} :vp 2 :collect [{:life 1}] :action [{:react :loselife :turn true}]}
  ]
  :placesofpower [
    {:id 6 :base 6 :name "Sacred Grove"          :type "pop" :fg? true :cost {:life 8 :calm 4} :action [{:turn true :cost {:calm 1} :gain {:life 5}} {:turn #{"Sacred Grove" "Creature"} :place {:life 1}}] :vp 2}
    {:id 0 :base 6 :name "Alchemists Tower"      :type "pop" :cost {:gold 3} :collect [{:any 3}] :action [] :vp 0}
                                                        
    {:id 1 :base 1 :name "Catacombs of the Dead" :type "pop" :fg? true :cost {:death 9} :collect [{:death 1}] :action [{:cost {:death 5} :place {:death 1}} {:turn true :place {:death 1}}] :vp 0}
    {:id 7 :base 1 :name "Sacrificial Pit"       :type "pop" :cost {:elan 8 :death 4} :action [] :vp 0}
                                                        
    {:id 3 :base 3 :name "Cursed Forge"          :type "pop" :fg? true :cost {:elan 5 :life 5 :calm 5} :collect [{:death -1} {:turn true}] :action [{:cost {:elan 2 :gold 1} :place {:gold 1}}] :vp 1}
    {:id 5 :base 3 :name "Dwarven Mines"         :type "pop" :cost {:elan 4 :life 2 :gold 1} :collect [{:gold 1}] :action [] :vp 0}
                                                        
    {:id 2 :base 2 :name "Coral Castle"          :type "pop" :fg? true :cost {:elan 3 :life 3 :calm 3 :death 3} :action [{:turn true :checkvictory true}] :vp 3}
    {:id 9 :base 2 :name "Sunken Reef"           :type "pop" :cost {:calm 5 :elan 2 :life 2} :collect [{:gold 1}] :action [] :vp 0}
                                                        
    {:id 4 :base 4 :name "Dragon's Lair"         :type "pop" :fg? true :cost {:elan 6 :death 3} :action [{} {:turn true :gain {:gold 2}} {:turn true :turnextra "Dragon" :place {:gold 2}}] :vp 0}
    {:id 8 :base 4 :name "Sorcerer's Bestiary"   :type "pop" :cost {:life 4 :elan 2 :calm 2 :death 2} :action [] :vp 0}
  ]
  
  :artifacts [
    {:id 0  :type "artifact" :name "Athanor"               :cost {:gold 1 :elan 1} :action [{:turn true :cost {:elan 1} :place {:elan 2}} {:turn true :remove {:elan 6}} ]}
    {:id 1  :type "artifact" :name "Bone Dragon"           :cost {:death 4 :life 1} :subtype "Dragon" :action [] :vp 1}
    {:id 2  :type "artifact" :name "Celestial Horse" :fg 3 :cost {:calm 2 :elan 1} :subtype "Creature" :collect [{:any 2}] :collectexclude #{:gold :death}}
    {:id 3  :type "artifact" :name "Chalice of Fire"      :cost {:gold 1 :elan 1} :collect [{:elan 2}] :action [{:turn true :cost {:elan 1} :refresh :any}]}
    {:id 4  :type "artifact" :name "Chalice of Life" :fg 2 :cost {:gold 1 :life 1 :calm 1} :collect [{:calm 1 :life 1}] :action [{:cost {:calm 2} :place {:calm 2 :life 1}} {:react true :turn true :ignore :loselife}]}
    {:id 5  :type "artifact" :name "Corrupt Altar"        :cost {:any 3 :death 2} :collect [{:life 1 :death 1}] :action [{:cost {:life 2} :place {:elan 3}} {:turn true :special 5}]}
    {:id 6  :type "artifact" :name "Crypt"                :cost {:any 3 :death 2} :action [{:turn true :gain {:death 2}} {:turn true :cost {:death 1} :special 6}]}
    {:id 7  :type "artifact" :name "Cursed Skull"         :cost {:death 2} :action [{:turn true :cost {:life 1} :place {:any 3} :placeexclude #{:gold :life}}]}
    {:id 8  :type "artifact" :name "Dancing Sword"  :fg 4 :cost {:gold 1 :elan 1} :collect [{:death 1 :elan 1}] :action [{:react true :cost {:elan 1} :place {:death 1} :ignore :loselife}]}
    {:id 9  :type "artifact" :name "Dragon Bridle"        :cost {:elan 1 :life 1 :calm 1 :death 1} :action [{:special 9} {:react true :turn true :ignore :dragonpower}] }
                
    {:id 10 :type "artifact" :name "Dragon Egg"           :cost {:gold 1} :vp 1 }
    {:id 11 :type "artifact" :name "Dragon Teeth"   :fg 1 :cost {:elan 1 :death 1} :action [{:turn false :cost {:elan 2} :place {:elan 3}} {:turn true :cost {:elan 3} :place {:subtype "Dragon" :cost {}}}]}
    {:id 12 :type "artifact" :name "Dwarven Pickaxe"      :cost {:elan 1} :action [{:turn true :cost {:elan 1} :gain {:gold 1}}]}
    {:id 13 :type "artifact" :name "Earth Dragon"         :cost {:elan 4 :life 3} :subtype "Dragon" :vp 1 :action [{:turn true :rivals {:loselife 2} :ignorecost {:gold 1}}]}
    {:id 14 :type "artifact" :name "Elemental Spring" :fg 1 :cost {:elan 2 :life 1 :calm 1} :collect [{:calm 1 :life 1 :elan 1}] :action [{:react true :cost {:calm 1} :ignore :loselife}]}
    {:id 15 :type "artifact" :name "Elvish Bow"           :cost {:elan 2 :life 1} :action [{:turn true :rivals {:loselife 1}} {:turn true :drawcard 1}]}
    {:id 16 :type "artifact" :name "Fiery Whip"           :cost {:elan 2 :death 2} :action [{:turn true :gain {:elan 3} :rivals {:elan 1} }]}
    {:id 17 :type "artifact" :name "Fire Dragon"          :cost {:elan 6} :subtype "Dragon" :vp 1}
    {:id 18 :type "artifact" :name "Flaming Pit"    :fg 2 :cost {:elan 2} :collect [{:elan 1}] :action [{:turn true :cost {:life 1} :gain {:elan 1 :death 1}}]}
    {:id 19 :type "artifact" :name "Fountain of Youth" :fg 4 :cost {:calm 1 :death 1} :collect [{:life 1}]}
      
    {:id 20 :type "artifact" :name "Guard Dog"            :cost {:elan 1}:subtype "Creature" }
    {:id 21 :type "artifact" :name "Hand of Glory"  :fg 1 :cost {:life 1 :death 1} :action [{:turn true :cost {} :gain {:death 2} :rivals {:death 1}}]}
    {:id 22 :type "artifact" :name "Hawk"           :fg 4 :cost {:life 1 :calm 1}:subtype "Creature" :collect [{:calm 1}]}
    {:id 23 :type "artifact" :name "Horn of Plenty"       :cost {:gold 2}}
    {:id 24 :type "artifact" :name "Hypnotic Basin"       :cost {:calm 2 :elan 1 :death 1} :collect [{:calm 2}]}  
    {:id 25 :type "artifact" :name "Jeweled Statuette"    :cost {:death 2 :gold 1} } 
    {:id 26 :type "artifact" :name "Magical Shard"  :fg 3 :cost {} } 
    {:id 27 :type "artifact" :name "Mermaid"              :cost {:life 2 :calm 2} :collect [{:calm 1}] :subtype "Creature"}
    {:id 28 :type "artifact" :name "Nightingale"          :cost {:life 1 :calm 1} :subtype "Creature"}
    {:id 29 :type "artifact" :name "Philosopher's Stone"  :cost {:elan 2 :life 2 :calm 2 :death 2} :vp 1}
    
    {:id 30 :type "artifact" :name "Prism"                :cost {} }
    {:id 31 :type "artifact" :name "Ring of Midas"        :cost {:life 1 :gold 1} }
    {:id 32 :type "artifact" :name "Sacrificial Dagger"   :cost {:death 1 :gold 1} }
    {:id 33 :type "artifact" :name "Sea Serpent"          :cost {:calm 6 :life 3} :subtype "Dragon Creature"}
    {:id 34 :type "artifact" :name "Treant"               :cost {:life 3 :elan 2} :subtype "Creature":collect [{:life 2}]}
    {:id 35 :type "artifact" :name "Tree of Life"   :fg 3 :cost {:any 2 :life 1} }
    {:id 36 :type "artifact" :name "Vault"          :fg 2 :cost {:gold 1 :any 1} :collect [{:special 36}]}
    {:id 37 :type "artifact" :name "Water Dragon"         :cost {:calm 6} :subtype "Dragon" }
    {:id 38 :type "artifact" :name "Wind Dragon"          :cost {:calm 4 :any 4} :subtype "Dragon" }
    {:id 39 :type "artifact" :name "Windup Man"           :cost {:elan 1 :life 1 :calm 1 :gold 1} :collect [{:special 39}]}
  ]
}))
