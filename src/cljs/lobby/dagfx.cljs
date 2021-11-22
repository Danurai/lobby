(ns lobby.dagfx)


  
(defn blips [ ctx {:keys [top bot]} x]
  (set! (.-font ctx) "16pt Orbitron")
  (set! (.-fillStyle ctx) "rgb(0,200,0)")
  (set! (.-textAlign ctx) "center")
  
  (.fillText ctx top x 40)
  (.fillText ctx bot x 275)
  )

(defn threat [ ctx x y terrain ]
  (let [c (get ["","rgb(50,250,50)","rgb(250,250,50)","rgb(250,128,0)","rgb(250,50,50)"] (:threat terrain))]
    (set! (.-strokeStyle ctx) c)
    (set! (.-fillStyle ctx)   c)
    (set! (.-lineWidth ctx) 2)
    (doseq [n (range 1 5)]
      (.beginPath ctx)
      (.rect ctx (-> n (* 7) (+ x)) (-> terrain :id (* 5) (+ y)) 4 10)
      (.stroke ctx)
      (if (>= (:threat terrain) n) (.fill ctx)))))
        
(defn marine [ ctx img hl m x ]
  (let [grad (.createLinearGradient ctx 0 0 0 300)]
    (set! (.-src img) "img/da/out.png")
    (.addColorStop grad 0   "rgba(255,255,255,0)")
    (.addColorStop grad 0.5 "rgba(255,255,255,0.8)")
    (.addColorStop grad 1   "rgba(255,255,255,0)")
    
    ; Light/Facing
    (.beginPath ctx)
    (.rect ctx x (if (-> m :facing (= :top)) 0 90) 150 210)
    (set! (.-fillStyle ctx) grad)
    (.fill ctx)
    
    ; Highlight / ring
    (.save ctx)
    (when (-> hl :type (= 1))
        (set! (.-shadowBlur ctx) 10)
        (set! (.-shadowColor ctx) "white"))
    (set! (.-fillStyle ctx) (-> m :squad name str))
    
    (.beginPath ctx)
    (.arc ctx (+ x 75) 150 54 0 (* 2 Math.PI))
    (.fill ctx)
    
    (.restore ctx)
    
    ; Marine
    (.drawImage ctx img 0 0 96 96 (+ x 27) 102 96  96)
    
    ; Stats
    
    (.beginPath ctx)
    (.rect ctx (+ x 10) 180 130 20)
    (set! (.-fillStyle ctx)   "rgba(0,0,0,0.8)")
    (set! (.-lineWidth ctx)   2)
    (set! (.-strokeStyle ctx) "rgba(255,255,255,0.8)")
    (.fill ctx)
    (.stroke ctx)
    
    
    (set! (.-textAlign ctx) "center")
    (set! (.-font ctx) "10pt Orbitron")
    (set! (.-fillStyle ctx) "rgba(0,0,0,0.2)")
    (.beginPath ctx)
    (let [tw (.-width (.measureText ctx (:name m)))]
      (.rect ctx (+ (- x 5) (-> 150 (- tw) (/ 2))) 90 (+ 10 tw) 20))
    (.fill ctx)
    
    (.beginPath ctx)
    (set! (.-textAlign ctx) "center")
    (set! (.-font ctx) "10pt Orbitron")
    (set! (.-fillStyle ctx) "rgba(255,255,255,1)")
    (.fillText ctx (:name m) (+ x 75) 105)
    
  ))
  
  
(defn swarm [ ctx s x y ]
  (set! (.-font ctx) "16pt Orbitron")
  (set! (.-fillStyle ctx) "rgb(0,200,0)")
  (if (-> s count (> 0)) (.fillText ctx (count s) (+ x 10) (+ y 10) ))
  )
  
   ;(.addColorStop grdbot 1 "rgba(255,255,255,0)")
   ;    (if (-> f :marine :facing (= :top))
   ;        (set! (.-fillStyle ctx) grdtop)
   ;        (set! (.-fillStyle ctx) grdbot))
   ;    (.beginPath ctx)
   ;    (.rect ctx x (if (-> f :marine :facing (= :top)) 0 200) 150 100)
   ;    (.fill ctx)