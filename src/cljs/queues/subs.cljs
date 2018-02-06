(ns queues.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :name
 (fn [db]
   (:name db)))

(rf/reg-sub
 :sink-ids
 (fn [db]
   (keys (:sinks db))))

(rf/reg-sub
 :agent-ids
 (fn [db]
   (keys (:agents db))))

(rf/reg-sub
 :sinks
 (fn [db]
   (:sinks db)))

(rf/reg-sub
 :sink
 (fn [db [_ sink-id]]
   (sink-id (:sinks db))))

(rf/reg-sub
 :scheduled
 (fn [db [_ sink-id]]
   (get-in db [:sinks sink-id :scheduled])))

(rf/reg-sub
 :psgrs
 (fn [db]
   (:psgrs db)))

(rf/reg-sub
 :queued
 (fn [db]
   (:queued db)))

(rf/reg-sub
 :qhead
 (fn [db]
   (peek (:queued db))))

(rf/reg-sub
 :sink-occupied-peek
 (fn [db [_ sink]]
   (peek (:occupied (sink (:sinks db))))))

(rf/reg-sub
 :occupied
 (fn [db [_ sinkid]]
   (:occupied (sinkid (:sinks db)))))

(rf/reg-sub
 :clock
 (fn [db]
   (str (:clock db))))

(rf/reg-sub
 :running
 (fn [db]
   (:running db)))

;; return first psgr in unprocessed queue
(rf/reg-sub
 :first-unprocessed
 (fn [db]
   (peek (:psgrs db))))

(rf/reg-sub
 :agents
 (fn [db _]
   (:agents db)))

(rf/reg-sub
 :agent-by-id
 (fn [db [_ id]]
   (id (:agents db))))

;; this returns the head of the busy queue, which is the psgr
;;  currently being service by this agent, if any; otherwise returns nil
(rf/reg-sub
 :agent-busy?
 (fn [db [_ id]]
   (if (peek (:busy (id (:agents db))))
     true false)))

(rf/reg-sub
 :agent-working-on
 (fn [db [_ agtid]]
   (peek (:busy (agtid (:agents db))))))

(rf/reg-sub
 :proc-time
 (fn [db [_ agtid]]
   (:proc-time (agtid (:agents db)))))

(rf/reg-sub
 :agent-open?
 (fn [db [_ id]]
   (:open (id (:agents db)))))

;; in order to get color of a psgr we use the color of the associated
;; sink, which we find with a kludge, since each psgr id is of the
;; form :psgrnnsinkmm, where n and m are integers
;; so we use the following regexp:

(defn psgrid-to-sinkid
  [psgrid]
  (let [m (re-matches #"(psgr\d+)(sink\d+)" (name psgrid))]
    (keyword (m 2))))

(rf/reg-sub
 :psgr-to-color
 (fn [db [_ psgrid]]
   (let [sinkid (psgrid-to-sinkid psgrid)]
     (:color (sinkid (:sinks db))))))
