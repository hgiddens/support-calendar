(defproject support-calendar "1"
  :description "Generates iCalendar files from the support roster"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.samba.jcifs/jcifs "1.2.19"]
                 [org.apache.poi/poi "3.7"]
                 [net.fortuna.ical4j/ical4j "1.0"]
                 [compojure "0.6.2"]
                 [ring/ring-jetty-adapter "0.3.7"]
                 [hiccup "0.3.4"]]
  :dev-dependencies [[swank-clojure "1.2.1"]]
  :main support-calendar.run
  :repositories {"modularity" {:url "http://m2.modularity.net.au/releases", :snapshots false}})
