(ns lobby.comms
  (:require 
    [taoensso.sente :as sente :refer [cb-success?]]
		[lobby.model :as model :refer [gid uname]]))

(defonce channel-socket
  (sente/make-channel-socket! "/chsk" {:type :auto :ws-kalive-ms 20000}))
(defonce chsk (:chsk channel-socket))
(defonce ch-chsk (:ch-recv channel-socket))
(defonce chsk-send! (:send-fn channel-socket))
(defonce chsk-state (:state channel-socket))

; (prn (:csrf-token @chsk-state))

(defn reset []
  (chsk-send! [:lobby/reset] 5000 nil))

; Lobby
              
(defn creategame [ ?data ]
  (chsk-send! [:lobby/create ?data] 5000 nil)) 
(defn joingame [ gid ]
  (chsk-send! [:lobby/join gid] 5000 nil))
(defn addai [ gid ]
  (chsk-send! [:lobby/addai gid] 5000 nil))
(defn removeai [ gid uname ]
  (chsk-send! [:lobby/removeai {:gid gid :pname uname}] 5000 nil))
(defn leavegame [ gid ]
  (chsk-send! [:lobby/leave gid] 5000 nil))
(defn startgame [ gid ]
  (chsk-send! [:lobby/start gid] 5000 nil))

(defn sendmsg! 
  ([ msg gid ]
    (chsk-send! [:lobby/chat {:msg msg :gid gid}] 5000 nil))
  ([ msg ]
    (sendmsg! msg nil)))
  
; RA Send
  
(defn ra-send! [ ?data ]
  (prn ?data)
  (chsk-send! [:lobby/game-action (assoc ?data :gid @gid)] 5000 nil))

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
  (let [chan (first ?data) state (second ?data)]
    ;(prn ?data)
    (case chan
      :lobby/appstate (model/reset-app-state! state)
      (prn "unhandled" chan)
      )))

(defmethod event-msg-handler :chsk/handshake [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (println "Handshake" ?data)))
    
;;;; Sente event router ('event-msg-handler' loop)
(defonce router
  (sente/start-client-chsk-router! ch-chsk event-msg-handler))