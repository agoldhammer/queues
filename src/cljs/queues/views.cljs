(ns queues.views
  (:require [re-frame.core :as f]
            [re-com.core :as c]
            [queues.subs :as subs]
            ))

(defn title []
  [c/title
   :label (str "A BIG Hello from Art" )
   :level :level1])

(defn rect
  [id x y w h]
  [:rect {:key id
          :style {:fill "rgba(200,128,128,0.4)"
                  :stroke-width 2
                  :stroke :black}
          :x x
          :y y
          :width w
          :height h}])

(defn svg-pane
  []
  (fn []
    [:svg {:style {:border "thin solid black"}
           :width 1000 :height 1000}
     (rect 1 600 450 30 30)
     (rect 2 300 500 20 20)
     (for [i (range 5)]
       (rect (+ i 3) (* i 100) 30, 20 20))]))

(defn svg-area []
  [c/h-box
   :gap "1em"
   :size "auto"
   :margin "10px"
   :align :center
   :children [[svg-pane]]])

(defn main-panel []
  [c/v-box
   :height "100%"
   :children [[svg-area]]])
