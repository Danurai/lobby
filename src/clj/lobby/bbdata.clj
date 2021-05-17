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
    ; dwarf 24-35
    ; freebooter 36-61

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