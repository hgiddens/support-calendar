(ns support-calendar.core
  (import [java.util Calendar TimeZone]
          [net.fortuna.ical4j.model Date]
          [net.fortuna.ical4j.model.component VEvent]
          [net.fortuna.ical4j.model.property CalScale ProdId Version]
          [net.fortuna.ical4j.util UidGenerator])
  (require [clojure.string :as string]))

(defn find-column [sheet heading]
  "sheet string -> int"
  (let [row (.getRow sheet 1)]
    (loop [column 1]
      (condp = (-> row (.getCell column) (.getStringCellValue))
          heading column
          "Date" nil
          (recur (inc column))))))

(defn days-in-month [month year]
  (let [months {"jan" 31
                "mar" 31
                "apr" 30
                "may" 31
                "jun" 30
                "jul" 31
                "aug" 31
                "sep" 30
                "oct" 31
                "nov" 30
                "dec" 31}
        leap? (and (zero? (rem year 4))
                   (or (not (zero? (rem year 100)))
                       (zero? (rem year 400))))]
    (if (= month "feb")
      (if leap? 29 28)
      (months month))))

(defn sheet-date [sheet]
  (let [name (.getSheetName sheet)
        month (string/lower-case (subs name 0 3))
        year (+ 2000 (Integer/parseInt (subs name 3)))]
    (assert (contains? #{"jan" "feb" "mar" "apr" "may" "jun" "jul" "aug" "sep" "oct" "nov" "dec"} month))
    [month year]))

(defn column-people [sheet column]
  (for [row (map #(+ % 2) (range (apply days-in-month (sheet-date sheet))))]
    (-> sheet (.getRow row) (.getCell column) (.getStringCellValue))))

(defn process-sheet
  "sheet -> [[start end person] ...]"
  [sheet column]
  (let [[month year] (sheet-date sheet)]
    (filter (complement nil?)
            (map (fn [name day]
                   (when-not (string/blank? name)
                     [name [year month day]]))
                 (column-people sheet column)
                 (range 1 32)))))

(defn collapse-days [days]
  (map (fn [persons-days]
         (let [person (first (first persons-days))
               days (mapcat rest persons-days)]
           [person ((juxt first last) days)]))
       (partition-by first days)))

(defn roster-sheets [workbook]
  (let [sheet-name (fn [sheet] (.getSheetName sheet))
        matching-sheet? (fn [name]
                          (re-find #"(?i)jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec\d{2}" name))]
    (filter (comp matching-sheet? sheet-name)
            (map (fn [index] (.getSheetAt workbook index))
                 (range (.getNumberOfSheets workbook))))))

(def utc (TimeZone/getTimeZone "utc"))

(defn generate-calendar [data]
  (let [calendar (new net.fortuna.ical4j.model.Calendar)
        uidg (new UidGenerator "1")]
    (doto (.getProperties calendar)
      (.add (new ProdId "-//Hugh Giddens//support-calendar//EN"))
      (.add Version/VERSION_2_0)
      (.add CalScale/GREGORIAN))
    (doseq [[name [start end]] data]
      (let [start-date (let [[y m d] start
                             calendar (Calendar/getInstance utc)]
                         (doto calendar
                           (.set Calendar/YEAR y)
                           (.set Calendar/MONTH ({"jan" 0, "feb" 1, "mar" 2, "apr" 3
                                                            "may" 4, "jun" 5, "jul" 6, "aug" 7
                                                            "sep" 8, "oct" 9, "nov" 10, "dec" 11} m))
                           (.set Calendar/DAY_OF_MONTH d))
                         (new Date (.getTime calendar)))
            end-date (let [[y m d] end
                           calendar (Calendar/getInstance utc)]
                       (doto calendar
                         (.set Calendar/YEAR y)
                         (.set Calendar/MONTH ({"jan" 0, "feb" 1, "mar" 2, "apr" 3
                                                          "may" 4, "jun" 5, "jul" 6, "aug" 7
                                                          "sep" 8, "oct" 9, "nov" 10, "dec" 11} m))
                         (.set Calendar/DAY_OF_MONTH d)
                         (.add Calendar/DAY_OF_MONTH 1))
                       (new Date (.getTime calendar)))
            event (new VEvent start-date end-date name)]
        (.add (.getProperties event) (.generateUid uidg))
        (.add (.getComponents calendar) event)))
    calendar))

(defn render-calendar [workbook system]
  (->> workbook
       (roster-sheets)
       (mapcat #(process-sheet % (find-column % system)))
       (collapse-days)
       (generate-calendar)))

