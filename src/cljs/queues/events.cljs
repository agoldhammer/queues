(ns queues.events
  (:require [re-frame.core :as rf]
            [queues.db :as db]))

(rf/reg-event-db
 ::initialize-db
 (fn  [_ _]
   db/default-db))

(rf/reg-event-db
 ::start-stop
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
   (-> db
       (update-in [:psgrs] pop)
       (update-in [:queued] conj psgr))))

(rf/reg-event-db
 :queue-to-agent
 (fn [db [_ agent]]
   (when-let [psgr (peek (:queued db))]
     (-> db
         (update-in [:agents agent :busy] conj psgr)
         (update-in [:queued] pop)))))

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
 :qhead-to-agt
 (fn [db [_ agtid proctime]]
   (-> db
       ;; move head of q to agent
       (update-in [:agents agtid :busy] conj (peek (:queued db)))
       ;; then update proc-time
       (update-in [:agents agtid] conj {:proc-time proctime})
       (update-in [:queued] pop))))

;; send ticks to clock-ch and update :clock in db
(defn heartbeat
  []
  (rf/dispatch [::tick])
  (db/pulse))

;; drive action with regular ticks
(def clock
  (js/setInterval heartbeat 25))
