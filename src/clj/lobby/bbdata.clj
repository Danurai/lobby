(ns lobby.bbdata)

(def players [
    {:id  0 :alliance "OWA" :team "Human" :race "Human" :position "Lineman" :skills [ :guard ] :abilities [] :spp [ 1 0 ]}
    {:id  1 :alliance "OWA" :team "Human" :race "Human" :position "Lineman" :skills [ :guard ] :abilities [] :spp [ 1 0 ]}
    {:id  2 :alliance "OWA" :team "Human" :race "Human" :position "Lineman" :skills [ :guard ] :abilities [] :spp [ 1 0 ]}
    {:id  3 :alliance "OWA" :team "Human" :race "Human" :position "Lineman" :skills [ :guard ] :abilities [] :spp [ 1 0 ]}
    {:id  4 :alliance "OWA" :team "Human" :race "Human" :position "Thrower" :skills [ :dumpoff ] :abilities [ :pass ] :spp [ 2 1 ]}
    {:id  5 :alliance "OWA" :team "Human" :race "Human" :position "Thrower" :skills [ :dumpoff ] :abilities [ :pass ] :spp [ 2 1 ]}
    {:id  6 :alliance "OWA" :team "Human" :race "Human" :position "Catcher" :skills [ :surehands :catcher ] :abilities [] :spp [ 3 1 ]}
    {:id  7 :alliance "OWA" :team "Human" :race "Human" :position "Catcher" :skills [ :surehands :catcher ] :abilities [] :spp [ 3 1 ]}
    {:id  8 :alliance "OWA" :team "Human" :race "Human" :position "Blitzer" :skills [ :blitzer ] :abilities [ :tackle ] :spp [ 3 1 ]}
    {:id  9 :alliance "OWA" :team "Human" :race "Human" :position "Blitzer" :skills [ :blitzer ] :abilities [ :tackle ] :spp [ 3 1 ]}
    {:id 10 :alliance "OWA" :team "Human" :race "Human" :position "Blitzer" :skills [ :blitzer ] :abilities [ :tackle ] :spp [ 3 1 ]}
    {:id 11 :alliance "OWA" :team "Human" :race ""  :position "Ogre" :skills [ :juggernaut ] :abilities [ :tackle :cheat ] :spp [ 4 2 ]}
    ; elf 12-23
    {:id 12 :alliance "OWA" :team "WoodElf" :race "Wood Elf" :position "Lineman" :skills [ :guard ] :abilities [] :spp [ 1 0 ]}
    {:id 13 :alliance "OWA" :team "WoodElf" :race "Wood Elf" :position "Lineman" :skills [ :guard ] :abilities [] :spp [ 1 0 ]}
    {:id 14 :alliance "OWA" :team "WoodElf" :race "Wood Elf" :position "Lineman" :skills [ :guard ] :abilities [] :spp [ 1 0 ]}
    {:id 15 :alliance "OWA" :team "WoodElf" :race "Wood Elf" :position "Lineman" :skills [ :guard ] :abilities [] :spp [ 1 0 ]}
    {:id 16 :alliance "OWA" :team "WoodElf" :race "Wood Elf" :position "Thrower" :skills [ ] :abilities [ :pass :pass ] :spp [ 2 1 ]}
    {:id 17 :alliance "OWA" :team "WoodElf" :race "Wood Elf" :position "Thrower" :skills [ ] :abilities [ :pass :pass ] :spp [ 2 1 ]}
    {:id 18 :alliance "OWA" :team "WoodElf" :race "Wood Elf" :position "Catcher" :skills [ ] :abilities [ :pass :sprint :sprint ] :spp [ 2 1 ]}
    {:id 19 :alliance "OWA" :team "WoodElf" :race "Wood Elf" :position "Catcher" :skills [ ] :abilities [ :pass :sprint :sprint ] :spp [ 2 1 ]}
    {:id 20 :alliance "OWA" :team "WoodElf" :race "" :position "Wardancer" :skills [ :wardancer ] :abilities [ :tackle ] :spp [ 3 1 ]}
    {:id 21 :alliance "OWA" :team "WoodElf" :race "" :position "Wardancer" :skills [ :wardancer ] :abilities [ :tackle ] :spp [ 3 1 ]}
    {:id 22 :alliance "OWA" :team "WoodElf" :race "" :position "Wardancer" :skills [ :wardancer ] :abilities [ :tackle ] :spp [ 3 1 ]}
    {:id 23 :alliance "OWA" :team "WoodElf" :race "" :position "Treeman" :skills [ :fend ] :abilities [ :tackle ] :spp [ 4 2 ]}
    ; dwarf 24-35
    {:id 24 :alliance "OWA" :team "Dwarf" :race "Dwarf" :position "Longbeard" :skills [ :guard ] :abilities [] :spp [ 1 0 ]}
    {:id 25 :alliance "OWA" :team "Dwarf" :race "Dwarf" :position "Longbeard" :skills [ :guard ] :abilities [] :spp [ 1 0 ]}
    {:id 26 :alliance "OWA" :team "Dwarf" :race "Dwarf" :position "Longbeard" :skills [ :guard ] :abilities [] :spp [ 1 0 ]}
    {:id 27 :alliance "OWA" :team "Dwarf" :race "Dwarf" :position "Longbeard" :skills [ :guard ] :abilities [] :spp [ 1 0 ]}
    {:id 28 :alliance "OWA" :team "Dwarf" :race "Dwarf" :position "Runner" :skills [ :standfirm ] :abilities [ :pass ] :spp [ 2 1 ]}
    {:id 29 :alliance "OWA" :team "Dwarf" :race "Dwarf" :position "Runner" :skills [ :standfirm ] :abilities [ :pass ] :spp [ 2 1 ]}
    {:id 30 :alliance "OWA" :team "Dwarf" :race "Dwarf" :position "Blocker" :skills [ :guard ] :abilities [ :tackle ] :spp [ 3 2 ]}
    {:id 31 :alliance "OWA" :team "Dwarf" :race "Dwarf" :position "Blocker" :skills [ :guard ] :abilities [ :tackle ] :spp [ 3 2 ]}
    {:id 32 :alliance "OWA" :team "Dwarf" :race "Dwarf" :position "Blitzer" :skills [ :stripball ] :abilities [ :tackle ] :spp [ 3 2 ]}
    {:id 33 :alliance "OWA" :team "Dwarf" :race "Dwarf" :position "Blitzer" :skills [ :stripball ] :abilities [ :tackle ] :spp [ 3 2 ]}
    {:id 34 :alliance "OWA" :team "Dwarf" :race "" :position "Troll Slayer" :skills [ :dauntless ] :abilities [ :cheat :tackle ] :spp [ 3 2 ]}
    {:id 35 :alliance "OWA" :team "Dwarf" :race "" :position "Troll Slayer" :skills [ :dauntless ] :abilities [ :cheat :tackle ] :spp [ 3 2 ]}
    ; freebooter 36-61
    {:id 36 :alliance "OWA" :team "Freebooter" :position "Aurora Silverleaf" :skills [] :abilities [ :tackle :cheat :sprint ] :spp [ 4 1 ]}
    {:id 37 :alliance "OWA" :team "Freebooter" :position "Barik Farblast" :skills [ :surehands :special ] :abilities [ :pass ] :spp [ 3 2 ]}
    {:id 38 :alliance "OWA" :team "Freebooter" :position "Cornelius Krieg" :skills [ :guard :special ] :abilities [ :tackle :cheat ] :app [ 3 1 ]}

    {:id 100 :alliance "CWC" :team "Orc" :race "Orc" :position "Lineman" :skills [ :guard ] :abilities [] :spp [ 1 0 ]}
    {:id 101 :alliance "CWC" :team "Orc" :race "Orc" :position "Lineman" :skills [ :guard ] :abilities [] :spp [ 1 0 ]}
    {:id 102 :alliance "CWC" :team "Orc" :race "Orc" :position "Lineman" :skills [ :guard ] :abilities [] :spp [ 1 0 ]}
    {:id 103 :alliance "CWC" :team "Orc" :race "Orc" :position "Lineman" :skills [ :guard ] :abilities [] :spp [ 1 0 ]}
    {:id 104 :alliance "CWC" :team "Orc" :race "Orc" :position "Thrower" :skills [ :surehands ] :abilities [ :pass ] :spp [ 2 1 ]}
    {:id 105 :alliance "CWC" :team "Orc" :race "Orc" :position "Thrower" :skills [ :surehands ] :abilities [ :pass ] :spp [ 2 1 ]}
    {:id 106 :alliance "CWC" :team "Orc" :race "" :position "Black Orc Blocker" :skills [ :frenzy ] :abilities [ :tackle ] :spp [ 3 1 ]}
    {:id 107 :alliance "CWC" :team "Orc" :race "" :position "Black Orc Blocker" :skills [ :frenzy ] :abilities [ :tackle ] :spp [ 3 1 ]}
    {:id 108 :alliance "CWC" :team "Orc" :race "Orc" :position "Blitzer" :skills [ ] :abilities [ :tackle :cheat ] :spp [ 3 1 ]}
    {:id 109 :alliance "CWC" :team "Orc" :race "Orc" :position "Blitzer" :skills [ ] :abilities [ :tackle :cheat ] :spp [ 3 1 ]}
    {:id 110 :alliance "CWC" :team "Orc" :race "Orc" :position "Blitzer" :skills [ ] :abilities [ :tackle :cheat ] :spp [ 3 1 ]}
    {:id 111 :alliance "CWC" :team "Orc" :race "" :position "Da Troll" :skills [ :pilingon ] :abilities [ :tackle :cheat ] :spp [ 4 2 ]}
    ; chaos 112-123
    ; skaven 124-135
    ; freebooter 136-161
])

(def highlights [
    {:id  0 :name "All-out Blitz"          :payout [ {:team 1} [ {:staf 2} {:fans 3} ] {:staf 2} ] }
    {:id  1 :name "Bad Snap"               :payout [ {:fans 2} [ {:fans 2 :team 1}   ] {:star 1} ] }
    {:id  2 :name "Double-crossing Routes" :payout [ {:fans 2} [ {:team 1} {:fans 2} ] {:star 3} ] }
    {:id  3 :name "Double Down"            :payout [ {:fans 2} [ {:fans 2} {:team 2} ] {:star 1} ] }
    {:id 10 :name "Razzle Dazzle"          :payout [ {:staf 1} [ {:fans 4}           ] {:staf 2} ] }
])