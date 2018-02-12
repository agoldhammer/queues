(ns queues.events
  (:require [re-frame.core :as rf]
            [queues.db :as db]))

(declare heartbeat)

(rf/reg-event-db
 :initialize-db
 (fn  [_ _]
   (let [temp (db/make-default-db)
         speedup (:speedup temp)]
     (assoc
      temp
      :timer-fn (js/setInterval heartbeat (/ 1000 speedup))))))

(rf/reg-event-db
 :start-stop
 (fn [db _]
   (if (:running db)
     (assoc db :running false)
     (let [earliest (:arrived-at (first (:psgrs db)))]
       (-> db
           (assoc :running true)
           (assoc :clock (dec earliest))) )) ))

(rf/reg-event-db
 ::tick
 (fn [db _]
   (if (:running db)
     (update-in db [:clock] inc)
     db)))

(rf/reg-event-db
 :queue-one
 (fn [db [_ psgr]]
   (let [maxq (max (inc (count (:queued db)))(:max-qlength db))]
     (-> db
         (assoc-in [:max-qlength] maxq)
         (update-in [:psgrs] pop)
         (update-in [:queued] conj psgr)))))

(rf/reg-event-db
 :agent-toggle-open
 (fn [db [_ id]]
      (update-in db [:agents id :open] not)))

;; add item to a persistent queue
;; e.g. (dispatch [add-to-sink-occupied asink psgr])
(rf/reg-event-db
 :add-to-sink-occupied
 (fn [db [_ sink psgr]]
   (update-in db [:sinks sink] conj psgr)))

(rf/reg-event-db
 :behead-queue
 (fn [db _]
   (update-in db [:queued] pop)))

(rf/reg-event-db
 :psgr-to-agt
 (fn [db [_ psgr agtid proctime]]
   (let [clock @(rf/subscribe [:clock-as-int])
         ;; processed-at is end of processing, so includes proctime
         updated-psgr (assoc psgr :processed-at (+ clock proctime))]
     (-> db
         ;; move head of q to agent
         (update-in [:agents agtid :busy] conj updated-psgr)
         ;; then update proc-time
         (update-in [:agents agtid] conj {:proc-time proctime})))))

(rf/reg-event-db
 :toggle-info
 (fn [db [_ _]]
   (update-in db [:info?] not)))

(rf/reg-event-db
 :agt-to-sink
 (fn [db [_ agtid]]
   (let [psgr (peek (:busy (agtid (:agents db))))
         dest (:dest psgr)]
     (-> db
         (update-in [:sinks dest :occupied] conj psgr) ;; add psgr to sink
         (update-in [:agents agtid :busy] pop)))))

(rf/reg-event-db
 :dec-proc-time
 (fn [db [_ agtid]]
   (update-in db [:agents agtid :proc-time] dec)))

(rf/reg-event-db
 :speedup-change
 (fn [db [_ val]]
   (let [interval (/ 1000 val)]
     (when (:timer-fn db)
       (js/clearInterval (:timer-fn db)))
     (-> db
         (assoc :speedup val)
         (assoc :timer-fn (js/setInterval heartbeat interval))))))

(rf/reg-event-db
 :close-agent
 (fn [db [_ agtid]]
   (assoc-in db [:agents agtid :open] false)))

;; send ticks to clock-ch and update :clock in db
(defn heartbeat
  []
  (when @(rf/subscribe [:running])
    (do
      (rf/dispatch [::tick])
      (db/pulse))))
