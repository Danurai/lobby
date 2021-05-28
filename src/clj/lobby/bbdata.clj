(ns lobby.bbdata)

(def players [
    {:id  0 :alliance "OWA" :team "Human" :race "Human" :position "Lineman" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id  1 :alliance "OWA" :team "Human" :race "Human" :position "Lineman" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id  2 :alliance "OWA" :team "Human" :race "Human" :position "Lineman" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id  3 :alliance "OWA" :team "Human" :race "Human" :position "Lineman" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id  4 :alliance "OWA" :team "Human" :race "Human" :position "Thrower" :active [ :dumpoff ] :skills [ :pass ] :spp [ 2 1 ]}
    {:id  5 :alliance "OWA" :team "Human" :race "Human" :position "Thrower" :active [ :dumpoff ] :skills [ :pass ] :spp [ 2 1 ]}
    {:id  6 :alliance "OWA" :team "Human" :race "Human" :position "Catcher" :active [ :surehands :catcher ] :skills [ :sprint ] :spp [ 3 1 ]}
    {:id  7 :alliance "OWA" :team "Human" :race "Human" :position "Catcher" :active [ :surehands :catcher ] :skills [ :sprint ] :spp [ 3 1 ]}
    {:id  8 :alliance "OWA" :team "Human" :race "Human" :position "Blitzer" :active [ :blitzer ] :skills [ :tackle ] :spp [ 3 1 ]}
    {:id  9 :alliance "OWA" :team "Human" :race "Human" :position "Blitzer" :active [ :blitzer ] :skills [ :tackle ] :spp [ 3 1 ]}
    {:id 10 :alliance "OWA" :team "Human" :race "Human" :position "Blitzer" :active [ :blitzer ] :skills [ :tackle ] :spp [ 3 1 ]}
    {:id 11 :alliance "OWA" :team "Human" :race ""  :position "Ogre" :active [ :juggernaut ] :skills [ :tackle :cheat ] :spp [ 4 2 ]}
    ; elf 12-23
    {:id 12 :alliance "OWA" :team "WoodElf" :race "Wood Elf" :position "Lineman" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id 13 :alliance "OWA" :team "WoodElf" :race "Wood Elf" :position "Lineman" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id 14 :alliance "OWA" :team "WoodElf" :race "Wood Elf" :position "Lineman" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id 15 :alliance "OWA" :team "WoodElf" :race "Wood Elf" :position "Lineman" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id 16 :alliance "OWA" :team "WoodElf" :race "Wood Elf" :position "Thrower" :active [ ] :skills [ :pass :pass ] :spp [ 2 1 ]}
    {:id 17 :alliance "OWA" :team "WoodElf" :race "Wood Elf" :position "Thrower" :active [ ] :skills [ :pass :pass ] :spp [ 2 1 ]}
    {:id 18 :alliance "OWA" :team "WoodElf" :race "Wood Elf" :position "Catcher" :active [ ] :skills [ :pass :sprint :sprint ] :spp [ 2 1 ]}
    {:id 19 :alliance "OWA" :team "WoodElf" :race "Wood Elf" :position "Catcher" :active [ ] :skills [ :pass :sprint :sprint ] :spp [ 2 1 ]}
    {:id 20 :alliance "OWA" :team "WoodElf" :race "" :position "Wardancer" :active [ :wardancer ] :skills [ :tackle ] :spp [ 3 1 ]}
    {:id 21 :alliance "OWA" :team "WoodElf" :race "" :position "Wardancer" :active [ :wardancer ] :skills [ :tackle ] :spp [ 3 1 ]}
    {:id 22 :alliance "OWA" :team "WoodElf" :race "" :position "Wardancer" :active [ :wardancer ] :skills [ :tackle ] :spp [ 3 1 ]}
    {:id 23 :alliance "OWA" :team "WoodElf" :race "" :position "Treeman" :active [ :fend ] :skills [ :tackle ] :spp [ 4 2 ]}
    ; dwarf 24-35
    {:id 24 :alliance "OWA" :team "Dwarf" :race "Dwarf" :position "Longbeard" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id 25 :alliance "OWA" :team "Dwarf" :race "Dwarf" :position "Longbeard" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id 26 :alliance "OWA" :team "Dwarf" :race "Dwarf" :position "Longbeard" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id 27 :alliance "OWA" :team "Dwarf" :race "Dwarf" :position "Longbeard" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id 28 :alliance "OWA" :team "Dwarf" :race "Dwarf" :position "Runner" :active [ :standfirm ] :skills [ :pass ] :spp [ 2 1 ]}
    {:id 29 :alliance "OWA" :team "Dwarf" :race "Dwarf" :position "Runner" :active [ :standfirm ] :skills [ :pass ] :spp [ 2 1 ]}
    {:id 30 :alliance "OWA" :team "Dwarf" :race "Dwarf" :position "Blocker" :active [ :guard ] :skills [ :tackle ] :spp [ 3 2 ]}
    {:id 31 :alliance "OWA" :team "Dwarf" :race "Dwarf" :position "Blocker" :active [ :guard ] :skills [ :tackle ] :spp [ 3 2 ]}
    {:id 32 :alliance "OWA" :team "Dwarf" :race "Dwarf" :position "Blitzer" :active [ :stripball ] :skills [ :tackle ] :spp [ 3 2 ]}
    {:id 33 :alliance "OWA" :team "Dwarf" :race "Dwarf" :position "Blitzer" :active [ :stripball ] :skills [ :tackle ] :spp [ 3 2 ]}
    {:id 34 :alliance "OWA" :team "Dwarf" :race "" :position "Troll Slayer" :active [ :dauntless ] :skills [ :cheat :tackle ] :spp [ 3 2 ]}
    {:id 35 :alliance "OWA" :team "Dwarf" :race "" :position "Troll Slayer" :active [ :dauntless ] :skills [ :cheat :tackle ] :spp [ 3 2 ]}
    ; freebooter 36-61
    {:id 36 :alliance "OWA" :team "Freebooter" :position "Aurora Silverleaf" :active [] :skills [ :tackle :cheat :sprint ] :spp [ 4 1 ]}
    {:id 37 :alliance "OWA" :team "Freebooter" :position "Barik Farblast" :active [ :surehands :special ] :skills [ :pass ] :spp [ 3 2 ]}
    {:id 38 :alliance "OWA" :team "Freebooter" :position "Cornelius Krieg" :active [ :guard :special ] :skills [ :tackle :cheat ] :spp [ 3 1 ]}
    {:id 39 :alliance "OWA" :team "Freebooter" :position "Deeproot Strongbranch" :active [ :guard ] :skills [ :tackle ] :spp [ 5 3 ]}
    {:id 40 :alliance "OWA" :team "Freebooter" :position "Eldril Sidewinder" :active [ :nervesofsteel :surehands ] :skills [ :pass :pass ] :spp [ 3 1 ]}
    {:id 41 :alliance "OWA" :team "Freebooter" :position "Freebooter Blitzer" :active [ :frenzy :freebooter ] :skills [ :tackle :cheat ] :spp [ 3 1 ]}
    {:id 42 :alliance "OWA" :team "Freebooter" :position "Freebooter Blitzer" :active [ :frenzy :freebooter ] :skills [ :tackle :cheat ] :spp [ 3 1 ]}
    {:id 43 :alliance "OWA" :team "Freebooter" :position "Freebooter Lineman" :active [ :guard :freebooter ] :skills [ ] :spp [ 3 2 ]}
    {:id 44 :alliance "OWA" :team "Freebooter" :position "Freebooter Lineman" :active [ :guard :freebooter ] :skills [ ] :spp [ 3 2 ]}
    {:id 45 :alliance "OWA" :team "Freebooter" :position "Freebooter Runner" :active [ :dodge :freebooter ] :skills [ :pass :sprint ] :spp [ 3 1 ]}
    {:id 46 :alliance "OWA" :team "Freebooter" :position "Freebooter Runner" :active [ :dodge :freebooter ] :skills [ :pass :sprint ] :spp [ 3 1 ]}
    {:id 47 :alliance "OWA" :team "Freebooter" :position "Freebooter Thrower" :active [ :surehands :freebooter ] :skills [ :pass :pass ] :spp [ 2 1 ]}
    {:id 48 :alliance "OWA" :team "Freebooter" :position "Freebooter Thrower" :active [ :surehands :freebooter ] :skills [ :pass :pass ] :spp [ 2 1 ]}
    {:id 49 :alliance "OWA" :team "Freebooter" :position "Griff Oberwald" :active [ :nervesofsteel ] :skills [ :pass :pass :sprint ] :spp [ 4 1 ]}
    {:id 50 :alliance "OWA" :team "Freebooter" :position "Grim Ironjaw" :active [ :stripball ] :skills [ :tackle :cheat ] :spp [ 4 2 ]}
    {:id 51 :alliance "OWA" :team "Freebooter" :position "Jordell Freshbreeze" :active [ :ballcarrier2 ] :skills [ :pass :pass :sprint :sprint ] :spp [ 3 1 ]}
    {:id 52 :alliance "OWA" :team "Freebooter" :position "Long Bomb Silver" :active [ :dumpoff ] :skills [ :cheat :pass ] :spp [ 3 1 ]}
    {:id 53 :alliance "OWA" :team "Freebooter" :position "Marcus Siebermann" :active [ :scoreboardcheat ] :skills [ :pass :pass :sprint ] :spp [ 3 1 ]}
    {:id 54 :alliance "OWA" :team "Freebooter" :position "Mighty Zug" :active [ :juggernaut ] :skills [ :tackle ] :spp [ 5 1 ]}
    {:id 55 :alliance "OWA" :team "Freebooter" :position "Morg 'N Thorg" :active [  ] :skills [ :cheat :tackle :pass ] :spp [ 5 2 ]}
    {:id 56 :alliance "OWA" :team "Freebooter" :position "Skuff Whitebeard" :active [ :standfirm ] :skills [ :tackle :pass ] :spp [ 3 2 ]}
    {:id 57 :alliance "OWA" :team "Freebooter" :position "Slab" :active [ :guard :fend ] :skills [ :tackle ] :spp [ 3 1 ]}
    {:id 58 :alliance "OWA" :team "Freebooter" :position "Spikey McSpike" :active [ :whenplayedcheat ] :skills [ :cheat :pass :sprint ] :spp [ 3 2 ]}
    {:id 59 :alliance "OWA" :team "Freebooter" :position "The Death Roller" :active [ :tacklecheat ] :skills [ :tackle :tackle :tackle :cheat ] :spp [ 5 0 ]}
    {:id 60 :alliance "OWA" :team "Freebooter" :position "Thornmane" :active [ :carrycheatfilter ] :skills [ :pass :tackle :cheat ] :spp [ 3 1 ]}

    {:id 100 :alliance "CWC" :team "Orc" :race "Orc" :position "Lineman" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id 101 :alliance "CWC" :team "Orc" :race "Orc" :position "Lineman" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id 102 :alliance "CWC" :team "Orc" :race "Orc" :position "Lineman" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id 103 :alliance "CWC" :team "Orc" :race "Orc" :position "Lineman" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id 104 :alliance "CWC" :team "Orc" :race "Orc" :position "Thrower" :active [ :surehands ] :skills [ :pass ] :spp [ 2 1 ]}
    {:id 105 :alliance "CWC" :team "Orc" :race "Orc" :position "Thrower" :active [ :surehands ] :skills [ :pass ] :spp [ 2 1 ]}
    {:id 106 :alliance "CWC" :team "Orc" :race "" :position "Black Orc Blocker" :active [ :frenzy ] :skills [ :tackle ] :spp [ 3 1 ]}
    {:id 107 :alliance "CWC" :team "Orc" :race "" :position "Black Orc Blocker" :active [ :frenzy ] :skills [ :tackle ] :spp [ 3 1 ]}
    {:id 108 :alliance "CWC" :team "Orc" :race "Orc" :position "Blitzer" :active [ ] :skills [ :tackle :cheat ] :spp [ 3 1 ]}
    {:id 109 :alliance "CWC" :team "Orc" :race "Orc" :position "Blitzer" :active [ ] :skills [ :tackle :cheat ] :spp [ 3 1 ]}
    {:id 110 :alliance "CWC" :team "Orc" :race "Orc" :position "Blitzer" :active [ ] :skills [ :tackle :cheat ] :spp [ 3 1 ]}
    {:id 111 :alliance "CWC" :team "Orc" :race "" :position "Da Troll" :active [ :pilingon ] :skills [ :tackle :cheat ] :spp [ 4 2 ]}
    
    {:id 112 :alliance "CWC" :team "Chaos" :race "Chaos" :position "Lineman" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id 113 :alliance "CWC" :team "Chaos" :race "Chaos" :position "Lineman" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id 114 :alliance "CWC" :team "Chaos" :race "Chaos" :position "Lineman" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id 115 :alliance "CWC" :team "Chaos" :race "Chaos" :position "Lineman" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id 116 :alliance "CWC" :team "Chaos" :race "" :position "Beastman" :active [ :tacklecheat ] :skills [ :cheat :tackle ] :spp [ 3 1 ]}
    {:id 117 :alliance "CWC" :team "Chaos" :race "" :position "Beastman" :active [ :tacklecheat ] :skills [ :cheat :tackle ] :spp [ 3 1 ]}
    {:id 119 :alliance "CWC" :team "Chaos" :race "" :position "Beastman" :active [ :tacklecheat ] :skills [ :cheat :tackle ] :spp [ 3 1 ]}
    {:id 119 :alliance "CWC" :team "Chaos" :race "" :position "Beastman Thrower" :active [ :dodge ] :skills [ :pass ] :spp [ 2 1 ]}
    {:id 120 :alliance "CWC" :team "Chaos" :race "" :position "Beastman Thrower" :active [ :dodge ] :skills [ :pass ] :spp [ 2 1 ]}
    {:id 121 :alliance "CWC" :team "Chaos" :race "Chaos" :position "Warrior" :active [ :dirtyplayer ] :skills [ :tackle :cheat ] :spp [ 3 1 ]}
    {:id 122 :alliance "CWC" :team "Chaos" :race "Chaos" :position "Warrior" :active [ :dirtyplayer ] :skills [ :tackle :cheat ] :spp [ 3 1 ]}
    {:id 123 :alliance "CWC" :team "Chaos" :race "" :position "Minotaur" :active [ :tackleback ] :skills [ :tackle :cheat :sprint ] :spp [ 4 2 ]}
    
    {:id 124 :alliance "CWC" :team "Skaven" :race "Skaven" :position "Lineman" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id 125 :alliance "CWC" :team "Skaven" :race "Skaven" :position "Lineman" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id 126 :alliance "CWC" :team "Skaven" :race "Skaven" :position "Lineman" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id 127 :alliance "CWC" :team "Skaven" :race "Skaven" :position "Lineman" :active [ :guard ] :skills [] :spp [ 1 0 ]}
    {:id 128 :alliance "CWC" :team "Skaven" :race "Skaven" :position "Thrower" :active [ ] :skills [ :cheat :pass ] :spp [ 2 0 ]}
    {:id 129 :alliance "CWC" :team "Skaven" :race "Skaven" :position "Thrower" :active [ ] :skills [ :cheat :pass ] :spp [ 2 0 ]}
    {:id 130 :alliance "CWC" :team "Skaven" :race "Skaven" :position "Gutter Runner" :active [ :dodge ] :skills [ :pass :pass :sprint ] :spp [ 2 0 ]}
    {:id 131 :alliance "CWC" :team "Skaven" :race "Skaven" :position "Gutter Runner" :active [ :dodge ] :skills [ :pass :pass :sprint ] :spp [ 2 0 ]}
    {:id 132 :alliance "CWC" :team "Skaven" :race "Skaven" :position "Gutter Runner" :active [ :dodge ] :skills [ :pass :pass :sprint ] :spp [ 2 0 ]}
    {:id 133 :alliance "CWC" :team "Skaven" :race "Skaven" :position "Blitzer" :active [] :skills [ :cheat :tackle :sprint ] :spp [ 3 1 ]}
    {:id 134 :alliance "CWC" :team "Skaven" :race "Skaven" :position "Blitzer" :active [] :skills [ :cheat :tackle :sprint ] :spp [ 3 1 ]}
    {:id 135 :alliance "CWC" :team "Skaven" :race "" :position "Rat Ogre" :active [ :tacklecheatfilter ] :skills [ :cheat :tackle ] :spp [ 4 2 ]}

    {:id 136 :alliance "CWC" :team "Freebooter" :position "Bite-Bite" :active [ :cheatejectone ] :skills [ :cheat :cheat :pass ] :spp [ 3 0 ]}
    {:id 137 :alliance "CWC" :team "Freebooter" :position "Blightmaw" :active [ :dumpoff ] :skills [ :pass :pass :sprint ] :spp [ 3 1 ]}
    {:id 138 :alliance "CWC" :team "Freebooter" :position "Bloodhorn" :active [ :scoreboardpass ] :skills [ :cheat :pass :pass ] :spp [ 3 1 ]}
    {:id 139 :alliance "CWC" :team "Freebooter" :position "Bone Crusher" :active [ :stripball ] :skills [ :cheat :tackle ] :spp [ 5 1 ]}
    {:id 140 :alliance "CWC" :team "Freebooter" :position "Bork" :active [ :passcheat ] :skills [ :pass :pass :sprint ] :spp [ 3 1 ]}
    {:id 141 :alliance "CWC" :team "Freebooter" :position "Crushface" :active [] :skills [ :cheat :pass ] :spp [ 4 1 ]}
    {:id 142 :alliance "CWC" :team "Freebooter" :position "Freebooter Blitzer" :active [ :frenzy :freebooter ] :skills [ :tackle :cheat ] :spp [ 3 1 ]}
    {:id 143 :alliance "CWC" :team "Freebooter" :position "Freebooter Blitzer" :active [ :frenzy :freebooter ] :skills [ :tackle :cheat ] :spp [ 3 1 ]}
    {:id 144 :alliance "CWC" :team "Freebooter" :position "Freebooter Lineman" :active [ :guard :freebooter ] :skills [] :spp [ 3 2 ]}
    {:id 145 :alliance "CWC" :team "Freebooter" :position "Freebooter Lineman" :active [ :guard :freebooter ] :skills [] :spp [ 3 2 ]}
    {:id 146 :alliance "CWC" :team "Freebooter" :position "Freebooter Runner" :active [ :dodge :freebooter ] :skills [ :pass :sprint ] :spp [ 3 1 ]}
    {:id 147 :alliance "CWC" :team "Freebooter" :position "Freebooter Runner" :active [ :dodge :freebooter ] :skills [ :pass :sprint ] :spp [ 3 1 ]}
    {:id 148 :alliance "CWC" :team "Freebooter" :position "Freebooter Thrower" :active [ :surehands :freebooter ] :skills [ :pass :pass ] :spp [ 2 1 ]}
    {:id 149 :alliance "CWC" :team "Freebooter" :position "Freebooter Thrower" :active [ :surehands :freebooter ] :skills [ :pass :pass ] :spp [ 2 1 ]}
    {:id 150 :alliance "CWC" :team "Freebooter" :position "Grashnak Blackhoof" :active [] :skills [ :sprint :sprint :tackle ] :spp [ 5 1 ]}
    {:id 150 :alliance "CWC" :team "Freebooter" :position "Headsplitter" :active [] :skills [ :cheat :tackle :cheat ] :spp [ 4 1 ]}
    {:id 150 :alliance "CWC" :team "Freebooter" :position "Hide-Sneak" :active [] :skills [ :cheat :pass :sprint :tackle ] :spp [ 2 1 ]}
    {:id 150 :alliance "CWC" :team "Freebooter" :position "Lord Borak the Despoiler" :active [ :scoreboardremovecheat ] :skills [ :cheat :cheat :cheat ] :spp [ 4 1 ]}
    {:id 150 :alliance "CWC" :team "Freebooter" :position "Morkai the Everchanging" :active [ :guard ] :skills [ :tackle :pass ] :spp [ 1 4 ]}
    {:id 150 :alliance "CWC" :team "Freebooter" :position "Morg 'N Thorg" :active [] :skills [ :cheat :tackle :sprint ] :spp [ 5 2 ]}
    {:id 150 :alliance "CWC" :team "Freebooter" :position "Niknik Yellowtail" :active [ :surehands ] :skills [ :cheat :pass :pass ] :spp [ 3 1 ]}
    {:id 150 :alliance "CWC" :team "Freebooter" :position "One Ear" :active [ :tackledinjured ] :skills [ :cheat :pass ] :spp [ 2 1 ]}
    {:id 150 :alliance "CWC" :team "Freebooter" :position "Smashjaw" :active [ :injurefans ] :skills [ :tackle :tackle ] :spp [ 3 1 ]}
    {:id 150 :alliance "CWC" :team "Freebooter" :position "Snotlobba" :active [ :throwteammate ] :skills [ :pass ] :spp [ 4 1 ]}
    
])

(def highlights [
    {:id  0 :name "All-out Blitz"          :payout [ {:team 1} [ {:staf 2} {:fans 3} ] {:staf 2} ] }
    {:id  1 :name "Bad Snap"               :payout [ {:fans 2} [ {:fans 2 :team 1}   ] {:star 1} ] }
    {:id  2 :name "Double-crossing Routes" :payout [ {:fans 2} [ {:team 1} {:fans 2} ] {:star 3} ] }
    {:id  3 :name "Double Down"            :payout [ {:fans 2} [ {:fans 2} {:team 2} ] {:star 1} ] }
    {:id 10 :name "Razzle Dazzle"          :payout [ {:staf 1} [ {:fans 4}           ] {:staf 2} ] }
])