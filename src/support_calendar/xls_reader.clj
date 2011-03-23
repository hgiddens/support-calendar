(ns support-calendar.xls-reader
  (import [java.text SimpleDateFormat]
          [java.util Calendar TimeZone]
          [org.apache.poi.ss.usermodel Cell])
  (require [clojure.string :as string]))

(def roster-sheet-name-pattern #"(?i)jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec\d{2}")

(def utc (TimeZone/getTimeZone "utc"))

(def roster-date-format (doto (new SimpleDateFormat "MMMyy")
                          (.setTimeZone utc)))

(defn range-from [n]
  (drop n (range)))

(defn sheet-columns [sheet]
  (for [column-index (range-from 2)
        :let [value (-> sheet (.getRow 1) (.getCell column-index) (.getStringCellValue))]
        :while (not= value "Date")
        :when (not (string/blank? value))]
    [value column-index]))

(defn sheet-days [sheet]
  (let [sheet-date (doto (Calendar/getInstance utc)
                     (.setTime (.parse roster-date-format (.getSheetName sheet))))]
    (for [day-index (range (.getActualMaximum sheet-date Calendar/DAY_OF_MONTH))]
      (doto (.clone sheet-date)
        (.set Calendar/DAY_OF_MONTH (inc day-index))))))

(defn system-cells [sheet column-index]
  (for [row-index (range 2 33)]
    (-> sheet (.getRow row-index) (.getCell column-index) (.getStringCellValue))))

(defn workbook-sheets [workbook]
  (for [index (range (.getNumberOfSheets workbook))]
    (.getSheetAt workbook index)))

(defn roster-sheet? [sheet]
  (re-find roster-sheet-name-pattern (.getSheetName sheet)))

(defn valid-event? [[name system date]]
  (not (string/blank? name)))

(defn inc-day [day]
  (doto (.clone day)
    (.add Calendar/DAY_OF_MONTH 1)))

(defn roster-sheets [workbook]
  "Returns a seq of the worksheets in workbook that have roster information."
  (filter roster-sheet? (workbook-sheets workbook)))

(defn sheet-events
  "Returns a seq of the events in a worksheet.

Events are a vector of [name, system, date]."
  [sheet]
  (let [days (sheet-days sheet)
        process-column (fn [[system column-index]]
                         (filter valid-event?
                                 (map vector
                                      (system-cells sheet column-index)
                                      (repeat system)
                                      days
                                      (map inc-day days))))]
    (mapcat process-column (sheet-columns sheet))))
