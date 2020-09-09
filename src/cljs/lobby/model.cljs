(ns lobby.model
	(:require [reagent.core :as r]))

(def app (r/atom {}))

(def pinglog (r/atom nil))

