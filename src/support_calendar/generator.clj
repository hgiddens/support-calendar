(ns support-calendar.generator
  (import [net.fortuna.ical4j.model Calendar Date]
          [net.fortuna.ical4j.model.component VEvent]
          [net.fortuna.ical4j.model.property CalScale Description ProdId Version]
          [net.fortuna.ical4j.util UidGenerator])
  (require [support-calendar.events :as ev]))

(defn to-date [calendar]
  (new Date (.getTime calendar)))

(defn generate-calendar [events]
  (let [calendar (new Calendar)
        generator (new UidGenerator "1")]
    (doto (.getProperties calendar)
      (.add (new ProdId "-//NZRB//support-calendar//EN"))
      (.add Version/VERSION_2_0)
      (.add CalScale/GREGORIAN))
    (doseq [{:keys [person system start end] :as event-map} events]
      (let [event (new VEvent (to-date start) (to-date end) (str system " support: " (:name person)))]
        (doto (.getProperties event)
          (.add (.generateUid generator))
          (.add (new Description (ev/contact-details-string event-map))))
        (.add (.getComponents calendar) event)))
    calendar))
