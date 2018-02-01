(ns queues.views
  (:require [re-frame.core :as rf]
            [re-com.core :as c]
            [queues.subs :as subs]
            [queues.events :as events]
            ))

;; layout of queues in rows alternating in direction, like mower
(defn mower [{:keys [nitems xstart ystart xspace yspace xmax ymax xmin]}]
  (loop [res []
         x   xstart
         y   ystart
         dir 1
         i   0]
    (if (< i nitems)
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

(defn center-of-rect [x y w h]
  [(+ x (/ w 2)) (+ y (/ h 2))])

(defn agent-rect
  [id i-pos]
  (let [open  @(rf/subscribe [:agent-open? id])
        color (if open "#80ffaa" "red")
        busy  @(rf/subscribe [:agent-busy? id])
        x     (* i-pos 100)
        agtrect (rect id x 0 90 48  color
                   #(rf/dispatch [:agent-toggle-open %]))]
    (if busy
      (seq [agtrect (circle (:id busy) (center-of-rect x 0 90 48) 4)])
      agtrect)))

(defn agent-elt
  []
  (fn []
     [:svg {:style {:border "thin solid black"}
           :width 1000 :height 50}
      (doall
       (map-indexed #(agent-rect %2 %1)
                    @(rf/subscribe [:agent-ids])))]))

(defn emptyq? [q]
  (not (peek q)))

(defn pcircles
  [ps x]
  (doall
   (map #(circle (:id %1) %2 6) ps
        (mower {:nitems (count ps)
                :xtart (+ x 2)
                :ystart 2
                :xspace 15
                :yspace 20
                :xmax 173
                :ymax 190
                :xmin 10}))))

(defn sink-rect
  [id ipos]
  (let [ps @(rf/subscribe [:occupied id])
        x (* ipos 200)
        sinkrect (rect id x 0 180 196 "yellow" prn)]
    (if (emptyq? ps)
      sinkrect
      (into [sinkrect] (pcircles ps x)))))

(defn sink-elt
  "Creates set of sink elts with ids from db"
  []
  (fn []
    [:svg {:style {:border "thin solid black"}
           :width 1000 :height 200}
     (doall
      (map-indexed #(sink-rect %2 %1) @(rf/subscribe [:sink-ids])))]))

(defn display-queued
  []
  (let [queue @(rf/subscribe [:queued])]
    (doall
     (map #(circle (:id %1) %2 6) queue
          (mower {:nitems (count queue)
                  :xstart 500
                  :ystart 14
                  :xspace 15
                  :yspace 20
                  :xmax 990
                  :ymax 290
                  :xmin 15})))))

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

(def HGAP1 [c/gap :size "10px"])

(defn title-area []
  [c/h-box
   :margin "10px"
   :gap "5px"
   :children [[c/label :label "No. queued"]
              [c/label :label (str (count @(rf/subscribe [:queued])))]
              HGAP1
              [c/label :label "Not yet arrived"]
              [c/label :label (str (count @(rf/subscribe [:psgrs])))]]])

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
