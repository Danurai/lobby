(ns lobby.users
  (:require [cemerick.friend.credentials :refer (hash-bcrypt)]))


(def users (atom {
	"dan" {
		:uid 1001
		:username "dan"
		:password (hash-bcrypt "user")
    :pin "1234" ;; only used by multi-factor
    :roles #{::user}}
	"p1" {
		:uid 1011
		:username "p1"
		:password (hash-bcrypt "user")
    :pin "1234" ;; only used by multi-factor
    :roles #{::user}}
	"p2" {
		:uid 1012
		:username "p2"
		:password (hash-bcrypt "user")
    :pin "1234" ;; only used by multi-factor
    :roles #{::user}}
	"andy" {
		:uid 1002
		:username "andy"
		:password (hash-bcrypt "user")
    :pin "1234" ;; only used by multi-factor
    :roles #{::user}}
	"matt" {
		:uid 1003
		:username "matt"
		:password (hash-bcrypt "user")
    :pin "1234" ;; only used by multi-factor
    :roles #{::user}}
	"julio" {
		:uid 1004
		:username "julio"
		:password (hash-bcrypt "user")
    :pin "1234" ;; only used by multi-factor
    :roles #{::user}}
  "root" {
		:uid 1000
		:username "root"
		:password (hash-bcrypt "admin")
		:pin "1234" ;; only used by multi-factor
		:roles #{::admin}}}))

(derive ::admin ::user)