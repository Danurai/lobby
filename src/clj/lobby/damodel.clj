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
  }
)  
  
(defn obfuscate [ gs plyr ]
  (assoc gs
    :events   (-> gs :events count)
    :blipdeck (-> gs :blipdeck count)
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
        
(defn- set-terrain [ formation terrain ]
; terrain [{:zone n :terrain [{:id :door :facing :top :pos 1} ...]}]
  (let [flen (count formation)
        adjterrain (map #(if (= (:facing %) :bot) (assoc % :pos (->> % :pos (- flen) inc)) %) terrain)]
    (mapv
      (fn [zone]
        (let [zt (->> adjterrain (filter #(= (:pos %) (:zone zone))) first)
              ter (->> data/terrain (filter #(= (:id %) (:id zt))) first)]
          (if (some? zt)
              (assoc zone :terrain [(merge zt ter)])
              zone)
          )) formation)))
       
(defn- travel [ gs ]
  (let [teamcount (-> gs :teams count (/ 2))
        newlocation (-> gs :path first (getlocation teamcount))]
    (assoc gs
      :path      (-> gs :path rest)                                    ; set path TODO FINAL LOCATION?
      :location  newlocation                                           ; set location 
      :formation (set-terrain (:formation gs) (:terrain newlocation))  ; add terrain based on newlocation
      :blips     (:blipcount newlocation)                              ; Discard and refill blips based on newlocation
)))
          
(defn- setformation [ teams teamcount ]
  (->>  (reduce-kv 
          (fn [m k v]
            (if (-> v :cmdr some?)
                (apply conj m (:members v))
                m))
            [] teams)
        shuffle
        (map-indexed 
          #(hash-map 
              :zone (inc %1)
              ;:terrain [] :swarm []
              :marine (assoc %2 :facing (if (< %1 teamcount) :top :bot))))))
    
(defn spawn [ gs {:keys [spawnzone spawnfacing spawnqty]}] ;spawnzone spawnfacing spawnqty ]
  (let [sq (min (-> gs :blips spawnfacing) spawnqty)
        blipdeck (:blipdeck gs)] ;TRAVEL ?
    ;(prn spawnzone spawnfacing spawnqty)
    (assoc gs
      :formation (mapv 
                  (fn [zone] 
                    (if (= (:zone zone) spawnzone)
                        (apply update zone :swarm conj (map #(assoc % :facing spawnfacing) (take spawnqty blipdeck))) 
                        zone)) (:formation gs))
      :blipdeck (nthrest blipdeck sq)
      
    )))
    
    
(defn event-spawn [ gs ev ]
  (let [threatmap (apply merge (map #(hash-map (:threat %) (get (:spawns gs) (:type %))) (:spawn ev)))
        spawnmap  (->>    ; {:zone #, :facing :top/:bot :qty qty)
                    (map #(map (fn [t] {:spawnzone (:zone %) :spawnfacing (:facing t) :spawnqty (get threatmap (:threat t))}) (:terrain %)) (:formation gs))
                    (apply concat)
                    (filter #(some? (:spawnqty %)))
                    )]
; Location Movement? (not turn 1)
    ;(prn threatmap spawnmap)
    ;(prn (reduce spawn gs spawnmap))
; Event Spawns
    (reduce spawn gs spawnmap)
; Event Effect? Macro/option?
))


(def blipdeck 
  (map-indexed 
    #(hash-map :id %1 :type %2)
    (apply concat (map #(repeat 5 %) [:claw :bite :tail :head]))))
          
(defn startgame [ gs ]
; can start?
  (let [teamcount (->> gs :teams (filter (fn [[k v]] (-> v :cmdr some?))) count)
        events (shuffle data/events)]
    (-> gs
        (assoc :status :started)
        (assoc :blipdeck blipdeck)  ; blip pile
        (assoc :path   (get data/paths teamcount))                                    ; set mission path
        (assoc :spawns (get data/spawns teamcount))                                   ; set spawn numbers (Void Lock)
        (assoc :formation (setformation (:teams gs) teamcount))                       ; set formation
        travel                                                                        ; travel to Void Lock
        (assoc :event (first events) :events (rest events))                           ; select event (for spawns)
        (event-spawn (first events))                                                  ; spawn
    )))
          
(defn parseaction [ gs ?data plyr ]
  (case (:action ?data)
    :start    (startgame gs)
    :pickteam (pickteam gs plyr (:team ?data))
    gs))