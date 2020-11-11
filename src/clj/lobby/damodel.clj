(ns lobby.damodel
  (:require [lobby.dadata :as data]))
  
(defn setup [ plyrs ]
  {
    :status :setup
    :players (zipmap plyrs (repeat {:public {:teams []}}))
    :teams data/teams
    :teamlimit (nth [0 3 2 2 2 2 1] (count plyrs))
    :timelimit 0
    :chat []
  } )
  
(defn pickteam [ gs uname tname ]
  (let [drop? (-> gs :teams tname :cmdr (= uname))]
    (if (or drop? (> (:teamlimit gs) (->> gs :teams (filter (fn [[k v]] (= (:cmdr v) uname))) count)))
        (assoc gs
          :teams
            (reduce-kv 
              (fn [m k v]
                (assoc m k
                  (if (= k tname)
                      (if drop?
                         (dissoc v :cmdr)
                         (assoc v :cmdr uname))
                      v))) 
              {} (:teams gs)))
          gs)))
          
(defn startgame [ gs ]
  gs)
          
(defn parseaction [ gs ?data uname ]
  (prn ?data)
  (case (:action ?data)
    :pickteam (pickteam gs uname (:team ?data))
    gs))