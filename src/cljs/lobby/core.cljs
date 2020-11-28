(ns lobby.core
  (:require 
    [reagent.core :as r]
    [lobby.view :refer [main]]))
  
(r/render [main] (.getElementById js/document "app"))

; core  -> view
; view  -> raview [ramain]
;       -> daview [damain]
;       -> comms
;         -> model
; comms -> model