(ns lobby.comms
  (:require 
    [taoensso.sente :as sente :refer [cb-success?]]
		[lobby.model :as model]))

(defonce channel-socket
  (sente/make-channel-socket! "/chsk" {:type :auto :ws-kalive-ms 20000}))
(defonce chsk (:chsk channel-socket))
(defonce ch-chsk (:ch-recv channel-socket))
(defonce chsk-send! (:send-fn channel-socket))
(defonce chsk-state (:state channel-socket))

; (prn (:csrf-token @chsk-state))

(defn sendone []
	(chsk-send! [:lobby/check]
							5000
							(fn [cb-reply] (prn cb-reply) (reset! model/app cb-reply))))

;;;; Sente send functions

;(defn load-data! []
;  (chsk-send! [:gkv2/data] 
;              5000
;              (fn [cb-reply] 
;                 (prn cb-reply))))
                 
;;;; Sente event handlers

(defmulti event-msg-handler :id)

(defmethod event-msg-handler :default [_])

(defmethod event-msg-handler :chsk/recv [{:as ev-msg :keys [?data]}]
  (println ?data)
	(if (= :lobby/appstate (first ?data))
			(reset! model/app (second ?data))))

(defmethod event-msg-handler :chsk/handshake [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (println "Handshake:" ?data)
    ;(chsk-send! [:lobby/getstate] 5000 (fn [cb-reply] (prn cb-reply) (reset! model/app cb-reply)))
    ))
    
;;;; Sente event router ('event-msg-handler' loop)
(defonce router
  (sente/start-client-chsk-router! ch-chsk event-msg-handler))