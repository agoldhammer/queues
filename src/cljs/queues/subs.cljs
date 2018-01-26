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

(r/reg-sub
 ::sinks
 (fn [db]
   (:sinks db)))

(r/reg-sub
 ::sink
 (fn [db [_ sink-id]]
   (sink-id (:sinks db))))

(r/reg-sub
 ::scheduled
 (fn [db [_ sink-id]]
   (get-in db [:sinks sink-id :scheduled])))

(r/reg-sub
 ::psgrs
 (fn [db]
   (:psgrs db)))

(r/reg-sub
 ::queued
 (fn [db]
   (:queued db)))

(r/reg-sub
 ::clock
 (fn [db]
   (str (:clock db))))
