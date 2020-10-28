(ns lobby.model
	(:require [reagent.core :as r]))

(def app (r/atom {}))


(defn timeformat [ t ]
  (str 
    (-> t .getHours   .toString (.padStart 2 "0")) ":"
    (-> t .getMinutes .toString (.padStart 2 "0")) ":"
    (-> t .getSeconds .toString (.padStart 2 "0"))))
