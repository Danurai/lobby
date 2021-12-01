(ns lobby.pages
  (:require 
    [lobby.radata :as radata]
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
      :href "https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/css/bootstrap.min.css" 
      :integrity "sha384-EVSTQN3/azprG1Anm3QDgpJLIm9Nao0Yz1ztcQTwFspd3yD65VohhpuuCOmLASjC" 
      :crossorigin "anonymous"}]
    [:script {
      :src "https://code.jquery.com/jquery-3.5.1.slim.min.js" 
      :integrity "sha384-DfXdz2htPH0lsSSs5nCTpuj/zy4C+OGpamoFVy38MVBnE+IbbVYUew+OrCXaRkfj" 
      :crossorigin "anonymous"}]
    [:script {
      :src "https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/js/bootstrap.min.js" 
      :integrity "sha384-cVKIPhGWiC2Al4u+LWgxfKTRIcfu0JTxR+EQDz/bgldoEyl4H0zUF0QKbrJ0EcQF" 
      :crossorigin "anonymous"}]
    [:script {
      :src "https://kit.fontawesome.com/3e3abf4a33.js"
      :crossorigin "anonymous"}]
    [:link {:rel "preconnect" :href "https://fonts.googleapis.com"}]
    [:link {:rel "preconnect" :href "https://fonts.gstatic.com" :crossorigin "true"}]
    [:link {:href "https://fonts.googleapis.com/css2?family=Orbitron&family=Pirata+One&display=swap" :rel "stylesheet"}]
    (h/include-css "/css/ra.css")
    (h/include-css "/css/bb.css")
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
		[:button.btn.btn-primary.mt-2 {:role "submit"} "Login"]])


(defn navbar [ req ]
	[:nav#navbar.navbar.navbar-expand-sm.navbar-dark.bg-dark.text-white.py-0
    [:div.container
      [:button.navbar-toggler {:type "button" :data-bs-toggle "collapse" :data-target "#navbarNav"}
        [:span.navbar-toggler-icon]]
      [:div#navbarNav.collapse.navbar-collapse
        [:a.navbar-brand {:href "/play"} "Play"]
        [:div.ms-auto
          (if-let [identity (friend/identity req)] 
            [:div [:span "Logged in as "][:span#loginname.me-2 (:current identity)][:a.btn.btn-sm.btn-danger.py-=0 {:href "/logout" :title "Logout"} [:i.fa.fa-times]]]
            "Not logged in")]]]])

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
      [:div#app 
        [:div.d-flex.my-3
          [:div.loader.mx-auto]]]]
    (h/include-css "/css/lobby.css?v=0.1")
    (h/include-js "/js/compiled/lobby.js")))
    
		
(defn testpage [ req ]
  (let [q (-> req :query-string)
        artifacts (-> @radata/data :artifacts)]
    (h/html5
      header
      [:body
        (navbar req)
        ;; Res Arcana Card Reference
        [:div.container
          [:div.d-flex.mb-2
            [:div.btn-group 
              (for [ k ["cost" "subtype" "collect" "action"]]
                [:a {:href (str "test?" k) :role "button" :class (str "btn btn-outline-secondary" (if (= q k) " active" ""))} k]) ]]
          [:div.row
            (for [a artifacts] ;(sort-by :fg artifacts)]
              [:div.col-2 
                [:div.d-flex.justify-content-between 
                  [:b  (:id a) " " (:name a)]
                  [:b (:fg a)]]
                [:img.img-fluid {:src (str "/img/ra/" (:type a) "-" (:id a) ".jpg")}]
                (if q 
                    [:small q " " (str (get a (keyword q))) ]
                    (for [k (select-keys a [:cost :subtype :collect :action])]
                          (if (some? k) [:small (str k)])))])]]])))