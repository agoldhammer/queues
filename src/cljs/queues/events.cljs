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

(def clock
  (js/setInterval #(rf/dispatch [::tick]) 500))
