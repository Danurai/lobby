(ns lobby.model
  (:require 
    [lobby.ramodel :as ramodel]))

(defonce appstate 
	(atom {
		:user-hash {}
    :games {}
	}))
  
(defn obfuscate-gm [ gm uname ]
  (assoc gm :state (ramodel/obfuscate uname)))
    
(defn obfuscate-state [ uid ]
  (let [reverseuidlookup (reduce-kv #(assoc %1 %3 %2) {} (:user-hash @appstate))
        uname (get reverseuidlookup uid)]
    ;(prn uid uname (assoc @appstate :games (mapv #(obfuscate-gm % uname) (:game @appstate))))
    @appstate))
    
(defn creategame [ uname data ]
  (let [gid (gensym "gm")]    
    (swap! appstate assoc :games
      (conj
        (vec (filter #(not= (:owner %) uname) (:games @appstate)))
        (assoc data 
          :owner uname
          :gid (gensym "gm")
          :plyrs #{uname})))))

(defn joingame [ uname gid ]
	(swap! appstate assoc :games
		(mapv #(if (= (:gid %) gid)
								(update % :plyrs conj uname)
								%) (:games @appstate))))

(defn leavegame [ uname gid ]
  (swap! appstate assoc :games 
    (->> @appstate
        :games
        (map #(if (not= (:owner %) uname) (update % :plyrs disj uname)))
        (remove nil?)
        vec)))
        
(defn gamesetup [ gname plyrs ]
  (case gname 
    "Res Arcana" (ramodel/setup plyrs)
    {}))
        
(defn startgame [?data]
  (let [gid (:gid ?data) 
        gname (:gname ?data)]
    (swap! appstate assoc :games 
      (->> @appstate
           :games
           (map #(if (= (:gid %) gid) (assoc % :state (gamesetup gname (-> % :plyrs vec))) %))))))