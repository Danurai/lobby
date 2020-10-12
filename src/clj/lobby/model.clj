(ns lobby.model)

(defonce appstate 
	(atom {
		:user-hash {}
    :games []
	}))
    
(defn obfuscate-state [ uid ]
  @appstate)
  
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