(ns martian-robots.core
  (use clojure.tools.trace)
  (:gen-class))

(def robot {:x 0, :y 0, :heading \N})

(def displacements
  { "N"  (fn [x y] {:x x :y (inc y)})
    "E"  (fn [x y] {:x (inc x) :y y})
    "S"  (fn [x y] {:x x :y (dec y)})
    "W"  (fn [x y] {:x (dec x) :y y})})

(defn turn
  [heading step]
  (->> (keys displacements)
       (#(if (pos? step) % (reverse %)))
       cycle
       (drop-while (partial not= heading))
       (#(nth % (Math/abs step)))))

(def moves
  { "F" (fn [{:keys [x y heading] :as robot}]
         (into robot ((displacements (str heading)) x y)))
    "R" (fn [{:keys [heading] :as robot}]
         (into robot {:heading (turn (str heading) 1)}))
    "L" (fn [{:keys [heading] :as robot}]
         (into robot {:heading (turn (str heading) -1)}))})

(defn set-grid [state x y]
  (assoc state :grid {:x (Integer/parseInt x)
                      :y (Integer/parseInt y)}))

(defn init-robot [state x y heading]
  (assoc state :robot {:x (Integer/parseInt x)
                       :y (Integer/parseInt y)
                       :heading heading
                       :lost false}))

(defn is-lost?
  [{:keys [grid robot]}]
  (some
   (fn [kw] (or (> (kw robot) (kw grid))
                (< (kw robot) 0)))
   [:x :y]))

(defn validate-and-update-state [state next-robot command]
  (let [next-state (assoc state :robot next-robot)]
    (if (trace "is-lost?" (is-lost? (trace "state" next-state)))
      (->
       state
       (assoc-in [:robot :lost] true)
       (update :danger-zones #(conj % [(state :robot) command])))
      next-state)))

(defn exec-commands [state commands]
  (loop [[command & rest] commands
         {:keys [robot] :as prev-state} state]
    (let [next-robot ((moves (str command)) robot)
          next-state (validate-and-update-state prev-state next-robot command)]
      (if (empty? rest)
          next-state
          (recur rest next-state)))))

(def parse-commands
  {#"(\d+)\s(\d+)" set-grid              ;; grid numbers
   #"(\d+)\s(\w+)\s(N|E|S|W)" init-robot ;; init robot
   #"([F|L|R]+)" exec-commands})         ;; command stream

(def base-state
  {:danger-zones #{} ;; Danger Will Robinson!
   :robot robot
   :grid {:x 0 :y 0}})

(defn stream-commands
  [state stream]
  (loop [curr-state state
         [cmd & rest] (clojure.string/split stream #"\n")]
    (as-> parse-commands $
          (map (fn [[regex f]] [(re-matches regex cmd) f]) $)
          (filter (fn [[matches]] (not (nil? matches))) $)
          (first $)
          (let [[[first & args] f] $
                next-state (apply (partial f curr-state) args)]
            (if (empty? rest)
                next-state
                (recur next-state rest))))))


; (stream-commands base-state "5 3
; 1 1 E
; RFRFRFRF")
;
(stream-commands base-state "5 3
3 2 N
FRRFLLFFRRFLL")




; (stream-commands base-state "5 3
; 1 1 E
; RFRFRFRF
;
; 3 2 N
; FRRFLLFFRRFLL
;
; 0 3 W
; LLFFFLFLFL")

(defn -main []
  (println "Hello world"))
