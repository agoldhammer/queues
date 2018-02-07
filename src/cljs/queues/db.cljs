(ns queues.db
  (:require [kixi.stats.core :as kixi]
            [kixi.stats.distribution :as d]
            [re-frame.core :as rf]
            [queues.subs :as subs]
            [cljs.core.async :as async])
  (:require-macros [cljs.core.async.macros
                    :as m :refer [go alt!]]))

;; clock channels
(def clock-ch (async/chan (async/dropping-buffer 25)))

(def multi-clock (async/mult clock-ch))

(def clock-ch-1 (async/chan))

(def clock-ch-2 (async/chan))

(async/tap multi-clock clock-ch-1)

#_(async/tap multi-clock clock-ch-2)

(defn pulse []
  (async/put! clock-ch :pulse))

;; end clocking

(def sched-deps [3.0 3.5 4.0 4.5 5.25])
(def color ["red", "orange", "blue", "green", "magenta"])

(def NPSGRS 180)

(defn make-sink
  [idnum capacity]
  {:id (keyword (str "sink" idnum))
   :capacity capacity
   :occupied #queue []
   :color (color idnum)
   :scheduled (* 3600 (sched-deps idnum))})

(defn make-agent
  [idnum]
  {:id (keyword (str "agent" idnum))
   :busy #queue []
   :proc-time 0
   :open true})

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

(def arrival-t-dist (d/normal {:mu 5400 :sd 2200}))

(def agent-t-dist (d/normal {:mu 100 :sd 25}))

(defn agent-time
  []
  (let [x (Math/round (first (d/sample 1 agent-t-dist)))]
    (if (< x 40) 40 x)))

(defn gen-arrival-deltas
  [n]
  (map Math/round (d/sample n arrival-t-dist)))

(defn make-psgr
  [idnum dest arrived-at processed-at]
  {:id (keyword (str "psgr" idnum))
   :dest dest
   :arrived-at arrived-at
   :processed-at processed-at})

(defn make-passenger
  [idnum dest color arrived-at processed-at]
  {:id (keyword (str "psgr" idnum (name dest)))
   :dest dest
   :color color
   :arrived-at arrived-at
   :processed-at processed-at})

(defn calc-arr
  [scheduled delta]
  (let [arr (- scheduled delta)]
    (cond
      (< arr 0) 0
      (> arr scheduled) scheduled
      :else arr)))

(def partial-db
  {:name " via re-frame"
   :speedup 50
   :clock 0
   :timer-fn nil
   :running false
   :sinks (add-type #(make-sink % NPSGRS) 5)
   :queued #queue []
   :agents (into (sorted-map) (add-type make-agent 10))})

(defn make-passengers
  [dest n]
  (let [deltas (gen-arrival-deltas n)
        sinks (:sinks partial-db)
        sink (dest sinks)
        color (:color sink)
        scheduled (:scheduled sink)]
    (map-indexed #(make-passenger %1 dest color (calc-arr scheduled %2) 0)
                     deltas)))

(defn sort-psgrs
  [psgrs]
  (sort-by #(:arrived-at %) psgrs))
(defn make-psgrs
  [dest scheduled n]
  (let [deltas (gen-arrival-deltas n)]
    (map-indexed #(make-psgr %1 dest
                             (calc-arr scheduled %2) 0)
                 deltas)))

(defn make-passenger-lists
  [n]
  (let [dests (keys (:sinks partial-db))]
    (apply concat (map #(make-passengers %1 n) dests))))

(defn make-sorted-passenger-queue
  [n]
  (into #queue [](sort-psgrs (make-passenger-lists n))))

(defn make-default-db
  []
    (assoc partial-db :psgrs (make-sorted-passenger-queue NPSGRS)))


(defn- emptyq? [q]
  (not (peek q)))

(declare agent-assign)
(declare agts-to-sinks)
;; go routine to move items from unprocessed to queued
(m/go-loop []
  (async/<! clock-ch-1)
  (when-let [nextup @(rf/subscribe [:first-unprocessed])]
    (let [now @(rf/subscribe [:clock])]
      (when (> now (:arrived-at nextup))
        (rf/dispatch [:queue-one nextup])))
    )
  (agent-assign)
  (agts-to-sinks)
  (recur))

(defn agt-open? [id]
  @(rf/subscribe [:agent-open? id]))

(defn agt-not-busy? [id]
  (not @(rf/subscribe [:agent-busy? id])))

(defn open-agents []
  (let [agents @(rf/subscribe [:agents])]
    (filter agt-open? (keys agents))))

(defn available-agents []
  (filter agt-not-busy? (open-agents)))

(defn move-from-qhead-to-agt
  [psgr agtid proctime]
  (rf/dispatch [:psgr-to-agt psgr agtid proctime]))

(defn move-from-agt-to-sink
  [agtid]
  (rf/dispatch [:agt-to-sink agtid]))

;; go routine to move items from queued to available agent(s)
;; On each tick: visit each available agent (open and not busy)
;; move psgr from head of queue
;; and set processing time to random pick from the distribution
(defn agent-assign
  []
  (when-let [agtid (first (available-agents))]
    (when-let [psgr @(rf/subscribe [:qhead])]
      (do
        (rf/dispatch [:behead-queue])
        (move-from-qhead-to-agt psgr agtid (agent-time))))))

;; go routine to move from agents to sinks
;; TODO modify to CHECK PROCTIME expired
(defn agts-to-sinks []
  (let [agentids (keys @(rf/subscribe [:agents]))]
    (doseq [agtid agentids]
      (when @(rf/subscribe [:agent-busy? agtid])
        (if (zero? @(rf/subscribe [:proc-time agtid]))
          (move-from-agt-to-sink agtid)
          (rf/dispatch [:dec-proc-time agtid]))))))
