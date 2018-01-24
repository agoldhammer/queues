(ns queues.views
  (:require [re-frame.core :as f]
            [re-com.core :as c]
            [queues.subs :as subs]
            ))

(defn title []
  [c/title
   :label (str "A BIG Hello from Art" @(f/subscribe [::subs/name]))
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
           :width 1000 :height 100}
      (map-indexed #(rect %2 (* %1 50) 0 38 18)
                   @(f/subscribe [::subs/sink-ids]))]))

(defn sink-elt
  "Creates set of sink elts with ids from db"
  []
  (fn []
    [:svg {:style {:border "thin solid black"}
           :width 1000 :height 200}
       (map-indexed #(rect %2 (* %1 200) 0 180 200)
                   @(f/subscribe [::subs/sink-ids]))
     #_(circle 99 4 4 2)]))

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
               :children [[c/button :label "btn"
                           :style {:background-color "lightblue"}]]]
              [c/line]
              [c/gap :size "15px"]
              [sink-area] [agent-area]]])
