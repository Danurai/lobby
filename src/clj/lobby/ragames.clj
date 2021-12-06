(ns lobby.ragames
  (:require 
    [lobby.radata :refer [data]]))

(def gs {
	:round 1
	:status :setup
	:pops []
	:monuments {}
	:magicitems (:magicitems @data)
	:players {}
	:plyr-to []
	:pass-to []
	:chat []})
	
(def playerdata {
	:public {
		:discard []
		:artifacts []
		:essence {
			:gold 1
			:calm 1
			:elan 1
			:life 1
			:death 1
		}
		:vp 0
	}
	:private { ; player knows
	}
	:secret { ; no-one knows
		:artifacts []}})


(def get-all-cards 
	(->>  (select-keys @data [:placesofpower :mages :magicitems :artifacts :monuments])
				vals
				(reduce concat)
				(map #(select-keys % [:id :type :name]))
				(sort-by #(-> % :name count) #(> %1 %2))))

(defn- setplayerdata [ n ]
	(-> playerdata
			(assoc-in [:public :mage] (->> @data :mages (filter #(= n (:fg %))) first))
			(assoc-in [:private :artifacts] (->> @data :artifacts (filter #(= n (:fg %))) (map #(assoc % :uid (gensym "art"))) ))
			(assoc-in [:secret :artifacts] (take 5 (nthrest (->> @data :artifacts (remove :fg)) (* n 5) )))
			(assoc-in [:public :essence] {:gold 99 :calm 99 :elan 99 :life 99 :death 99})
			(assoc :action (if (= n 1 ) :play :waiting))
		))
(def game1 
	(assoc gs
		:status :play 
		:phase  :action
		:pops   (->> @data :placesofpower (filter :fg?))
		:monuments (hash-map :public (->> @data :monuments (take 2)) :secret (-> @data :monuments (nthrest 2)))
		:plyr-to ["dan" "AI123"] 
		:display-to ["dan" "AI123"] 
		:players (hash-map "dan" (setplayerdata 1) "AI123" (setplayerdata 2))
		:magicitems (map #(assoc (case (:id %) 1 (assoc % :owner "dan") 2 (assoc % :owner "AI123") %) :uid (gensym "mi")) (:magicitems @data) )
		:chat [{:uname "dan" :msg "Swap to predefined Game1" :timestamp (new java.util.Date)}]
		:allcards get-all-cards
		))

(def game2 
	(-> game1
			(assoc :magicitems 
				(map 
					(fn [mi] 
						(cond (= (:owner mi) "dan")			(dissoc mi :owner)
									(= (:name mi) "Research") (assoc mi :owner "dan")
									:default 									mi))
					(:magicitems game1)))
			(assoc-in [:chat 0 :msg] "Swap to predefined Game2")))