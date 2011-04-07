(ns support-calendar.events)

(defn merge-events [[last-event & rest :as all] event]
  (if (= (:start event) (:end last-event))
    (cons (assoc last-event :end (:end event)) rest)
    (cons event all)))

(defn collapse-date-ranges [events]
  (assert (not (empty? events)))
  ;; assert date ranges sorted and non-overlapping
  (let [grouped (group-by (partial take 2) events)]
    (mapcat (fn [[_ grouped-events]]
              (reverse (reduce merge-events (take 1 grouped-events) (rest grouped-events))))
            grouped)))
