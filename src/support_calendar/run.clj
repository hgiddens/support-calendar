(ns support-calendar.run
  (:gen-class)
  (:use [support-calendar.server :only [start-server]]))

(def server (atom nil))

(defn -main [& args]
  (swap! server (constantly (start-server))))
