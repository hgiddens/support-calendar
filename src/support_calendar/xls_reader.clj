(ns support-calendar.xls-reader
  (import [java.text SimpleDateFormat]
          [java.util Calendar TimeZone])
  (require [clojure.string :as string]
           [support-calendar.sheets :as sheets]))

(def roster-sheet-name-pattern #"(?i)jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec\d{2}")

(def utc (TimeZone/getTimeZone "utc"))

(def roster-date-format (doto (new SimpleDateFormat "MMMyy")
                          (.setTimeZone utc)))

(defn range-from [n]
  (drop n (range)))

(defn take-range [seq s e]
  (take (- e s) (drop s seq)))

(defn sheet-systems [sheet]
  (for [column (drop 2 (sheets/columns sheet))
        :let [value (sheets/cell-value (second column))]
        :while (not= value "Date")
        :when (not (string/blank? value))]
    [value column]))

(defn sheet-days [sheet]
  (let [sheet-date (doto (Calendar/getInstance utc)
                     (.setTime (.parse roster-date-format (sheets/sheet-name sheet))))]
    (for [day-index (range (.getActualMaximum sheet-date Calendar/DAY_OF_MONTH))]
      (doto (.clone sheet-date)
        (.set Calendar/DAY_OF_MONTH (inc day-index))))))

(defn system-cells [column]
  (map sheets/cell-value (take-range column 2 33)))

(defn roster-sheet? [sheet]
  (re-find roster-sheet-name-pattern (sheets/sheet-name sheet)))

(defn valid-event? [event]
  (not (string/blank? (:name (:person event)))))

(defn inc-day [day]
  (doto (.clone day)
    (.add Calendar/DAY_OF_MONTH 1)))

(defn roster-sheets [workbook]
  "Returns a seq of the worksheets in workbook that have roster information."
  (filter roster-sheet? (sheets/sheets workbook)))

(defn valid-contact-details? [person]
  (not (or (string/blank? (:name person))
           (string/blank? (:initials person))
           (string/blank? (:extension person))
           (every? string/blank? (:phone person)))))

(defn extract-people [sheet]
  (let [contact-details-layout [{:name 0
                                 :initials 2
                                 :extension 3
                                 :phone [4 5]}
                                {:name 7
                                 :initials 8
                                 :extension 9
                                 :phone [10 11]}
                                {:name 13
                                 :initials 14
                                 :extension 15
                                 :phone [16 17]}]
        get-val (fn [row index]
                  (let [defaulted-value (if-let [cell (.getCell row index)]
                                          (sheets/cell-value cell)
                                          "")]
                    (cond
                     (number? defaulted-value) (Integer/toString defaulted-value)
                     (string? defaulted-value) (string/trim defaulted-value)
                     :otherwise defaulted-value)))
        make-person (fn [row details]
                      (into {} (map (fn [[property index]]
                                      [property (if (coll? index)
                                                  (filter (complement string/blank?) (map (partial get-val row) index))
                                                  (get-val row index))])
                                    details)))]
    (into {} (for [row (drop 34 (sheets/rows sheet))
                   details contact-details-layout
                   :let [person (make-person row details)]
                   :when (valid-contact-details? person)]
               [(:initials person) person]))))

(defn sheet-events
  "Returns a seq of the events in a worksheet.

Events are a vector of [name, system, date]."
  [sheet]
  (let [days (sheet-days sheet)
        name-map (extract-people sheet)
        expand-name (fn [initials]
                      (or (name-map initials) {:name initials, :initials initials}))
        process-column (fn [[system column]]
                         (filter valid-event?
                                 (map (fn [initials day]
                                        {:person (expand-name initials)
                                         :system system
                                         :start day
                                         :end (inc-day day)})
                                      (system-cells column)
                                      days)))]
    (mapcat process-column (sheet-systems sheet))))

(defn roster-events [workbook]
  (mapcat sheet-events (roster-sheets workbook)))

