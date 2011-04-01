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

(defn valid-event? [[name system date]]
  (not (string/blank? name)))

(defn inc-day [day]
  (doto (.clone day)
    (.add Calendar/DAY_OF_MONTH 1)))

(defn roster-sheets [workbook]
  "Returns a seq of the worksheets in workbook that have roster information."
  (filter roster-sheet? (sheets/sheets workbook)))

(defn sheet-events
  "Returns a seq of the events in a worksheet.

Events are a vector of [name, system, date]."
  [sheet]
  (let [days (sheet-days sheet)
        process-column (fn [[system column]]
                         (filter valid-event?
                                 (map vector
                                      (system-cells column)
                                      (repeat system)
                                      days
                                      (map inc-day days))))]
    (mapcat process-column (sheet-systems sheet))))

(defn roster-events [workbook]
  (mapcat sheet-events (roster-sheets workbook)))

(defn extract-people [sheet]
  (let [person-details-things [{:name 0
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
        rows (drop 34 sheet)
        get-val (fn [row index]
                  (let [v (if-let [cell (.getCell row index)] (sheets/cell-value cell) "")]
                    (cond
                     (number? v) (Integer/toString v)
                     (string? v) (string/trim v)
                     :otherwise v)))
        make-person (fn [row details]
                      (into {} (map (fn [[k v]]
                                      [k (if (coll? v)
                                           (filter (complement string/blank?) (map (partial get-val row) v))
                                           (get-val row v))])
                                    details)))
        valid-person? (fn [person]
                        (not (or (string/blank? (:name person))
                                 (string/blank? (:initials person))
                                 (string/blank? (:extension person))
                                 (every? string/blank? (:phone person)))))]
    (for [row rows
          details person-details-things
          :let [person (make-person row details)]
          :when (valid-person? person)]
      person)))
