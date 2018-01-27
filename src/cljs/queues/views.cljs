(ns queues.views
  (:require [re-frame.core :as rf]
            [re-com.core :as c]
            [queues.subs :as subs]
            [queues.events :as events]
            ))

(defn title []
  [c/title
   :label (str "A BIG Hello from Art" @(rf/subscribe [::subs/name]))
   :level :level1])

(defn circle
  [id cx cy r]
  [:circle {:key id
            :style {:fill :red
                    :stroke-width 2
                    :stroke :black}
            :cx cx
            :cy cy
            :r r}])

(defn rect
  [id x y w h]
  [:rect {:id id
          :key id
          :style {:fill "rgba(200,128,128,0.4)"
                  :stroke-width 2
                  :stroke :black}
          :x x
          :y y
          :width w
          :height h
          :on-click #(prn (-> % .-target .-id))}])

(defn agent-elt
  []
  (fn []
     [:svg {:style {:border "thin solid black"}
           :width 1000 :height 50}
      (map-indexed #(rect %2 (* %1 100) 0 90 50)
                   @(rf/subscribe [::subs/agent-ids]))]))

(defn sink-elt
  "Creates set of sink elts with ids from db"
  []
  (fn []
    [:svg {:style {:border "thin solid black"}
           :width 1000 :height 200}
       (map-indexed #(rect %2 (* %1 200) 0 180 200)
                   @(rf/subscribe [::subs/sink-ids]))
     (circle 99 90 5 2)]))

(defn display-queued
  [count]
  (let [pos 0
        row 0
        x   500
        y   14
        id  1000
        queue @(rf/subscribe [::subs/queued])
        ]
    (for [i (range count)]
     (circle (+ id i) (+ x (* 15 i)) y 6) )))

(defn queue-elt
  []
  (fn []
    [:svg {:style {:border "thin solid black"}
           :width 1000 :height 400}
     (display-queued 5)]))

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
                           :label @(rf/subscribe [::subs/clock])]]]
              [c/line]
              [c/gap :size "15px"]
              [sink-area] [agent-area] [queuing-area]]])
