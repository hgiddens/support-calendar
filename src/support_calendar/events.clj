(ns support-calendar.events
  (import [java.util Calendar]))

(defn split-date [[name system start-date]]
  (let [end-date (doto (.clone start-date)
                   (.add Calendar/DAY_OF_MONTH 1))]
    [name system start-date end-date]))

(defn merge-events [[last-event & rest :as all] event]
  (assert (= (count event) 4))
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
