(ns queues.views
  (:require [re-frame.core :as rf]
            [re-com.core :as c]
            [queues.subs :as subs]
            [queues.events :as events]
            ))

;; layout of queues in rows alternating in direction, like mower
(defn mower [n xstart ystart xspace yspace xmax ymax xmin]
  (loop [res []
         x   xstart
         y   ystart
         dir 1
         i   0]
    (if (< i n)
      (let [xnew (+ x (* dir xspace))
            ynew (+ y yspace)]
        (if (and (< xnew xmax)
                 (> xnew xmin))
          (recur (conj res [xnew y]) xnew y dir (inc i))
          (recur (conj res [x ynew]) x ynew (- dir) (inc i))))
      res)))


(defn title []
[c/title
    :label (str "A BIG Hello from Art" @(rf/subscribe [:name]))
    :level :level4])

(defn circle
  [id [cx cy] r]
  [:circle {:key id
            :id id
            :style {:fill :red
                    :stroke-width 2
                    :stroke :black}
            :on-click #(prn (-> % .-target .-id))
            :cx cx
            :cy cy
            :r r}])

(defn rect
  [id x y w h color clickfn]
  [:rect {:id id
          :key id
          :style {:fill color ;; "#80ffaa" ;; "rgba(200,128,128,0.4)"
                  :stroke-width 2
                  :stroke :black}
          :x x
          :y y
          :width w
          :height h
          :on-click #(clickfn id)}])

(defn agent-rect
  [id i-pos]
  (let [open @(rf/subscribe [:agent-open? id])
        color (if open "#80ffaa" "red")]
    (rect id (* i-pos 100) 0 90 48  color
          #(rf/dispatch [:agent-toggle-open %]))))

(defn agent-elt
  []
  (fn []
     [:svg {:style {:border "thin solid black"}
           :width 1000 :height 50}
      (doall
       (map-indexed #(agent-rect %2 %1)
                    @(rf/subscribe [:agent-ids])))]))

(defn sink-elt
  "Creates set of sink elts with ids from db"
  []
  (fn []
    [:svg {:style {:border "thin solid black"}
           :width 1000 :height 200}
       (map-indexed #(rect %2 (* %1 200) 0 180 196 "yellow" prn)
                   @(rf/subscribe [:sink-ids]))]))

(defn display-queued
  []
  (let [queue @(rf/subscribe [:queued])]
    (doall
     (map #(circle (:id %1) %2 6) queue
              (mower (count queue) 500 14 15 20 990 300 15)))))

(defn queue-elt
  []
  (fn []
    [:svg {:style {:border "thin solid black"}
           :width 1000 :height 400}
     (display-queued)]))

(defn agent-area []
  [c/h-box
   :gap "1em"
   :size "auto"
   :margin "10px"
   :align :center
   :children [[agent-elt]]])

(defn sink-area []
  [c/h-box
   :gap "1em"
   :size "auto"
   :margin "10px"
   :align :center
   :children [[sink-elt]]])

(defn queuing-area []
  [c/h-box
   :gap "1em"
   :size "auto"
   :margin "10px"
   :align :center
   :children [[queue-elt]]])

(defn title-area []
  [c/h-box
   :margin "10px"
   :children [[title]]])

(defn main-panel []
  [c/v-box
   :height "100%"
   :children [[title-area]
              [c/gap :size "15px"]
              [c/h-box
               :margin "10px"
               :children [[c/button :label "Start"
                           :style {:background-color "lightblue"}
                           :on-click #(rf/dispatch [::events/start-stop])]
                          [c/gap :size "15px"]
                          [c/title
                           :level :level2
                           :label @(rf/subscribe [:clock])]]]
              [c/line]
              [c/gap :size "15px"]
              [sink-area] [agent-area] [queuing-area]]])
