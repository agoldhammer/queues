(ns queues.db)

(defn make-sink
  [idnum capacity time-to-empty]
  {:id (keyword (str "sink" idnum))
   :capacity capacity
   :occupied 0
   :time-to-empty time-to-empty})

#_(defn add-sinks
  [n]
  (map #(make-sink % 300 120) (range n)))

(defn make-agent
  [idnum]
  {:id (keyword (str "agent" idnum))})

#_(defn add-agents
  [n]
  (map make-agent (range n)))

(defn add-type
  "add vector of n instances of type to db
  using function make-type-f to create each instance"
  [make-type-f n]
  (mapv make-type-f (range n)))

(def default-db
  {:name " via re-frame"
   :sinks (add-type #(make-sink % 300 120) 5)
   :sources []
   :agents (add-type make-agent 10)})
