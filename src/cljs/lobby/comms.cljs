(ns lobby.comms
  (:require 
    [taoensso.sente :as sente :refer [cb-success?]]
		[lobby.model :as model]
    [lobby.ramodel :as ra :refer [gamestate]]))

(defonce channel-socket
  (sente/make-channel-socket! "/chsk" {:type :auto :ws-kalive-ms 20000}))
(defonce chsk (:chsk channel-socket))
(defonce ch-chsk (:ch-recv channel-socket))
(defonce chsk-send! (:send-fn channel-socket))
(defonce chsk-state (:state channel-socket))

; (prn (:csrf-token @chsk-state))

; Lobby
              
(defn creategame [ ?data ]
  (chsk-send! [:lobby/create ?data] 5000 nil)) 
(defn joingame [ gid ]
  (chsk-send! [:lobby/join gid] 5000 nil))
(defn leavegame [ gid ]
  (chsk-send! [:lobby/leave gid] 5000 nil))
(defn startgame [ gname gid ]
  (chsk-send! [:lobby/start {:gid gid :gname gname}] 5000 nil))


(defn ra-send [ ?data ]
  (chsk-send! [:lobby/ra-action ?data] 5000 nil))

;;;; Sente send functions

;(defn load-data! []
;  (chsk-send! [:gkv2/data] 
;              5000
;              (fn [cb-reply] 
;                 (prn cb-reply))))
                 
;;;; Sente event handlers

(defmulti event-msg-handler :id)

(defmethod event-msg-handler :default [_])

; Receiver for server (chsk-send! uid [message]

(defmethod event-msg-handler :chsk/recv [{:as ev-msg :keys [?data]}]
  (let [chan (first ?data)]
    ;(prn "chsk/recv" ?data)
    (case chan
      :lobby/appstate (reset! model/app (second ?data))
      :lobby/ragame   (reset! ra/gamestate (second ?data))
      (prn "unhandled" chan)
      )))

(defmethod event-msg-handler :chsk/handshake [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (println "Handshake" ?data)))
    
;;;; Sente event router ('event-msg-handler' loop)
(defonce router
  (sente/start-client-chsk-router! ch-chsk event-msg-handler))