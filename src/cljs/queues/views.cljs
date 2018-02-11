(ns queues.views
  (:require [re-frame.core :as rf]
            [re-com.core :as c]
            [queues.subs :as subs]
            [queues.events :as events]
            [goog.string :as gstring]
            [goog.string.format]
            ))

(declare secs-to-hms)

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
            :style {:fill @(rf/subscribe [:psgr-to-color id])
                    :stroke-width 1
                    :stroke :black}
            :on-click #(prn (-> % .-target .-id))
            :cx cx
            :cy cy
            :r r}])

;; actionfns specified as map fns of 0 arity, viz. {:on-click #(clickfn id)}
(defn rect
  [id x y w h color actionfns]
  [:rect (merge {:id id
                 :key id
                 :style {:fill color ;; "#80ffaa" ;; "rgba(200,128,128,0.4)"
                         :stroke-width 2
                         :stroke :black}
                 :x x
                 :y y
                 :width w
                 :height h} actionfns)])

(defn center-of-rect [x y w h]
  [(+ x (/ w 2)) (+ y (/ h 2))])

(defn agent-rect
  [id i-pos]
  (let [open  @(rf/subscribe [:agent-open? id])
        color (if open "#80ffaa" "red")
        busy  @(rf/subscribe [:agent-working-on id])
        x     (* i-pos 100)
        agtrect (rect id x 0 90 48  color
                      {:on-click #(rf/dispatch [:agent-toggle-open id])})]
    (if busy
      (seq [agtrect (circle (:id busy) (center-of-rect x 0 90 48) 4)])
      agtrect)))

(defn agent-elt
  []
  (fn []
     [:svg {:width 1000 :height 50}
      (doall
       (map-indexed #(agent-rect %2 %1)
                    @(rf/subscribe [:agent-ids])))]))

(defn- emptyq? [q]
  (not (peek q)))

(defn pcircles
  [ps x]
  (doall
   (map #(circle (:id %1) %2 4) ps
        (mower {:nitems (count ps)
                :xstart x
                :ystart 6
                :xspace 10
                :yspace 10
                :xmax (+ x 176)
                :ymax 190
                :xmin x}))))

#_(defn prn-sink [sinkid]
  (prn @(rf/subscribe [:sink sinkid])))

(defn avg-delay
  "given a sinkid, calc the avg delay of psgrs in the sink's occupied queue"
  [sinkid]
  (let [queue @(rf/subscribe [:occupied sinkid])
        n     (count queue)
        delay (fn [psgr] (- (:processed-at psgr) (:arrived-at psgr)))]
    (if (zero? n)
      "Nothing queued yet"
      (secs-to-hms(/ (reduce + (map #(delay %) queue)) n)))))

(defn all-avg-delay
  []
  (let [sinkids @(rf/subscribe [:sink-ids])]
    (map avg-delay sinkids)))



(defn sink-rect
  [id ipos]
  (let [ps @(rf/subscribe [:occupied id])
        x (* ipos 200)
        sinkrect (rect id x 0 180 196 "lightcyan"
                       {:on-click #((comp prn avg-delay) id)})]
    (if (emptyq? ps)
      sinkrect
      (seq [sinkrect  (pcircles ps x)]))))

(defn sink-elt
  "Creates set of sink elts with ids from db"
  []
  (fn []
    [:svg {:width 1000 :height 200}
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

(def speeds [{:id 1 :label "1x"}
             {:id 10 :label "10x"}
             {:id 25 :label "25x"}
             {:id 50 :label "50x"}])

(defn title-area []
  [c/h-box
   :margin "10px"
   :gap "10px"
   :children [[c/label :label (str "No. queued: " (count @(rf/subscribe [:queued])))
               :style {:border "solid black 1px" :padding "2px"}]
              [c/label :label (str "Not yet arrived: " (count @(rf/subscribe [:psgrs])))
               :style {:border "solid black 1px" :padding "2px"}]
              [c/single-dropdown :width "auto"
               :style {:border "solid black 1px" :padding "2px"}
               :model @(rf/subscribe [:speedup])
               :choices speeds
               :id-fn #(:id %)
               :label-fn #(str "Clock multiplier: " (:label %))
               :on-change (fn [val] (rf/dispatch [:speedup-change val]))
               ]
              [c/label :label (str "Max queue len: "
                                   @(rf/subscribe [:max-qlength]))
               :style {:border "solid black 1px" :padding "2px"}]
              [c/popover-anchor-wrapper
               :showing? (rf/subscribe [:info-showing?])
               :position :below-center
               :anchor [c/button :label "info"
                        :on-click #(rf/dispatch [:toggle-info])]
               :popover [c/popover-content-wrapper :body (into [:p] (interleave
                                                                     (all-avg-delay)
                                                                     (repeat [:br])))]]]])

(defn secs-to-hms
 [secs]
  (let [h (Math/floor(/ secs 3600))
        excess (rem secs 3600)
        m (Math/floor (/ excess 60))
        s (rem excess 60)]
    (gstring/format "%02d:%02d:%02d" h m s)))

(defn sink-labels
  []
  [c/h-box
   :margin "10px"
   :gap "20px"
   :size "auto"
   :width "80%"
   :justify :around
   :children [[c/label :label (str (secs-to-hms
                                    @(rf/subscribe [:scheduled :sink0])))
               :style {:color "red" :font-weight "bold"}]
              [c/label :label (str (secs-to-hms
                                    @(rf/subscribe [:scheduled :sink1])))
               :style {:color "orange"}]
              [c/label :label (secs-to-hms
                               @(rf/subscribe [:scheduled :sink2]))
               :style {:color "green" :font-weight "bold"}]
              [c/label :label (secs-to-hms
                               @(rf/subscribe [:scheduled :sink3]))
               :style {:color "blue" :font-weight "bold"}]
              [c/label :label (secs-to-hms
                               @(rf/subscribe [:scheduled :sink4]))
               :style {:color "magenta" :font-weight "bold"}]]])

(defn main-panel []
  [c/v-box
   :height "100%"
   :children [[title-area]
              [c/gap :size "15px"]
              [c/h-box
               :margin "10px"
               :children [[c/button :label "Start/Stop"
                           :style {:background-color "lightblue"}
                           :on-click #(rf/dispatch [:start-stop])]
                          [c/gap :size "15px"]
                          [c/button :label "Reset"
                           :style {:background-color "LightPink"}
                           :on-click #(rf/dispatch [:initialize-db])]
                          [c/gap :size "15px"]
                          [c/title
                           :level :level2
                           :label (secs-to-hms @(rf/subscribe [:clock]))]]]
              [c/line]
              [c/gap :size "15px"]
              [sink-labels][sink-area] [agent-area] [queuing-area]]])
