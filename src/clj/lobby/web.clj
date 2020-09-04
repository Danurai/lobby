(ns lobby.web
  (:require
    [compojure.core :refer [defroutes GET POST ANY context]]
    [compojure.route :refer [not-found resources]]
    (ring.middleware
      [defaults :refer :all]
      [session :refer [wrap-session]]
      [params :refer [wrap-params]]
      [keyword-params :refer [wrap-keyword-params]]
      [anti-forgery :refer [wrap-anti-forgery]])
    [taoensso.sente :as sente]
    [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter sente-web-server-adapter]]
    [lobby.pages :as pages]))   
    
;[hiccup.page :as h]
; [ring.util.response :refer [response resource-response content-type redirect]]

;;;; Sente channel-socket            
(declare channel-socket)

(defn start-websocket []
  (defonce channel-socket
    (sente/make-channel-socket! (get-sch-adapter) )))
    
    
(defroutes app-routes
;sente
  (GET  "/chsk"    req ((:ajax-get-or-ws-handshake-fn channel-socket) req))
  (POST "/chsk"    req ((:ajax-post-fn channel-socket) req))
;standard
  (GET "/" [] pages/home)
  (resources "/"))
   
(def app 
  (-> app-routes
    (wrap-keyword-params)
    (wrap-params)
    (wrap-anti-forgery)
    (wrap-session)
    ))
    
    
;; multi to handle Sente 'events'
(defmulti event :id)
;
;;; default for no other matching handler
(defmethod event :default [{:as ev-msg :keys [event]}]
  (println "Unhandled event: " event))

(defmethod event :chsk/ws-ping      [_])

;;(defmethod event :gkv2/data [{:as ev-msg :keys [event ?data ?reply-fn]}]
;  (when ?reply-fn
;    (?reply-fn (model/get-data))))
;    
;(defmethod event :gkv2/apply-progress [{:as ev-msg :keys [event ?data ?reply-fn]}]
;  (when ?reply-fn
;    (?reply-fn (model/apply-progress ?data))))
;    
;(defmethod event :gkv2/end-of-day [{:as ev-msg :keys [event ?data ?reply-fn]}]
;  (when ?reply-fn
;    (?reply-fn (model/end-of-day ?data))))

; Sente event router ('event' loop)
(defn start-router []
  (defonce router
    (sente/start-chsk-router! (:ch-recv channel-socket) event)))
    
;; Initalisation
(start-websocket)
(start-router)  