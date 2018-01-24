(ns queues.db)

(defn make-sink
  [idnum capacity time-to-empty]
  {:id (keyword (str "sink" idnum))
   :capacity capacity
   :occupied 0
   :time-to-empty time-to-empty})

(defn add-sinks
  [n]
  (map #(make-sink % 300 120) (range n)))

(def default-db
  {:name " via re-frame"
   :sinks (add-sinks 5)
   :sources []
   :agents []})
