(defproject support-calendar "1"
  :description "Generates iCalendar files from the support roster"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.samba.jcifs/jcifs "1.2.19"]
                 [org.apache.poi/poi "3.7"]
                 ;; ical4j
                 ;; compojure
                 ;; ring-jetty-adapter
                 ;; hiccup
                 ]
  :dev-dependencies [[swank-clojure "1.2.1"]])
