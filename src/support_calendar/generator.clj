(ns support-calendar.generator
  (import [net.fortuna.ical4j.model Calendar Date]
          [net.fortuna.ical4j.model.component VEvent]
          [net.fortuna.ical4j.model.property CalScale Description ProdId Version]
          [net.fortuna.ical4j.util UidGenerator])
  (use [clojure.string :only [join]]))

(defn to-date [calendar]
  (new Date (.getTime calendar)))

(defn generate-calendar [events]
  (let [calendar (new Calendar)
        generator (new UidGenerator "1")]
    (doto (.getProperties calendar)
      (.add (new ProdId "-//NZRB//support-calendar//EN"))
      (.add Version/VERSION_2_0)
      (.add CalScale/GREGORIAN))
    (doseq [{:keys [person system start end]} events]
      (let [event (new VEvent (to-date start) (to-date end) (str system " support: " (:name person)))]
        (doto (.getProperties event)
          (.add (.generateUid generator))
          (.add (new Description (with-out-str
                                   (when-let [extension (:extension person)]
                                     (println "Extension:" extension))
                                   (when-let [phones (:phone person)]
                                     (println "Phone:" (join ", " phones)))))))
        (.add (.getComponents calendar) event)))
    calendar))
