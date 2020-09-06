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
    
;;;; Sente channel-socket            
(declare channel-socket)

(defn start-websocket []
  (defonce channel-socket
    (sente/make-channel-socket! (get-sch-adapter) {:user-id-fn (fn [ring-req] (:client-id ring-req))})))
    
    
(defroutes app-routes
;sente
  (GET  "/chsk"    req ((:ajax-get-or-ws-handshake-fn channel-socket) req))
  (POST "/chsk"    req ((:ajax-post-fn channel-socket) req))
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
    (friend/authenticate friend-authentication-hash)
    (wrap-keyword-params)
    (wrap-params)
    (wrap-session)
    ;(wrap-anti-forgery)
    ))
    
    
;; multi to handle Sente 'events'
(defmulti event :id)
;
;;; default for no other matching handler
(defmethod event :default [{:as ev-msg :keys [event]}]
  (println "Unhandled event: " event))

(defmethod event :chsk/ws-ping      [_])

(defmethod event :chsk/uidport-open [{:as ev-msg :keys [ring-req uid]}] 
	(when-let [user (-> ring-req friend/identity :current)]
		(swap! model/appstate assoc-in [:user-hash user] uid)
		(prn (:user-hash @model/appstate))
		))

(defmethod event :lobby/check [{:as ev-msg :keys [event ?data ?reply-fn]}]
	(prn ev-msg)
	(when ?reply-fn
		(?reply-fn {:data "From the server"})))

;;(defmethod event :gkv2/data [{:as ev-msg :keys [event ?data ?reply-fn]}]
;  (when ?reply-fn
;    (?reply-fn (model/get-data))))

; Sente event router ('event' loop)
(defn start-router []
  (defonce router
    (sente/start-chsk-router! (:ch-recv channel-socket) event)))
    
;; Initalisation
(start-websocket)
(start-router)  