(ns martian-robots.core
  (use clojure.tools.trace)
  (:gen-class))

(def robot {:x 0, :y 0, :heading \N})

(def base-state
  {:danger-zones #{} ;; Danger Will Robinson!
   :robot robot
   :grid {:x 0 :y 0}})

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


(defn danger?
  [{:keys [robot danger-zones] :as state} command]
  (trace "danger" [robot command])
  (boolean (danger-zones [robot command])))

(defn validate-and-update-state [state next-robot command]
  (let [next-state (assoc state :robot next-robot)]
    (cond
      (danger? state command) state
      (is-lost? next-state) (-> state
                                (assoc-in [:robot :lost] true)
                                (update :danger-zones #(conj % [(state :robot) command])))
      :else next-state)))

(defn exec-commands [state commands]
  (loop [[command & rest] commands
         {:keys [robot] :as prev-state} state]
    (let [next-robot ((moves (str command)) robot)
          next-state (validate-and-update-state prev-state next-robot command)]
      (if (or (empty? rest) (get-in next-state [:robot :lost]))
          next-state
          (recur rest next-state)))))

(defn apply-args [f state]
  (fn [[first & args]] (apply (partial f state) args)))

(defn parse-commands [state s]
  (condp re-matches s
    #"(\d+)\s(\d+)" :>> (apply-args set-grid state)
    #"(\d+)\s(\w+)\s(N|E|S|W)" :>> (apply-args init-robot state)
    #"([F|L|R]+)" :>> (apply-args exec-commands state)
    state))

(defn stream-commands
  [state stream]
  (loop [curr-state state
         [cmd & rest] (clojure.string/split stream #"\n")]
     (let [next-state (trace cmd (parse-commands curr-state cmd))]
      (if (empty? rest)
          next-state
          (recur next-state rest)))))

(defn -main []
  (println "Hello world"))
