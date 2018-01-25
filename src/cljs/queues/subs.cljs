(ns queues.subs
  (:require [re-frame.core :as r]))

(r/reg-sub
 ::name
 (fn [db]
   (:name db)))

(r/reg-sub
 ::sink-ids
 (fn [db]
   (keys (:sinks db))))

(r/reg-sub
 ::agent-ids
 (fn [db]
   (keys (:agents db))))
