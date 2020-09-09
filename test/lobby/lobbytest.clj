(ns lobby.lobbytest
  (:require 
    [expectations :refer :all]
    [lobby.model :as model]))

(expect {:owner "dan" :game "RA" :title "Game"}
  (select-keys
    (-> (model/creategame "dan" {:game "RA" :title "Game"})
        :games
        last)
    [:owner :game :title]))

(expect 2
  (do 
    (swap! model/appstate assoc :games [])
    (model/creategame "dan" {:game "RA" :title "Game"})
    (model/creategame "andy" {:game "RA" :title "Game"})
    (-> @model/appstate :games count)))
    
(expect 1
  (do 
    (swap! model/appstate assoc :games [])
    (model/creategame "dan" {:game "RA" :title "Game"})
    (model/creategame "dan" {:game "RA" :title "Game"})
    (->> @model/appstate :games count)))
    
(expect 1 1)
