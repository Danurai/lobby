(ns lobby.dadata)

(def paths {
  3 [:0 :2 :3 :4]
  4 [:0 :1C :2 :3 :4]
  5 [:0 :1B :2 :3 :4]
  6 [:0 :1A :2 :3 :4]})

(def spawns {
  3 {:maj 2 :min 1}
  4 {:maj 3 :min 1}
  5 {:maj 4 :min 2}
  6 {:maj 5 :min 3}
})

(def locations [
    {:id :void3 :stage :0 :name "Void Lock" :teams 3 :blipcount {:top 6 :bot 6} :terrain [{:id :door :facing :top :pos 1}{:id :corner :facing :top :pos 3}{:id :vent :facing :bot :pos 3}{:id :corridor :facing :bot :pos 2}] }
    {:id :void4 :stage :0 :name "Void Lock" :teams 4 :blipcount {:top 7 :bot 7} :terrain [{:id :door :facing :top :pos 1}{:id :corner :facing :top :pos 3}{:id :vent :facing :bot :pos 4}{:id :corridor :facing :bot :pos 2}] }
    {:id :void5 :stage :0 :name "Void Lock" :teams 5 :blipcount {:top 8 :bot 8} :terrain [{:id :door :facing :top :pos 2}{:id :corner :facing :top :pos 4}{:id :vent :facing :bot :pos 5}{:id :corridor :facing :bot :pos 3}] }
    {:id :void6 :stage :0 :name "Void Lock" :teams 6 :blipcount {:top 9 :bot 9} :terrain [{:id :door :facing :top :pos 2}{:id :corner :facing :top :pos 6}{:id :vent :facing :bot :pos 3}{:id :corridor :facing :bot :pos 3}] }
  ])
  
;    {"id":1,"text":"Setup for 1 player (6 Space Marines)",       ,
;    {"id":2,"text":"Setup for 2 or 4 players (8 Space Marines)", ,
;    {"id":3,"text":"Setup for 5 players (10 Space Marines)",     ,
;    {"id":4,"text":"Setup for 3 or 6 players (12 Space Marines)",

(def terrain [
  {:id :door 
    :threat 2 :support true :text "Activate: Place 1 support token on this card. When travelling, the current player may first slay 1 Genestealer in the formation for each token on this card." :set "SL06"}
  {:id :corner 
    :threat 3 :support false :text "" :set "SL06"},
  {:id :corridor 
    :threat 1 :support false :text "" :set "SL06"},
  {:id :vent 
    :threat 4 :support false :text "" :set "SL06"},
  {:id :artefact 
    :threat 1 :support false :text "Activate: Place this card in your hand. You may discard this card after 1 of your defending Space marines rolls the die to make the attack miss." :set "SL06"}
  {:id :control 
    :threat 2 :support true :text "Activate: Use the current Location card's \"Activate Control Panel\" ability." :set "SL06"},
  {:id :tank 
    :threat 3 :support false :text "Acivate: Discard this Terrain card and slay all Genestealers on this position. Then roll a die, if you roll [0], slay this Space Marine." :set "SL06"}
  {:id :chimney 
    :threat 4 :support false :text "Activate: Roll a die, if you roll [skull], discard this Terrain card." :set "SL06"}
])
  
(def genestealers nil) ; claw bite tail etc
(def teams {        ; Players: Teams {1 3, 2 2, 3 2, 4 1, 5 1, 6 1}
    :blue {
      :orders [
         {:id 3  :type :support :squad :blue :name "Counter Attack"  :text "Each time Sergeant Lorenzo rolls a [skull] <b>while defending</b> the attack misses and slay 1 of the attacking Genestealers. If the swarm still contains a Genestealer, it attacks again."}
         {:id 11 :type :move    :squad :blue :name "Intimidation"    :text "After resolving this card's action, you may roll a die. Shuffle that many Genestealer cards (of your choice) engaged with your Space Marines into the smallest blip pile."}
         {:id 13 :type :attack  :squad :blue :name "Lead by Example" :text "When 1 of your Space Marines slays a Genestealer, you may place 1 Support Token on any Space Marine (limit once per round)."}
      ]
      :members [
         {:id 1 :type :sword :squad :blue :range 2 :name "Brother Deino" :text "Brother Deino's expert marksmanship often rallies those around him and causes enemies to have second thoughts about attacking his squad."}
         {:id 2 :type :sword :squad :blue :range 2 :name "Sergeant Lorenzo" :text "Sergeant Lorenzo inspires those around him with his heroism, and can take turn aside Genestealer attacks with parries of his power sword."}
      ]}
    :gray {
      :orders [
         {:id 6 :type :support :squad :gray :name "Power Field" :text "After resolving this card's action, you may choose any swarm. Genestealers in the chosen swarm may not attack or be slain this round."}
         {:id 8 :type :move :squad :gray :name "Stealth Tactics" :text "After resolving this card's action, you may discard 1 card from either blip pile. You may then spend 1 Support Token to discard 1 card from the other blip pile."}
         {:id 15 :type :attack :squad :gray :name "Psionic Attack" :text "Each time Lexicanium Calistarius rolls a [skull] while attacking, he may immediately make 1 additional attack."}
      ]
      :members [
         {:id 3 :type :book :squad :gray :range 2 :name "Brother Scipio" :text "Brother Scipio relies on his wits to stay one step ahead of Genestealer ambushes in the long, dark corridors of the space hulk."}
         {:id 4 :type :book :squad :gray :range 2 :name "Lexicanium Calistarius" :text "Lexicanium Calistarius's psionic powers tear apart large numbers of Genestealers. His power field psychic power can lock down an entire swarm."}
      ]}
    :green {
      :orders [
         {:id 1 :type :support :squad :green :name "Block" :text "Each time Sergeant Gideon rolls a [skull] <b>while defending</b>, the attack misses."}
         {:id 12 :type :move :squad :green :name "Run and Gun" :text "After resolving this card's action, each of your Space Marines may spend 1 Support Token to make 1 attack."}
         {:id 16 :type :attack :squad :green :name "Dead Aim" :text "Each time 1 of your attacking Space marines rolls a [4], slay up to 3 Genestealers from the defending swarm."}
      ]
      :members [
         {:id 5 :type :hammer :squad :green :range 2 :name "Brother Noctis" :text "In the heat of battle, Brother Noctis stays cool and clear-headed. His years of service have earned the trust and respect of Sergeant Gideon"}
         {:id 6 :type :hammer :squad :green :range 0 :name "Sergeant Gideon" :text "Sergeant Gideon's storm shield can form an impenetrable wall against his enemies. His thunder hammer makes disposing of genestealers easy."}
      ]}
    :purple {
      :orders [
         {:id 5 :type :support :squad :purple :name "Strategize" :text "After resolving this card's action, you may move 1 swarm to an adjacent position and/or move it to the other side of the formation."}
         {:id 10 :type :move :squad :purple :name "Forward Scouting" :text "After resolving this card's action, you may look at the top card of the Event Deck. Then place it on the top or bottom of the deck."}
         {:id 14 :type :attack :squad :purple :name "Flamer Attack" :text "When Brother Zael attacks, ignore all [skull] rolled. Instead, slay a number of Genestealers in the swarm equal to the number rolled."}
      ]
      :members [
         {:id 7 :type :flamer :squad :purple :range 2 :name "Brother Omnio" :text "Brother Omnio tracks down the Genestealer swarms with his auspex, preparing his battle brothers for the coming rush."}
         {:id 8 :type :flamer :squad :purple :range 1 :name "Brother Zael" :text "Brother Zael can incinerate large numbers of Genestealers with a single blast from his heavy flamer."}
      ]}
    :red {
      :orders [
         {:id 4 :type :support :squad :red :name "Overwatch" :text "At the end of the Event Phase, each of your Space Marines may spend 1 Support Token to make 1 attack."}
         {:id 7 :type :move :squad :red :name "Onward Brothers!" :text "Each time 1 of your Space Marines activates a Door, you may place 1 additional Support Token on the Terrain Card."}
         {:id 17 :type :attack :squad :red :name "Full Auto" :text "Brother Leon may attack up to 3 times (instead of just once)"}
      ]
      :members [
         {:id 9 :type :assault :squad :red :range 3 :name "Brother Leon" :text "Brother Leon's assault cannon provides unmatched power and range. Only one thing matters to Brother Leon in battle: kill count."}
         {:id 10 :type :assault :squad :red :range 2 :name "Brother Valencio" :text "Brother Valencio cuts through doors and Genestealers alike with his powerful chain fist. Valencio is eager to prove himself to the veterans of the squad."}
      ]}
    :yellow {
      :orders [
         {:id 2 :type :support :squad :yellow :name "Defensive Stance" :text "Each time 1 of your <b>defending</b> Space Marine spends a Support Token to reroll a die, the attack misses unless the new roll is a [0]"}
         {:id 9 :type :move :squad :yellow :name "Reorganize" :text "Your Space Marines may move to any position in the formation (instead of just adjacent positions)"}
         {:id 18 :type :attack :squad :yellow :name "Heroic Charge" :text "Instead of attacking with Brother Claudio, you <b>may</b> slay up to 3 Genestealers within 1 range of him (ignoring facing). Then roll a die. If you roll a [0] Brother Claudio is slain."}
      ]
        :members [
         {:id 11 :type :claw :squad :yellow :range 0 :name "Brother Claudio" :text "Brother Claudio bravely dives into battle with little reservation for his own mortality. His control combat prowess has kept him alive in many sticky situations."}
         {:id 12 :type :claw :squad :yellow :range 2 :name "Brother Goriel" :text "Brother Goriel carefully balances his natural aggression with his trained discipline. Brother Goriel is never far away when violence is brewing."}
      ]}
      })

(def events [
  {:id 1 :name "Chaos of Battle" :spawn [{:threat 4 :type :min},{:threat 2 :type :min}] :swarm "skull" :action "move" :text "Change every Space Marine's facing."},
  {:id 2 :name "Chaos of Battle" :spawn [{:threat 4 :type :min},{:threat 2 :type :min}] :swarm "skull" :action "move" :text "Change every Space Marine's facing."},
  {:id 3 :name "Cleansing Flames" :spawn [{:threat 3 :type :maj},{:threat 1 :type :maj}] :swarm "tongue" :action "move" :text "<b>Instinct</b> Choose a Space Marine and roll a die. If you roll a [skull] slay 2 Genestealers engaged with him (of your choice)."},
  {:id 4 :name "Enter Formation" :spawn [{:threat 3 :type :maj},{:threat 2 :type :maj}] :swarm "claw" :action "move" :text "Each time a player resolves a Move + Activate action card next round, he may first place 1 Support Token in any Space Marine."},
  {:id 5 :name "Evasion" :spawn [{:threat 2 :type :min},{:threat 4 :type :min}] :swarm "skull" :action "flank" :text "When a player resolves an Attack Action next round, he may only attack with 1 Space Marine of that Combat Team (instead of both)."},
  {:id 6 :name "Flanking Manoeuvre" :spawn [{:threat 4 :type :min},{:threat 3 :type :min}] :swarm "null" :action "null" :text "Move all Swarms so that they are behind their engaged Space Marine."},
  {:id 7 :name "For my Battle Brothers!" :spawn [{:threat 4 :type :maj},{:threat 3 :type :min}] :swarm "tail" :action "move" :text "<b>Instinct</b> Choose a Space Marine that has at least 1 Support Token (if able). Discard 1 Support Token form him and 1 Genestealer engaged with him (of your choice)."},
  {:id 8 :name "For my Battle Brothers!" :spawn [{:threat 4 :type :maj},{:threat 3 :type :min}] :swarm "tail" :action "move" :text "<b>Instinct</b> Choose a Space Marine that has at least 1 Support Token (if able). Discard 1 Support Token form him and 1 Genestealer engaged with him (of your choice)."},
  {:id 9 :name "Full Scan" :spawn [{:threat 4 :type :maj},{:threat 2 :type :maj}] :swarm "tongue" :action "move" :text "<b>Instinct</b> Choose a blip pile. Discard the top card of the chosen pile."},
  {:id 10 :name "Full Scan" :spawn [{:threat 4 :type :maj},{:threat 2 :type :maj}] :swarm "tongue" :action "move" :text "<b>Instinct</b> Choose a blip pile. Discard the top card of the chosen pile."},
  {:id 11 :name "Gun Jam" :spawn [{:threat 2 :type :maj},{:threat 4 :type :min}] :swarm "tail" :action "move" :text "<b>Instinct</b> Choose a Combat Team that did not reveal an Attack Action this round. Next round,  that combat team may not play an Attack Action card."},
  {:id 12 :name "Gun Jam" :spawn [{:threat 2 :type :maj},{:threat 4 :type :min}] :swarm "tail" :action "move" :text "<b>Instinct</b> Choose a Combat Team that did not reveal an Attack Action this round. Next round,  that combat team may not play an Attack Action card."},
  {:id 13 :name "Out of Thin Air" :spawn [{:threat 3 :type :min},{:threat 4 :type :min}] :swarm "skull" :action "flank" :text "<b>Instinct:</b> Choose a Space Marine. Spawn 2 Genestealers behind him."},
  {:id 14 :name "Out of Thin Air" :spawn [{:threat 3 :type :min},{:threat 4 :type :min}] :swarm "skull" :action "flank" :text "<b>Instinct:</b> Choose a Space Marine. Spawn 2 Genestealers behind him."},
  {:id 15 :name "Outnumbered" :spawn [{:threat 3 :type :min},{:threat 4 :type :min}] :swarm "claw" :action "flank" :text "Discard all Support Tokens from each Space Marine that is engaged with at least 1 Swarm."},
  {:id 16 :name "Psychic Assault" :spawn [{:threat 4 :type :min},{:threat 3 :type :min}] :swarm "tongue" :action "flank" :text "<b>Instinct</b> Choose a Space Marine and roll a die. If you roll a [0] or [1], the space marine is slain."},
  {:id 17 :name "Quick Instincts" :spawn [{:threat 4 :type :maj},{:threat 3 :type :maj}] :swarm "tongue" :action "move" :text "<b>Instinct</b> Choose a Space Marine. He may immediately make 1 attack."},
  {:id 18 :name "Rescue Space Marine" :spawn [{:threat 3 :type :maj},{:threat 4 :type :maj}] :swarm "claw" :action "move" :text "<b>Instinct</b> Choose a Space Marine that has been slain belonging to a non-eliminated Combat Team. Place the Space Marine card at the bottom of the formation facing the right."},
  {:id 19 :name "Resupply" :spawn [{:threat 4 :type :maj},{:threat 2 :type :min}] :swarm "skull" :action "move" :text "<b>Instinct</b> Choose a Space Marine. Move all Support Tokens to him from all other Space Marines."},
  {:id 20 :name "Rewarded Faith" :spawn [{:threat 1 :type :maj},{:threat 4 :type :min}] :swarm "tail" :action "move" :text "<b>Instinct</b> Choose a Space Marine. You may discard any number of Support Tokens from him to slay an equal number of Genestealers engaged with him."},
  {:id 21 :name "Second Wind" :spawn [{:threat 2 :type :maj},{:threat 4 :type :maj}] :swarm "tongue" :action "move" :text "<b>Instinct</b> Choose a Space Marine. Each time he rolls a [0] while defending next round, the attack misses."},
  {:id 22 :name "Secret Route" :spawn [{:threat 3 :type :maj},{:threat 1 :type :maj}] :swarm "claw" :action "move" :text "If there is a Door terrain card in the formation, place 2 Support Tokens on it."},
  {:id 23 :name "Secret Route" :spawn [{:threat 3 :type :maj},{:threat 1 :type :maj}] :swarm "claw" :action "move" :text "If there is a Door terrain card in the formation, place 2 Support Tokens on it."},
  {:id 24 :name "Stalking from the Shadows" :spawn [{:threat 4 :type :maj},{:threat 1 :type :maj}] :swarm "head" :action "move" :text "<b>Instinct</b> Choose a Space Marine with at least 1 Support Token. DIscard all his Support Tokens."},
  {:id 25 :name "Stalking from the Shadows" :spawn [{:threat 4 :type :maj},{:threat 1 :type :maj}] :swarm "head" :action "move" :text "<b>Instinct</b> Choose a Space Marine with at least 1 Support Token. DIscard all his Support Tokens."},
  {:id 26 :name "Surrounded" :spawn [{:threat 4 :type :min},{:threat 3 :type :min}] :swarm "null" :action "null" :text "<b>Instinct</b> Choose a Space Marine. Move all Genestealers (from every position) to the chosen Space Marine's position (do not change their side)."},
  {:id 27 :name "Temporary Sanctuary" :spawn [{:threat 4 :type :maj},{:threat 3 :type :maj}] :swarm "claw" :action "move" :text "<b>Instinct</b> Choose a swarm of Genestealers. Shuffle all cards from the chose swarm into the smallest blip pile."},
  {:id 28 :name "The Swarm" :spawn [{:threat 2 :type :min},{:threat 3 :type :min}] :swarm "tail" :action "flank" :text "Place 2 Genestealer cards into each blip pile (from the Genestealer deck)."},
  {:id 29 :name "The Swarm" :spawn [{:threat 2 :type :min},{:threat 3 :type :min}] :swarm "tail" :action "flank" :text "Place 2 Genestealer cards into each blip pile (from the Genestealer deck)."},
  {:id 30 :name "They're Everywhere!" :spawn [{:threat 3 :type :min},{:threat 4 :type :min}] :swarm "tail" :action "flank" :text "Spawn 1 Genestealer in front of each Space Marine that is not enagaged with a swarm."}
])
;{
;  "location": [
;    {"id":1,"name":"Void Lock","text":"Setup for 1 player (6 Space Marines)","blipcount":[6,6],"terrain":[1,2,4,3],"terrainlocation":[1,3,3,2],"spawn":{"major":2,"minor":1},"setup":["2","3","4"],"deck":"0","set":"SL06"},
;    {"id":2,"name":"Void Lock","text":"Setup for 2 or 4 players (8 Space Marines)","blipcount":[7,7],"terrain":[1,2,4,3],"terrainlocation":[1,3,4,2],"spawn":{"major":3,"minor":1},"setup":["1C","2","3","4"],"deck":"0","set":"SL06"},
;    {"id":3,"name":"Void Lock","text":"Setup for 5 players (10 Space Marines)","blipcount":[8,8],"terrain":[1,2,4,3],"terrainlocation":[2,4,5,3],"spawn":{"major":4,"minor":2},"setup":["1B","2","3","4"],"deck":"0","set":"SL06"},
;    {"id":4,"name":"Void Lock","text":"Setup for 3 or 6 players (12 Space Marines)","blipcount":[9,9],"terrain":[1,2,4,3],"terrainlocation":[2,5,6,3],"spawn":{"major":5,"minor":3},"setup":["1A","2","3","4"],"deck":"0","set":"SL06"},
;    {"id":5,"name":"Maintenance Tunnels","text":"<b>Activate Control Panel:</b><br>Discard the \"Control Panel\" Terrain Card, and replace it with a \"Corridor\" Terrain Card.","blipcount":[8,8],"terrain":[2,6,1,4],"terrainlocation":[3,7,5,3],"spawn":{"major":0,"minor":0},"setup":"","deck":"1A","set":"SL06"},
;    {"id":6,"name":"Main Corridor","text":"<b>Upon Entering:</b><br>Spawn 2 Genestealers on the \"Corridor\" terrain card.","blipcount":[7,8],"terrain":[4,1,3,2],"terrainlocation":[2,5,7,3],"spawn":{"major":0,"minor":0},"setup":"","deck":"1A","set":"SL06"},
;    {"id":7,"name":"Service Shaft","text":"<b>Upon Entering:</b><br> make all marines face right","blipcount":[7,7],"terrain":[2,4,3,1],"terrainlocation":[2,6,6,3],"spawn":{"major":0,"minor":0},"setup":"","deck":"1A","set":"SL06"},
;    {"id":8,"name":"Cryo Control","text":"<b>Activate Control Panel:</b><br>Discard 1 card from the blip pile of your choice.","blipcount":[7,7],"terrain":[6,4,2,3],"terrainlocation":[1,4,5,2],"spawn":{"major":0,"minor":0},"setup":"","deck":"1B","set":"SL06"},
;    {"id":9,"name":"Wreckage Labyrinth","text":"<b>Upon Entering:</b><br>Change the facing of each Space Marine who is facing a Terrain Card in his position.","blipcount":[7,6],"terrain":[4,3,7,1],"terrainlocation":[1,5,5,3],"spawn":{"major":0,"minor":0},"setup":"","deck":"1B","set":"SL06"},
;    {"id":10,"name":"Lower Accessway","text":"<b>Upon Entering:</b></br>Spawn 2 Genestealers behind the Space Marine at the top of the formation.","blipcount":[7,7],"terrain":[4,3,2,1],"terrainlocation":[3,4,5,3],"spawn":{"major":0,"minor":0},"setup":"","deck":"1B","set":"SL06"},
;    {"id":11,"name":"Munitorium","text":"<b>Upon Entering:</b><br>The current player places 1 Support Token each on any 2 Space Marines.","blipcount":[6,7],"terrain":[7,1,2,4],"terrainlocation":[3,5,4,2],"spawn":{"major":0,"minor":0},"setup":"","deck":"1C","set":"SL06"},
;    {"id":12,"name":"Core Cogitator","text":"<b>Activate Control Panel:</b><br>Choose a Terrain card. A maximum of 1 Genestealer may spawn on that Terrain card during the next Event Phase.","blipcount":[7,5],"terrain":[3,4,2,6],"terrainlocation":[1,4,3,1],"spawn":{"major":0,"minor":0},"setup":"","deck":"1C","set":"SL06"},
;    {"id":13,"name":"Apothecarion","text":"<b>Activate Control Panel:</b><br>Place 1 Support Token on any Space marine. You may then change that Space Marine's facing.","blipcount":[7,6],"terrain":[6,4,3,2],"terrainlocation":[2,3,5,2],"spawn":{"major":0,"minor":0},"setup":"","deck":"1C","set":"SL06"},
;    {"id":14,"name":"Teleportarium","text":"<b>Activate Control Panel:</b><br>Each Space Marine must discard 1 Support Token or roll a die. On a [0] the Space Marine is slain. Then, regardless of the dice rolls, discard all cards from both blip piles.","blipcount":[7,5],"terrain":[4,2,6,3],"terrainlocation":[2,4,4,1],"spawn":{"major":0,"minor":0},"setup":"","deck":"2","set":"SL06"},
;    {"id":15,"name":"Black Holds","text":"<b>Upon Entering:</b><br>The current player chooses a swarm (if able), and spawns 2 Genestealers on it. if 0 swarms in the formation, this has no effect.","blipcount":[5,6],"terrain":[2,1,4,7],"terrainlocation":[2,4,3,2],"spawn":{"major":0,"minor":0},"setup":"","deck":"2","set":"SL06"},
;    {"id":16,"name":"Dark Catacombs","text":"<b>Upon Entering:</b><br>The current player must choose a Space Marine with 0 support tokens (if able). Spawn 1 Genestealer behind a chosen Space Marine.","blipcount":[6,6],"terrain":[2,1,4,3],"terrainlocation":[1,3,5,4],"spawn":{"major":0,"minor":0},"setup":"","deck":"2","set":"SL06"},
;    {"id":17,"name":"Wrath of Baal Chapel","text":"<b>Upon Entering:</b><br>The current player places the \"Artefact\" Terrain card on any position and side of the formation.","blipcount":[5,6],"terrain":[4,3,1,2],"terrainlocation":[1,3,4,1],"spawn":{"major":0,"minor":0},"setup":"","deck":"3","set":"SL06"},
;    {"id":18,"name":"Generatorium","text":"<b>Activate Control Panel:</b><br>Roll a die. If you roll [skull], slay up to 4 Genestealers of your choice (in any positions). Otherwise slay this Space Marine.","blipcount":[5,5],"terrain":[3,2,6,4],"terrainlocation":[1,3,4,3],"spawn":{"major":0,"minor":0},"setup":"","deck":"3","set":"SL06"},
;    {"id":19,"name":"Hibernation Cluster","text":"<b>Upon Entering:</b><br>For each Space Marine in the formation, place 1 Genestealer card into <b>each</b> blip pile from the Genestealer deck.","blipcount":[0,0],"terrain":[4,1,8,2],"terrainlocation":[3,4,2,1],"spawn":{"major":0,"minor":0},"setup":"","deck":"3","set":"SL06"},
;    {"id":20,"name":"Launch Control Room","text":"<b>Activate Control Panel:</b><br>Place a Support Token on this card or roll a die. If the roll is equal or less than the number of support tokens on this card, Space Marines win. Otherwise there is no effect.","blipcount":[6,6],"terrain":[2,6,4,3],"terrainlocation":[1,3,2,1],"spawn":{"major":0,"minor":0},"setup":"","deck":"4","set":"SL06"},
;    {"id":21,"name":"Toxin Pumping Station","text":"<b>Activate Control Panel:</b><br>Roll a die and discard that many cards from the blip pile of your choice. Space Marines may only win if there are 0 cards in <b>both</b> blip piles and 0 Genestealer cards in the formation.","blipcount":[7,7],"terrain":[3,2,6,4],"terrainlocation":[1,2,2,1],"spawn":{"major":0,"minor":0},"setup":"","deck":"4","set":"SL06"},
;    {"id":22,"name":"Genestealer Lair","text":"<b>Upon Entering:</b><br>Move all swarms to the red Terrain card on their side of the formation. Then Spawn 1 Brood Lord on each red Terrain card. If both Brood Lords are slain, Space Marines win.","blipcount":[6,5],"terrain":[2,4,3,8],"terrainlocation":[1,2,2,1],"spawn":{"major":0,"minor":0},"setup":"","deck":"4","set":"SL06"}
;  ],
;  "terrain": [
;    {"id":1,"name":"Door","threat":2,"support":true,"text":"Activate: Place 1 support token on this card. When travelling, the current player may first slay 1 Genestealer in the formation for each token on this card.","set":"SL06"},
;    {"id":2,"name":"Dark Corner","threat":3,"support":false,"text":"","set":"SL06"},
;    {"id":3,"name":"Corridor","threat":1,"support":false,"text":"","set":"SL06"},
;    {"id":4,"name":"Ventilation Duct","threat":4,"support":false,"text":"","set":"SL06"},
;    {"id":5,"name":"Artefact","threat":1,"support":false,"text":"Activate: Place this card in your hand. You may discard this card after 1 of your defending Space marines rolls the die to make the attack miss.","set":"SL06"},
;    {"id":6,"name":"Control Panel","threat":2,"support":true,"text":"Activate: Use the current Location card's \"Activate Control Panel\" ability.","set":"SL06"},
;    {"id":7,"name":"Promethean Tank","threat":3,"support":false,"text":"Acivate: Discard this Terrain card and slay all Genestealers on this position. Then roll a die, if you roll [0], slay this Space Marine.","set":"SL06"},
;    {"id":8,"name":"Spore Chimney","threat":4,"support":false,"text":"Activate: Roll a die, if you roll [skull], discard this Terrain card.","set":"SL06"}
;  ]