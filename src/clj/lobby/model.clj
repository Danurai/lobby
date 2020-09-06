(ns lobby.model)

(defonce appstate 
	(atom {
		:user-hash {}
		}))