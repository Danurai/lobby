(ns lobby.model
  (:require 
    [lobby.ramodel :as ramodel]
    [lobby.damodel :as damodel]
    [lobby.bbmodel :as bbmodel]
  ))

(defonce verbose? true)

(defonce gamelist {
  "Res Arcana" {
    :minp 1 :maxp 4 :has-ai true
  }
  "Death Angel" {
    :minp 1 :maxp 6
  }
  "BBTM" {
    :minp 2 :maxp 2 :has-ai true
  }})
  
(defonce appstate 
	(atom {
		:user-hash {}
    :games {}
    :gamelist gamelist
	}))

(defn addchat! 
  ([ gid uname txt event ]
    (if verbose? (println "model/addchat!" gid uname txt event))
    (let [msg {:msg txt :uname uname :timestamp (new java.util.Date)}]
      (if gid
          (case (-> @appstate :games gid :game)
            "Res Arcana" (swap! appstate assoc-in [:games gid :state] (ramodel/chat-handler (-> @appstate :games gid :state) txt uname))
            (swap! appstate update-in [:games gid :state :chat] conj msg))
          (swap! appstate update-in [:chat] conj msg))))
  ([ gid uname txt ]
    (addchat! gid uname txt :usermsg)))
  
(defn obfuscate-gm [ gm uname ]
  (if (nil? (:state gm))
      gm
      (case (:game gm)
        "Res Arcana"  (assoc gm :state (ramodel/obfuscate (:state gm) uname))
        "Death Angel" (assoc gm :state (damodel/obfuscate (:state gm) uname))
        "BBTM"        (assoc gm :state (bbmodel/obfuscate (:state gm) uname))
        gm)))
        
(defn obfuscate-state [ uid ]
  (let [reverseuidlookup (reduce-kv #(assoc %1 %3 %2) {} (:user-hash @appstate))
        uname (get reverseuidlookup uid)]
    ;@appstate))
    (assoc @appstate :games 
      (reduce-kv (fn [m k v]
        (assoc m k 
          (if (contains? (:plyrs v) uname)
              (obfuscate-gm v uname)
              (dissoc v :state)))) {} (:games @appstate)))))
            
            
; GAME MANAGEMENT
        
    
(defn creategame! [ uname data ]
  (let [gid (-> "gm" gensym keyword)]
    (addchat! nil uname (str "created " (:game data) " game " (:title data)) :gamestate)
    (swap! appstate assoc :games
      (assoc
        (reduce-kv (fn [m k v] (if (= (:owner v) uname) (dissoc m k) (assoc m k v))) {} (:games @appstate)) ; delete any game associated with uname
        gid (merge 
              (assoc data :owner uname :plyrs #{uname})
              (-> @appstate :gamelist (get (:game data))))))))

(defn joingame! [ uname gid ]
  (let [gm    (-> @appstate :games gid)
        pname (if (= uname "AI") (-> "AI" gensym str) uname)]
    (when (> (:maxp gm) (-> gm :plyrs count))
      (addchat! nil pname (str "joined " (:title gm)) :gamestate)
      (swap! appstate update-in [:games gid :plyrs] conj pname))))

(defn leavegame! [ uname gid ]
  (let [gm (-> @appstate :games gid)]
    (addchat! nil (str uname (if (= (:owner gm) uname) " (owner)")) (str "left game " (-> @appstate :games gid :title)) :gamestate)
    (swap! appstate assoc :games
      (reduce-kv (fn [m k v] (if (= (:owner v) uname) (dissoc m k) (assoc m k  (update-in v [:plyrs] disj uname)))) {} (:games @appstate)))))
    
(defn- gamesetup [ gname plyrs ]
  (case gname 
    "Res Arcana"  (ramodel/setup plyrs)
    "Death Angel" (damodel/setup plyrs)
    "BBTM"        (bbmodel/setup plyrs)
    {}))
    
(defn startgame! [ gid ]
  (let [gm (-> @appstate :games gid)]
    (when (>= (-> gm :plyrs count) (:minp gm))
      (addchat! nil (:owner gm) (str "started " (-> @appstate :games gid :name)) :gamestate)
      (swap! appstate assoc-in [:games gid :started] true)
      (swap! appstate assoc-in [:games gid :state] (gamesetup (:game gm) (:plyrs gm))))))
           
(defn updategame! [ ?data uname ]
  (if verbose? (println "model/updategame!" ?data uname))
  (when-let [gid (:gid ?data)]
    (let [game (-> @appstate :games gid)]
      (swap! appstate assoc-in [:games gid :state]
        (case (:game game)
          "Res Arcana"  (ramodel/parseaction (:state game) ?data uname)
          "Death Angel" (damodel/parseaction (:state game) ?data uname)
          "BBTM"        (bbmodel/parseaction (:state game) ?data uname)
          (:state game))))))