(ns lobby.web
  (:require
    [compojure.core :refer [defroutes GET POST ANY context]]
    [compojure.route :refer [not-found resources]]
		[hiccup.page :as h]
    (ring.middleware
      [defaults :refer :all]
      [session :refer [wrap-session]]
      [params :refer [wrap-params]]
      [keyword-params :refer [wrap-keyword-params]]
      [anti-forgery :refer [wrap-anti-forgery]])
		[ring.util.response :refer [response resource-response content-type redirect]]

    [cemerick.friend :as friend]
    (cemerick.friend 
			[workflows :as workflows]
			[credentials :as creds])
    [taoensso.sente :as sente]
    [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter sente-web-server-adapter]]
    [lobby.pages :as pages]
		[lobby.users :as users :refer [users]]
		[lobby.model :as model]
		))   
        
; sente
(let [{:keys [ch-recv 
              send-fn
              connected-uids
              ajax-post-fn
              ajax-get-or-ws-handshake-fn]}
     (sente/make-channel-socket! (get-sch-adapter) {:user-id-fn (fn [ring-req] (:client-id ring-req))})]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake   ajax-get-or-ws-handshake-fn)
  (def ch-chsk                      ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                   send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
)
    
(defroutes app-routes
;sente
  (GET  "/chsk"    [] ring-ajax-get-or-ws-handshake) ; ((:ajax-get-or-ws-handshake-fn channel-socket) req))
  (POST "/chsk"    [] ring-ajax-post)
;standard
  (GET "/" 		  [] (redirect "/play"))
	(GET "/login" [] pages/login)
	(GET "/play"  [] 
		;pages/lobby)
		(friend/wrap-authorize pages/lobby #{::users/user}))
  (friend/logout 
		(ANY "/logout" [] (redirect "/play")))
  (resources "/"))
   
(def friend-authentication-hash {
  :allow-anon? true
  :login-uri "/login"
  :default-landing-uri "/"
  :unauthorised-handler (h/html5 [:body [:div.h5 "Access Denied " [:a {:href "/"} "Home"]]])
  :credential-fn #(creds/bcrypt-credential-fn @users %)
  :workflows [(workflows/interactive-form)]})
	
(def app 
  (-> app-routes
    (wrap-anti-forgery)
    (friend/authenticate friend-authentication-hash)
    (wrap-keyword-params)
    (wrap-params)
    (wrap-session)
    ))
    
    
; Sente broadcast
(defn broadcast []
  (doseq [uid (:any @connected-uids)]
    (chsk-send! uid [:lobby/appstate @model/appstate])))
        
;; multi to handle Sente 'events'
(defmulti event :id)
;
;;; default for no other matching handler
(defmethod event :default [{:as ev-msg :keys [event]}]
  (println "Unhandled event: " event))

(defmethod event :chsk/ws-ping      [_])

;TEST
(defmethod event :lobby/getstate [{:as ev-msg :keys [?reply-fn]}]
  (when ?reply-fn
    (?reply-fn @model/appstate)))
;TEST

(defmethod event :lobby/create [{:keys [?data uid ring-req]}]
 	(when-let [user (-> ring-req friend/identity :current)]
    (model/creategame user ?data)
    (broadcast)))
    
(defmethod event :lobby/leave [{:keys [?data ring-req]}]
 	(when-let [user (-> ring-req friend/identity :current)]
    (model/leavegame user ?data)
    (broadcast)))

(defmethod event :lobby/join [{:keys [?data ring-req]}]
 	(when-let [user (-> ring-req friend/identity :current)]
    (model/joingame user ?data)
    (broadcast)))
    
(defmethod event :lobby/start [{:keys [?data ring-req]}]
 	(when-let [user (-> ring-req friend/identity :current)]
    (model/startgame ?data)
    (broadcast)))
    
; Connection Management
(defmethod event :chsk/uidport-open [{:as ev-msg :keys [ring-req uid]}] 
	(when-let [user (-> ring-req friend/identity :current)]
    (swap! model/appstate assoc-in [:user-hash user] uid)
		(broadcast)))
    
(defmethod event :chsk/uidport-close [{:as ev-msg :keys [ring-req uid]}]
  (when-let [u (->> @model/appstate :user-hash (filter #(= (val %) uid)) first)]
    (swap! model/appstate update-in [:user-hash] dissoc (key u))
    (broadcast)))
    
    
; Sente event router ('event' loop)
(defonce router
  (sente/start-chsk-router! ch-chsk event))