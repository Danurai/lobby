(ns lobby.raessence)

(def essence-icon-hash {
	:elan {
		:path "m2.3 13.5c-0.46762-0.35517-1.1188-2.5348-1.2805-4.2864-0.23854-2.5831 1.3432-5.337 4.3592-7.5898 1.3504-1.0086 1.8042-0.95142 1.4608 0.18418-0.33387 1.1038-0.13436 2.3267 0.46859 2.8723 0.31296 0.28323 0.53169 0.19393 1.0233-0.41779 0.25793-0.32095 0.49841-0.49285 0.68951-0.49285 0.36901 0 1.4719 0.97559 1.7389 1.5381 0.17936 0.37797 0.17954 0.48402 0.0019 1.1598-0.31873 1.2124-0.18092 2.133 0.52926 3.5354 0.72846 1.4386 0.83864 2.1918 0.44933 3.072l-0.25724 0.58153-8.9375-2.7e-4z"
		:path-fill   "#ff2020"
		:font-stroke "#800000"
		:y "68%" 
	}
	:life {
		:path "m2 13.5c-0.20359-0.25883-0.7461-2.0351-1.0217-3.345-0.06878-0.32698-0.10276-0.99465-0.07551-1.4837 0.08844-1.5871 0.57384-2.3082 2.3899-3.5503 0.52977-0.36233 1.0211-0.79358 1.1418-1.0021 0.30149-0.52097 0.36562-1.6844 0.13632-2.4731-0.19127-0.65793-0.1127-1.0624 0.20639-1.0624 0.08456 0 0.51141 0.26907 0.94856 0.59793 0.66119 0.4974 0.82473 0.57593 0.97274 0.46709 0.09786-0.0719 0.45813-0.34512 0.80061-0.60703 0.34247-0.2619 0.71167-0.45922 0.82044-0.43849 0.25305 0.0483 0.31058 0.45299 0.14361 1.0103-0.16055 0.53586-0.1585 1.7972 0.0036 2.225 0.17271 0.45578 0.4793 0.74755 1.4973 1.4249 1.7041 1.1339 2.1749 1.9572 2.163 3.7829-0.0074 1.1391-0.47952 3.2146-0.94995 4.1763l-0.2419 0.49457-8.7194 0.0574z"
		:path-fill   "#20dd20"
		:font-stroke "#008000"
		:y "68%" 
	}
	:calm {
		:path "m2.4 13.7c-0.56849-0.2283-1.6032-3.2924-1.5995-4.7366 0.0043-1.6528 0.60611-2.5134 2.4762-3.5406 1.7619-0.96782 1.8147-1.0079 2.0708-1.572 0.30875-0.68015 0.31828-1.3526 0.03203-2.2607-0.21389-0.67855-0.2131-0.70962 0.02255-0.88192 0.21406-0.15654 0.3162-0.13841 0.8263 0.14665 2.6708 1.4927 5.0595 4.0781 5.6601 6.1259 0.56904 1.9404-0.69034 5.9852-1.4552 6.7294l-8.0332-0.0107z"
		:path-fill   "#00aaee"
		:font-stroke "#005a80"
		:y "68%" 
	}
	:death {
		:path "m3.3 13.45c-0.14568-0.14567-0.18034-0.33694-0.12441-0.6867 0.21087-1.3187-0.56157-2.8854-1.7476-3.5446-0.64284-0.35727-0.72171-0.4503-0.8741-1.031-0.10547-0.40188-0.14902-1.1247-0.11758-1.9517 0.04575-1.2037 0.0895-1.3935 0.52367-2.272 0.31401-0.63535 0.67857-1.1389 1.0811-1.4934 1.1178-0.98427 2.7681-1.7978 4.0314-1.9872 1.3357-0.20029 3.6427 0.77657 5.0038 2.1187 1.1228 1.1073 1.4237 1.8582 1.4933 3.7268 0.06919 1.8584-0.10094 2.3997-0.88883 2.8279-0.82908 0.45063-1.0829 0.70921-1.4672 1.4947-0.27093 0.55376-0.33493 0.86997-0.33493 1.6548 0 0.67946-0.05624 1.0265-0.18761 1.1579-0.28921 0.2892-6.1005 0.27618-6.391-0.0137z"
		:path-fill	 "#666666"
		:font-stroke "#000000"
		:y "60%" 
	}
	:gold {
		:path "m3.5 13.7c-0.21633-0.0956-0.48992-0.28494-0.60798-0.42098-0.3568-0.41117-2.0844-3.581-2.1613-3.9656-0.09449-0.47248 2.2109-6.3651 2.6451-6.7609 0.16401-0.14959 1.1209-0.69528 2.1265-1.2128 1.6924-0.87103 1.8654-0.93257 2.3284-0.82815 0.92937 0.20961 1.0787 0.42816 2.7753 4.061 0.88711 1.8996 1.6129 3.5796 1.6129 3.7333 0 0.42693-1.1654 3.2427-1.4865 3.5916-0.49827 0.54134-1.8823 1.5898-2.3648 1.7914l-4.8675 0.0108z"
		:path-fill	 "#ebeb00"
		:font-stroke "#bc9010"
		:y "60%" 
	}
	:any {
		:path "m6.5,1.5 6.09157,4.63784 -2.314465,7.52199 -7.521989,0.011 -2.334379,-7.51518 z"
		:path-fill 	 "#ffffff"
		:font-stroke "#606060"
		:y "60%" 
	}})

(defn essence-svg 
	([ type val tags ]
		(let [n (if (number? val) (if (< -2 val 2) nil (Math.abs val)) val)
					size (case (:size tags) :lg 60 :sm 30 40)]
			[:svg.mt-auto {:key (gensym "esvg") :height (str (if (:exclude tags) (/ size 2) size) "px") :view-box "0 0 13 14.6"}
				[:defs 
					[:radialGradient {:id (str "grad" (name type) ) :cx "50%" :cy "50%" :r "50%"}
						[:stop {:offset "0%" 		:stop-color (-> essence-icon-hash type :path-fill)}]
						[:stop {:offset "100%" 	:stop-color (-> essence-icon-hash type :font-stroke)}]]
					[:radialGradient#gradx {:cx "0%" :cy "100%" :r "100%"}
						[:stop {:offset "0%"    :stop-color "black"}]
						[:stop {:offset "100%"  :stop-color "red"}]
					]]
				[:path {
					:d (-> essence-icon-hash type :path)
					:fill (str "url('#grad" (name type) "')")
					:stroke "white"
					:stroke-width "0.6px"
					:stroke-linejoin "round"
				}]
				[:g {:style {:font "8px Pirata One"}}
					[:text {:y "67%" :x "50%" :text-anchor "middle" :alignment-baseline "middle" :style {:fill "none" :stroke (-> essence-icon-hash type :font-stroke) :stroke-width "1.5px" }} n]
					[:text {:y "67%" :x "50%" :text-anchor "middle" :alignment-baseline "middle" :style {:fill "white"}} n]]
				(if (number? val)
					(if (< val 0)
						[:g {:style {:font ".5rem Pirata One"}}
							[:text {:y "97%" :x "3%" :style {:fill "none" :stroke "white" :stroke-width ".5px"}} "x"]
							[:text {:y "97%" :x "3%" :style {:fill "url('#gradx')"}} "x"]]
						))
				(if (:equal tags)
					[:g {:style {:font-size ".7rem"}}
						[:text {:y "18%" :x "97%" :style {:text-anchor "end" :alignment-baseline "middle" :fill "none" :stroke "white" :stroke-width ".75px"}} "="]
						[:text {:y "18%" :x "97%" :style {:text-anchor "end" :alignment-baseline "middle" :fill "#0080cc"}} "="]])
				(if (:exclude tags)
					[:g
						[:circle {:cx "2.8" :cy "11.5" :r "2.5" :style {:fill "url('#gradx')" :stroke "white" :stroke-width ".5px"}} ]
						[:path {:d "m1.1,11.5 3.4,0" :stroke "white" :stroke-width ".7px"}]
						])
				]))
	([ type val ] (essence-svg type val nil))
	([ type ] 		(essence-svg type nil nil)))

(defn lose-life-svg
	([ n tags ]
		(let [size (case (:size tags) :lg 300 :sm 30 40)]
			[:svg.mt-auto {:key (gensym "esvg") :height (str (if (:exclude tags) (/ size 2) size) "px") :view-box "0 0 13 15.7"}
				[:defs 
					[:radialGradient {:id (str "gradlife" ) :cx "50%" :cy "50%" :r "50%"}
						[:stop {:offset "0%" 		:stop-color (-> essence-icon-hash :life :path-fill)}]
						[:stop {:offset "100%" 	:stop-color (-> essence-icon-hash :life :font-stroke)}]]
					[:radialGradient#gradx {:cx "50%" :cy "50%" :r "100%"}
						[:stop {:offset "0%" :stop-color "red"}]
						[:stop {:offset "100%"  :stop-color "black"}]]]
				[:svg 
					[:path {
						:d (-> essence-icon-hash :life :path)
						:fill (str "url('#gradlife')")
						:stroke "#aa0000"
						:stroke-width "0.6px"
						:stroke-linejoin "round"}]
					[:g {:style {:font "8px Pirata One"}}
						[:text {:y "60%" :x "53%" :text-anchor "middle" :alignment-baseline "middle" :style {:fill "none" :stroke (-> essence-icon-hash :life :font-stroke) :stroke-width "1px" }} n]
						[:text {:y "60%" :x "53%" :text-anchor "middle" :alignment-baseline "middle" :style {:fill "white"}} n]]
					[:g 
						[:circle {:cx "2.6" :cy "13.1" :r "2" :fill "url('#gradx')" :stroke "white" :stroke-width ".3"}]
						[:circle {:cx "2.6" :cy "13.1" :r "0.2" :fill "white"}]
						[:path {:d "m2.6,10.5 0.4,0.6 -0.4,1.5 -0.4,-1.5" :fill "white"}]
						[:path {:d "m2.6,15.7 -0.4,-0.6 0.4,-1.5 0.4,1.5" :fill "white"}]
						[:path {:d "m0,13.1 0.6,0.4 1.5,-0.4 -1.5,-0.4" :fill "white"}]
						[:path {:d "m5.2,13.1 -0.6,0.4 -1.5,-0.4 1.5,-0.4" :fill "white"}]
						]
				]]))
	([ n ] (lose-life-svg n nil)))

(defn place-cost-svg 
	([ n tags ]
		(let [size (case (:size tags) :lg 300 :sm 30 40)]
			[:svg.mt-auto {:key (gensym "esvg") :height (str size "px") :view-box "0 0 7.1 10"}
				[:image {:href "/img/ra/rae/place_cost.png" :height "100%"}]
				[:g {:style {:font "5.5px Pirata One"}}
					[:text {:y "53%" :x "48%" :text-anchor "middle" :alignment-baseline "middle" :style {:fill "none" :stroke "white" :stroke-width "0.5px" }} n]
					[:text {:y "53%" :x "48%" :text-anchor "middle" :alignment-baseline "middle" :style {:fill "#683834"}} n]]
			]))
	([ n ] (place-cost-svg n nil)))

(defn invert-essences [ e ]
	(reduce-kv 
		(fn [m k v]
			(if (number? v)
					(assoc m k (- 0 v))
					v))
			{} e))

(defn text_svg [ txt ]
	[:svg.mt-auto {:key (gensym "psvg") :height "40px" :view-box "0 0 3.5 10"}
		[:g {:style {:font "8px Pirata One"}}
			[:text {:x "50%" :y "50%" :text-anchor "middle" :alignment-baseline "middle" :style {:stroke "white" :stroke-width ".25px"}} txt]
			[:text {:x "50%" :y "50%" :text-anchor "middle" :alignment-baseline "middle" :style {:fill "black"}} txt]]])
	
(defn- render-essence-list 
	([ essence-list tags ]
		[:div.d-flex.justify-content-center {:key (gensym "rel")}
			(rest (apply concat 
				(for [[k v] (dissoc essence-list :exclude)]
					[(if (:or tags) (text_svg "/") (text_svg "+")) (essence-svg k v)])))
			(if-let [excl (:exclude essence-list)]
				[:div.d-flex.mt-auto 
					[:span "("]
					(for [k excl]
						;(essence-svg k 1 {:exclude true}) Actual Code
						(essence-svg (keyword k) 1 {:exclude true}) ; handle json->clj translation of set
						)
					[:span ")"]])])
	([ essence-list ] (render-essence-list essence-list nil)))

(defonce essence-list [:calm :life :elan :death :gold])

(defn action-bar [ action ]
	(let [acount (->> [(:turn action) (-> action :cost some?) (:bought action) (-> action :destroy some?)] (filter true?) count)]
		[:div.d-flex.justify-content-center.py-2.rounded.mb-1 {:key (gensym "ab") :style {:background "grey"}}
			(if (:bought action) [:div.text-ab.me-1 "When bought"])
			(if (:react action)
				[:div.react
					(cond
						(= :loselife (:ignore action)) [:div.d-flex.mx-2 [:div.text-ab.text-center "React\nto"] (lose-life-svg "?")]
						(= :victory (:react action))   [:div.text-ab.text-ab-sm.text-center.me-1  "React to\nchecking\nvictory"])])
			(if (:turn action) [:img.img-ab {:src "/img/ra/rae/turn.png"}])
			(if (:turnextra action)
				[:div
					(if (:turn action) (text_svg "+"))
					[:img.img-ab {:src (str "/img/ra/rae/turn_" (-> action :turnextra :subtype clojure.string/lower-case) ".png")}]
				])
			(if (> acount 1) (text_svg "+"))
			(if-let [acost (:cost action)]
				(if-let [excl (:exclude acost)]
					(render-essence-list (invert-essences (zipmap (remove #(contains? excl %) essence-list) (repeat 1))) {:or true})
					(render-essence-list (invert-essences acost))))
			(if (:destroy action)
				[:div.d-flex 
					(cond
						(= :this (:destroy action)) [:div.d-flex.me-1 [:div.text-ab.me-1 "Destroy"] [:img.img-ab {:src "/img/ra/rae/place.png"}]]
						(= :anyartifact (:destroy action)) [:div.d-flex.me-1 [:div.text-ab.text-ab-sm "destroy " [:em "any\none "] "of your\nartifacts"]]
						(= :otherartifact (:destroy action)) [:div.d-flex.me-1 [:div.text-ab.text-ab-sm "destroy " [:em "\nanother "] "of\nyour artifacts"]]
						(set? (:destroy action)) [:div.d-flex (text_svg "+") [:div.text-ab.text-ab-sm.mx-1 (str "Destroy one of\nyour " (->> action :destroy (map #(str % "s")) (clojure.string/join "\nor ") ))]])
					(if (:discard action) [:div.d-flex (text_svg "+") [:div.text-ab.mx-1 "discard\na card"]])

				])

			(if (> acount 0) [:img.my-auto.mx-1 {:style {:height "25px"} :src "/img/ra/rae/then.png"}])
		
			(if (:ignore action) 
				(if (:place action)
						[:div.text-ab.text-ab-sm "ignore\nand\ngain"]
						[:div.text-ab "ignore"]))
			(if (:checkvictory action) [:div.text-ab.ms-1 "check victory now!"])
			(if-let [place (:place action)]
				[:div.d-flex
					(render-essence-list (dissoc place :cost))
					[:div.text-ab.mx-1 "on"]
					[:img.img-ab {:src "img/ra/rae/place.png"}]])
			(if-let [gain (:gain action)] (render-essence-list gain))
			(if-let [gre (:gainrivalequal action)] [:div.text-ab.text-ab-sm.mx-1 "gain" (essence-svg (first gre) "?") "equal to" (essence-svg (last gre) "?") "of one rival"	])
			(if-let [rg (:rivals action)] [:div.d-flex (text_svg "+") [:span.text-ab.mx-1 "all rivals gain"] (render-essence-list rg) ])
			(if-let [ct (:convertto action)]
					(if (:destroy action)
							(let [convertto_plus (reduce-kv (fn [m k v] (assoc m k (if-let [cp (:convertplus action)] (str "+" cp) 1))) ct (dissoc ct :exclude))   ]
								[:div.d-flex [:div.text-ab.mx-1 (if (:discard action) "gain the\ndiscard's" "gain")] (place-cost-svg "?") [:div.text-ab.mx-1 "in"] (render-essence-list convertto_plus) ])))
			(if (:straighten action) 
				(let [restriction (:restriction action)]
					[:img.img-ab {:title (str restriction) :src (str "/img/ra/rae/straighten" (if (= "Creature" (:subtype restriction)) "_creature") ".png")}]))
			(if (or (:reducer_b action) (:reducer_a action))
				[:div.d-flex 
					[:div.text-ab.mx-1 
						"Place " 
						(if-let [t (-> action :restriction :type)] (str t "s")) 
						(if-let [st (-> action :restriction :subtype)] (str st "s")) ;[:img.img-ab {:src (str "/img/re/rae/"  ".png")}])
						" at" ]
					[:div.me-1 (place-cost-svg "?")]
					(render-essence-list (reduce-kv #(if (number? %3) (assoc %1 %2 (str "- " %3)) %1) (:reduction action) (:reduction action)))])
			(if-let [draw (:draw action)]
				[:div.text-ab.mx-1
					"draw" [:span.text-ab-lg.mx-1 draw] "card" ])
			(if (:draw3 action)
				[:div.text-ab.text-ab-sm.mx-1
					"draw" [:span.text-ab-lg.mx-1 "3"] "cards, reorder, put back\n" [:em "(may also use on Monument deck)"]])
			(if (:divine action) [:div.text-ab "draw" [:span.text-ab-lg.mx-1 "3"] "cards, add\nto hand, discard" [:span.text-ab-lg.ms-1 "3"]])
			
		]))
		