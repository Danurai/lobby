(ns lobby.pages
  (:require 
    [hiccup.page :as h]))
    
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
      :crossorigin "anonymous"}]])
  
                  
(defn home [ req ]
  (h/html5 
    header
    [:body 
      [:div.container.my-3
        [:div.h4 "Clojure Generated"]
        [:div#app.h5 [:i "Not Generated"]]]]
    (h/include-js "/js/compiled/lobby.js")))
    