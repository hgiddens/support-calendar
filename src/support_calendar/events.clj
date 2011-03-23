(ns support-calendar.events)

(defn merge-events [[last-event & rest :as all] event]
  (let [start-date #(nth % 2)
        end-date #(nth % 3)
        merge-into (fn [target updater]
                     [(nth target 0) (nth target 1) (nth target 2) (nth updater 3)])]
    (if (= (start-date event) (end-date last-event))
      (cons (merge-into last-event event) rest)
      (cons event all))))

(defn collapse-date-ranges [events]
  (assert (not (empty? events)))
  ;; assert date ranges sorted and non-overlapping
  (let [grouped (group-by (partial take 2) events)]
    (mapcat (fn [[_ grouped-events]]
              (reverse (reduce merge-events (take 1 grouped-events) (rest grouped-events))))
            grouped)))
