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
    (assoc @appstate :games (mapv #(obfuscate-gm % uname) (:game @appstate)))
  ))
    
(defn creategame [ uname data ]
  (let [gid (-> "gm" gensym keyword)]
    (println "creating game " gid " owner " uname)
    (swap! appstate assoc :games
      (assoc 
  ; delete any game associated with uname
        (reduce-kv (fn [m k v] (if (= (:owner v) uname) (dissoc m k) (assoc m k v))) {} (:games @appstate))
        gid (assoc data :owner uname :plyrs #{uname})))))

(defn joingame [ uname gid ]
	(swap! appstate update-in [:games gid :plyrs] conj uname))

(defn leavegame [ uname gid ]
	(swap! appstate assoc :games
    (reduce-kv (fn [m k v] (if (= (:owner v) uname) (dissoc m k) (update-in m [k :plyrs] disj uname))) {} (:games @appstate))))
        
(defn gamesetup [ gname plyrs ]
  (case gname 
    "Res Arcana" (ramodel/setup plyrs)
    {}))
        
(defn startgame [ gid ]
  (let [gname (-> @appstate :games gid :game)]
    (swap! appstate assoc-in [:games gid :state] (gamesetup gname (-> @appstate :games gid :plyrs vec)))))
           
           
           
(defn updategame [ func ?data uname ]
  (swap! appstate assoc :games
    (mapv #(if (contains? (:plyrs %) uname) (assoc % :state (func (:state %) ?data uname)) %) (:games @appstate))))