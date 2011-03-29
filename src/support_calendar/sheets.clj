(ns support-calendar.sheets
  (:import [java.io InputStream]
           [org.apache.poi.hssf.usermodel HSSFWorkbook]
           [org.apache.poi.ss.usermodel Cell])
  (:require [clojure.java.io :as io]))

(defn workbook [source]
  (with-open [in (io/input-stream source)]
    (new HSSFWorkbook in)))

(defn sheets [workbook]
  (for [index (range (.getNumberOfSheets workbook))]
    (.getSheetAt workbook index)))

(defn sheet-name [sheet]
  (.getSheetName sheet))

(defn columns [sheet]
  (let [all-rows (seq sheet)
        last-cell (apply max (map (memfn getLastCellNum) all-rows))]
    (for [column (range last-cell)]
      (for [row all-rows]
        (.getCell row column)))))

(defmulti cell-value (memfn getCellType))
(defmethod cell-value Cell/CELL_TYPE_STRING [cell]
  (.getStringCellValue cell))
(defmethod cell-value Cell/CELL_TYPE_BLANK [cell]
  nil)
