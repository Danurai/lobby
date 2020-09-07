(ns lobby.model)

(defonce appstate 
	(atom {
		:user-hash {}
		}))
    
(defn obfuscate-state [ uid ]
  @appstate)