  (ns lobby.model
	(:require [reagent.core :as r]))

(def app (r/atom {}))

(def gm (r/atom nil))
(def gid (r/atom nil))
(def uname (r/atom (.. js/document (getElementById "loginname") -textContent)))


(defn timeformat [ t ]
  (str 
    (-> t .getHours   .toString (.padStart 2 "0")) ":"
    (-> t .getMinutes .toString (.padStart 2 "0")) ":"
    (-> t .getSeconds .toString (.padStart 2 "0"))))


(defn reset-app-state! [ state ]
  (let [gameid (->> state :games (reduce-kv (fn [m k v] (if (contains? (:plyrs v) @uname) (assoc m :gid k) m)) {:gid nil}) :gid)]
    (reset! gid gameid)
    (reset! gm (if (some? gameid) (-> state :games gameid) nil))
    (reset! app state)))