(ns lobby.comms
  (:require 
    [taoensso.sente :as sente]))

(defonce channel-socket
  (sente/make-channel-socket! "/chsk" {:type :auto :ws-kalive-ms 20000}))
(defonce chsk (:chsk channel-socket))
(defonce ch-chsk (:ch-recv channel-socket))
(defonce chsk-send! (:send-fn channel-socket))
(defonce chsk-state (:state channel-socket))

;;;; Sente send functions

(defn load-data! []
  (chsk-send! [:gkv2/data] 
              5000
              (fn [cb-reply] 
                 (prn cb-reply))))
                 
;;;; Sente event handlers

(defmulti event-msg-handler :id)

(defmethod event-msg-handler :default [_])

(defmethod event-msg-handler :chsk/handshake [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (println "Handshake:" ?data)
    (load-data!)))
    
;;;; Sente event router ('event-msg-handler' loop)
(defonce router
  (sente/start-client-chsk-router! ch-chsk event-msg-handler))