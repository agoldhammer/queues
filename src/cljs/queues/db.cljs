(ns queues.db
  (:require [kixi.stats.core :as kixi]
            [kixi.stats.distribution :as d]
            [re-frame.core :as rf]
            [queues.subs :as subs]
            [cljs.core.async :as async])
  (:require-macros [cljs.core.async.macros
                    :as m :refer [go alt!]]))

;; clock channel
(def clock-ch (async/chan (async/dropping-buffer 1)))

(defn pulse []
  (async/put! clock-ch :pulse))

(def sched-deps [3.25 3.5 4.0 4.5 5])

(def NPSGRS 10)

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
  [dest scheduled n]
  (let [deltas (gen-arrival-deltas n)]
    (map-indexed #(make-psgr %1 dest
                             (calc-arr scheduled %2) 0)
                 deltas)))

(defn sort-psgrs
  [psgrs]
  (sort-by #(:arrived-at %) psgrs))

;; TODO this is for testing only; need to concat psgr lists
;; for all sinks in final version
(defn make-psgr-list [dest scheduled n]
  (doall (sort-psgrs (make-psgrs dest scheduled n))))

(def default-db
  (let [partial-db
        {:name " via re-frame"
         :clock 0
         :running false
         :sinks (add-type #(make-sink % NPSGRS) 5)
         :psgrs []
         :queued []
         :agents (add-type make-agent 10)}]
    ;; TODO: this is for testing only; uses only one destination
    (assoc partial-db :psgrs (make-psgr-list :sink0
                                             (:scheduled (:sink0 (:sinks partial-db)))
                                             NPSGRS))))

;; go routine to move items from unprocessed to queued
(m/go-loop []
  (async/<! clock-ch)
  (when-let [nextup @(rf/subscribe [::subs/first-unprocessed])]
    (let [now @(rf/subscribe [::subs/clock])]
      (when (> now (:arrived-at nextup))
        (rf/dispatch [:queue-one nextup])))
    (recur)))
