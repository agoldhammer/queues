(ns queues.db
  (:require [kixi.stats.core :as kixi]
            [kixi.stats.distribution :as d]
            [re-frame.core :as rf]))

(def sched-deps [3.25 3.5 4.0 4.5 5])

(defn make-sink
  [idnum capacity]
  {:id (keyword (str "sink" idnum))
   :capacity capacity
   :occupied 0
   :scheduled (* 3600 (sched-deps idnum))})

(defn make-agent
  [idnum]
  {:id (keyword (str "agent" idnum))})

(defn add-type
  "add map of ids to n instances of type under type key in db
  using function make-type-f to create each instance"
  [make-type-f n]
  (let [entities (map make-type-f (range n))]
    (reduce conj {} (map
                     (fn [ent] [(:id ent) ent])
                     entities))))

(def default-db
  {:name " via re-frame"
   :sinks (add-type #(make-sink % 300) 5)
   :sources []
   :agents (add-type make-agent 10)})

;; we model arrival times as normally distributed around
;; 90 minutes ahead of departure, with sd of 30 mins

(def arrival-t-dist (d/normal {:mu 5400 :sd 1800}))

(defn gen-arrival-deltas
  [n]
  (map Math/round (d/sample n arrival-t-dist)))

(defn make-psgr
  [idnum dest arrived-at processed-at]
  {:id (keyword (str "psgr" idnum))
   :dest dest
   :arrived-at arrived-at
   :processed-at processed-at})

(defn calc-arr
  [scheduled delta]
  (let [arr (- scheduled delta)]
    (cond
      (< arr 0) 0
      (> arr scheduled) scheduled
      :else arr)))

(defn make-psgrs
  [dest n]
  (let [scheduled @(rf/subscribe [:scheduled dest])
        deltas (gen-arrival-deltas n)]
    (map-indexed #(make-psgr %1 dest
                             (calc-arr scheduled %2) 0)
                 deltas)))
