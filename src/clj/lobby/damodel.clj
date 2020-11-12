(ns lobby.damodel
  (:require [lobby.dadata :as data]))
  
(defn setup [ plyrs ]
  {
    :status :setup
    :players (zipmap plyrs (repeat {:public {:teams []}}))
    :teams data/teams
    :teamlimit (nth [0 3 2 2 1 1 1] (count plyrs))
    :timelimit 0
    :maxsupport 12
    :chat []
  } )
  
  
(defn obfuscate [ gs plyr ]
  (assoc gs
    :events (-> gs :events count)
    :gdeck  (-> gs :gdeck  count)
))
  
(defn pickteam [ gs plyr tname ]
  (let [drop? (-> gs :teams tname :cmdr (= plyr))]
    (if (or drop? (> (:teamlimit gs) (->> gs :teams (filter (fn [[k v]] (= (:cmdr v) plyr))) count)))
        (assoc gs
          :teams
            (reduce-kv 
              (fn [m k v]
                (assoc m k
                  (if (= k tname)
                      (if drop?
                         (dissoc v :cmdr)
                         (assoc v :cmdr plyr))
                      v))) 
              {} (:teams gs)))
          gs)))
          
(defn- getlocation [ stage teamcount ]
  (->> data/locations
       (filter #(= (:stage %) stage))
       (filter #(if (= (:stage %) :0) (= (:teams %) teamcount) true))
        rand-nth))
       
(defn- travel [ gs ]
  (let [teamcount (-> gs :teams count (/ 2))
        newlocation (-> gs :path first (getlocation teamcount))]
    (assoc gs
      :path (-> gs :path rest)  ; set path TODO FINAL LOCATION?
      :location newlocation     ; set location 
      :terrain nil              ; add terrain based on newlocation
      :blips nil                ; Discard and refill blips based on newlocation
)))
          
(defn- setformation [ teams teamcount ]
  (->>  (reduce-kv 
          (fn [m k v]
            (if (-> v :cmdr some?)
                (apply conj m (:members v))
                m))
            [] teams)
        shuffle
        (map-indexed #(assoc %2 :facing (if (< %1 (/ teamcount 2)) :top :bottom)))))
    
(defn spawn [ gs ev ]
  ;(prn (:spawn ev))
  (let [threatmap (apply merge (map #(hash-map (:threat %) (get (:spawns gs) (:type %))) (:spawn ev)))]
    (prn threatmap)
    gs
))
          
(defn startgame [ gs ]
; can start?
  (let [teamcount (->> gs :teams (filter (fn [[k v]] (-> v :cmdr some?))) count)
        events (shuffle data/events)]
    (-> gs
        (assoc :gdeck  (apply concat (map #(repeat 5 %) [:claw :bite :tail :head])))  ; blip pile
        (assoc :path   (get data/paths teamcount))                                    ; set mission path
        (assoc :spawns (get data/spawns teamcount))                                   ; set spawn numbers (Void Lock)
        (assoc :formation (setformation (:teams gs) teamcount))                       ; set formation
        travel                                                                        ; travel to Void Lock
        (assoc :event (first events) :events (rest events))                           ; select event (for spawns)
        (spawn (first events))                                                        ; spawn
        )                       
))
          
(defn parseaction [ gs ?data plyr ]
  (case (:action ?data)
    :start    (startgame gs)
    :pickteam (pickteam gs plyr (:team ?data))
    gs))