(ns lobby.bbmodel)

(defn obfuscate [ state uname ]
    state)

(defn parseaction [ state ?data uname]
    state)

(defn setup [ plyrs ]
    {
        :status :setup
        :players (zipmap plyrs (repeat {:public {} :private {} :secret {}}))
        :chat []
    })