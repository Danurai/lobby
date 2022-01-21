(ns lobby.test
	(:require 
		[reagent.core :as r]
		[lobby.comms :as comms]
		[lobby.raessence :refer [action-bar lose-life-svg place-cost-svg]]))

(defonce radata (r/atom nil))

(defn main []
	[:div.container.py-3 
		;[:style ".img-ab {height: 40px;} .text-ab {font-size: 13pt; font-family: \"Pirata One\", cursive; margin-top: auto; margin-bottom: auto; line-height: 1.1rem; white-space: pre;} .text-ab-lg {font-size: 1.3rem; font-weight: bold;}"]
		[:div [:button.btn.btn-outline-secondary {:on-click #(comms/load-data! radata)} "Load Data"] ]
		[:div 
			;[:div.bg-secondary.p-2
			;	(lose-life-svg "?" {:size :lg})
			;	(place-cost-svg "?" {:size :lg})]
			;[:div (str @radata)]
			(for [[k v] @radata]
				[:div  {:key (gensym)}
					[:h3 (name k)]
					(if (contains? #{:placesofpower :artifacts} k) ; :mages y  :magicitems y  :artifacts x  :monuments 1  :placesofpower 2
						[:div.row 
							(for [c v]
								[:div.col-sm-4.mb-3 {:key (gensym) :style {:display "flex" :flex-flow "column"}}
									[:img.img-fluid {:src (str "/img/ra/" (:type c) "-" (:id c) ".jpg")}]
									(for [a (:action c)] (action-bar a))
								])])])]])

(r/render [main] (.getElementById js/document "app"))
(defonce start 
	(comms/load-data! radata))