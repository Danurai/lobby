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
			(assoc-in [:public :mage] (->> @data :mages (filter #(= n (:fg %))) (map #(assoc % :uid (gensym "mage"))) first))
			(assoc-in [:private :artifacts] (->> @data :artifacts (filter #(= n (:fg %))) (map #(assoc % :uid (gensym "art"))) ))
			(assoc-in [:secret :artifacts] (take 5 (nthrest (->> @data :artifacts (remove :fg) (map #(assoc % :uid (gensym "art")))) (* n 5) )))
			(assoc-in [:public :essence] {:gold 99 :calm 99 :elan 99 :life 99 :death 99})
			(assoc :action (if (= n 1 ) :play :waiting))
		))
(def game1 
	(let [monuments (mapv #(assoc % :uid (gensym "mon")) (:monuments @data))
				pops			(mapv #(assoc % :uid (gensym "pop")) (:placesofpower @data))]
		(assoc gs
			:status :play 
			:phase  :action
			:pops   (filter :fg? pops)
			:monuments (hash-map :public (take 2 monuments) :secret (nthrest monuments 2))
			:plyr-to ["p1" "AI123"] 
			:display-to ["p1" "AI123"] 
			:players (hash-map "p1" (setplayerdata 1) "AI123" (setplayerdata 2))
			:magicitems (map #(assoc (case (:id %) 1 (assoc % :owner "p1") 2 (assoc % :owner "AI123") %) :uid (gensym "mi")) (:magicitems @data) )
			:chat [{:uname "p1" :msg "Swap to predefined Game1" :timestamp (new java.util.Date)}]
			:allcards get-all-cards
			)))

(def game2 
	(-> game1
			(assoc :magicitems 
				(map 
					(fn [mi] 
						(cond (= (:owner mi) "p1")			(dissoc mi :owner)
									(= (:name mi) "Research") (assoc mi :owner "p1")
									:default 									mi))
					(:magicitems game1)))
			(assoc-in [:chat 0 :msg] "Swap to predefined Game2")))

(def game3
	(-> game1
			(assoc :magicitems 
				(map 
					(fn [mi] 
						(cond (= (:owner mi) "p1")			 (dissoc mi :owner)
									(= (:name mi) "Reanimate") (assoc mi :owner "p1")
									:default 									mi))
					(:magicitems game1)))
			(assoc :pops (->> @data :placesofpower (remove :fg?) (map #(assoc % :uid (gensym "pop")))))
			(assoc-in [:chat 0 :msg] "Swap to predefined Game3")))

(def game4
	(-> game1 
			(assoc :plyr-to ["p1" "p2"])
			(assoc :display-to ["p1" "p2"])
			(assoc-in [:players "p2"] (-> game1 :players (get "AI123")))
			(assoc :magicitems (map #(if (= (:owner %) "AI123") (assoc % :owner "p2") %) (:magicitems game1)))
			(assoc :pops (->> @data :placesofpower (remove :fg?) (map #(assoc % :uid (gensym "pop")))))
			(update-in [:players] dissoc "AI123")
			(assoc-in [:chat 0 :msg] "Swap to predefined Game4")))

(def game5 
	game4)