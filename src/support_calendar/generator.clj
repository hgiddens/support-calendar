(ns support-calendar.generator
  (import [net.fortuna.ical4j.model Calendar Date]
          [net.fortuna.ical4j.model.component VEvent]
          [net.fortuna.ical4j.model.property CalScale ProdId Version]
          [net.fortuna.ical4j.util UidGenerator]))

(defn to-date [calendar]
  (new Date (.getTime calendar)))

(defn generate-calendar [events]
  (let [calendar (new Calendar)
        generator (new UidGenerator "1")]
    (doto (.getProperties calendar)
      (.add (new ProdId "-//NZRB//support-calendar//EN"))
      (.add Version/VERSION_2_0)
      (.add CalScale/GREGORIAN))
    (doseq [[name system start end] events]
      (let [event (new VEvent (to-date start) (to-date end) (str system " support: " name))]
        (.add (.getProperties event) (.generateUid generator))
        (.add (.getComponents calendar) event)))
    calendar))
