(ns queues.events
  (:require [re-frame.core :as rf]
            [queues.db :as db]))

(rf/reg-event-db
 ::initialize-db
 (fn  [_ _]
   db/default-db))

(rf/reg-event-db
 ::start
 (fn [db _]
   (let [earliest (:arrived-at (first (:psgrs db)))]
     #_(println (:psgrs db))
     (assoc db :clock (dec earliest))) ))
