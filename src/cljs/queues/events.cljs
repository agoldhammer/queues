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
       (update-in [:psgrs] #(-> % rest vec))
       (update-in [:queued] conj psgr))))

(rf/reg-event-db
 :agent-toggle-open
 (fn [db [_ id]]
      (update-in db [:agents id :open] not)))

;; send ticks to clock-ch and update :clock in db
(defn heartbeat
  []
  (rf/dispatch [::tick])
  (db/pulse))

;; drive action with regular ticks
(def clock
  (js/setInterval heartbeat 50))
