(ns queues.events
  (:require [re-frame.core :as r]
            [queues.db :as db]))

(r/reg-event-db
 ::initialize-db
 (fn  [_ _]
   db/default-db))
