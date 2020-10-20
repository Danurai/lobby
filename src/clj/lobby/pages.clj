(ns lobby.pages
  (:require 
    [hiccup.page :as h]
    [cemerick.friend :as friend]
    [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
    [ring.util.anti-forgery :refer [anti-forgery-field]]))
    
(def header 
  [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:link {
      :rel "stylesheet" 
      :href "https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css" 
      :integrity"sha384-JcKb8q3iqJ61gNV9KGb8thSsNjpSL0n8PARn9HuZOnIxN0hoP+VmmDGMN5t9UJ0Z" 
      :crossorigin "anonymous"}]
    [:script {
      :src "https://code.jquery.com/jquery-3.5.1.slim.min.js" 
      :integrity "sha384-DfXdz2htPH0lsSSs5nCTpuj/zy4C+OGpamoFVy38MVBnE+IbbVYUew+OrCXaRkfj" 
      :crossorigin "anonymous"}]
    [:script {
      :src "https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/js/bootstrap.min.js" 
      :integrity "sha384-B4gt1jrGC7Jh4AgTPSdUtOBvfO8shuf57BaghqFfPlYxofvL8/KUEfYiJOMMV+rV" 
      :crossorigin "anonymous"}]
    [:script {
      :src "https://kit.fontawesome.com/3e3abf4a33.js"
      :crossorigin "anonymous"}]
    (h/include-css "/css/ra.css")
  ])
			
(defn loginform []
	[:form {:method "post" :action "/login"}
		[:div.form-group
			[:label {:for "username"} "User Name"]
			[:input#username.form-control {:name "username" :type "text" :autofocus true}]]
		[:div.form-group	
			[:label {:for "password"} "Password"]
			[:input#password.form-control {:name "password" :type "password"}]]
    (anti-forgery-field)
		[:button.btn-primary {:role "submit"} "Login"]])


(defn navbar [ req ]
	[:nav#navbar.navbar.navbar-expand-sm.navbar-dark.bg-dark.py-0
		[:a.navbar-brand {:href "#"} "Portal"]
		[:button.navbar-toggler {:type "button" :data-toggle "collapse" :data-target "#navbarNav"}
			[:span.navbar-toggler-icon]]
		[:div#navbarNav.collapse.navbar-collapse
			[:a.nav-link.active.text-white {:href "/play"} "Play"]
			[:span.ml-auto.text-white 
				(if-let [identity (friend/identity req)] 
					[:div [:span "Logged in as "][:span#loginname.mr-2 (:current identity)][:a.btn.btn-sm.btn-danger.py-=0 {:href "/logout" :title "Logout"} [:i.fa.fa-times]]]
					"Not logged in")]]])

(defn login [ req ]
	(h/html5
    header
		[:body
			(navbar req)
			[:div.container.my-3
				[:div.row
					[:div.col-sm-6.offset-3
						(loginform)]]]]))
                  
(defn lobby [ req ]
  (h/html5 
    header
    ;(into header [[:meta {:name "csrf-token" :content *anti-forgery-token*}]])
    [:body 
			(navbar req)
      [:div#app "No app connection"]]
    (h/include-js "/js/compiled/lobby.js")))
    
		
		