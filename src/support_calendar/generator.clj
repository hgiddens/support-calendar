(ns support-calendar.generator
  (import [net.fortuna.ical4j.model Calendar Date]
          [net.fortuna.ical4j.model.component VEvent]
          [net.fortuna.ical4j.model.property CalScale Comment ProdId Version]
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
    (doseq [[details system start end] events]
      (let [event (new VEvent (to-date start) (to-date end) (str system " support: " (details :name)))]
        (doto (.getProperties event)
          (.add (.generateUid generator))
          (.add (new Comment (with-out-str
                               (when-let [extension (details :extension)]
                                 (println "Extension:" extension))
                               (when-let [phones (details :phone)]
                                 (println "Phone:" (join ", " phones)))))))
        (.add (.getComponents calendar) event)))
    calendar))
