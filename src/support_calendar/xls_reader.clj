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
