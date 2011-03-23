(ns support-calendar.server
  (import [java.io ByteArrayInputStream ByteArrayOutputStream]
          [java.util.concurrent Executors TimeUnit]
          [jcifs.smb SmbFile]
          [net.fortuna.ical4j.data CalendarOutputter]
          [org.apache.poi.hssf.usermodel HSSFWorkbook])
  (use compojure.core
       compojure.route
       [hiccup :only [html]]
       [ring.adapter.jetty :only [run-jetty]]
       [ring.util.codec :only [url-encode]]
       [support-calendar.events :only [collapse-date-ranges]]
       [support-calendar.generator :only [generate-calendar]]
       [support-calendar.xls-reader :only [roster-events]]))

(def roster-path "smb://flroa01/shared/general/intranet/shared-documents/support roster.xls")

(defonce events (atom []))

(defn systems [events]
  (distinct (map second events)))

(defn people [events]
  (distinct (map first events)))

(defroutes calendar-routes
  (GET "/people/" request
    (html
     [:html
      [:head [:title "Calendars by person"]]
      [:body
       [:ul (for [person (sort (people @events))]
              [:li
               [:a {:href (str "webcal://"
                               (request :server-name)
                               ":"
                               (request :server-port)
                               "/people/" (url-encode person))}
                person]])]]]))
  (GET "/people/:person" [person]
    (let [ev @events]
      (when (some (partial = person) (people ev))
        {:status 200
         :headers {"Content-Type" "text/calendar"}
         :body (let [calendar (generate-calendar (filter (fn [[name event-system start end]]
                                                           (= name person))
                                                         ev))
                     outputter (new CalendarOutputter true)
                     stream (new ByteArrayOutputStream)]
                 (.output outputter calendar stream)
                 (new ByteArrayInputStream (.toByteArray stream)))})))
  (GET "/systems/" request
    (html
     [:html
      [:head [:title "Calendars by system"]]
      [:body
       [:ul (for [system (sort (systems @events))]
              [:li
               [:a {:href (str "webcal://"
                               (request :server-name) ":" (request :server-port)
                               "/systems/" (url-encode system))}
                system]])]]]))
  (GET "/systems/:system" [system]
    (let [ev @events]
      (when (some (partial = system) (systems ev))
        {:status 200
         :headers {"Content-Type" "text/calendar"}
         :body (let [calendar (generate-calendar (filter (fn [[name event-system start end]]
                                                           (= event-system system))
                                                         ev))
                     outputter (new CalendarOutputter true)
                     stream (new ByteArrayOutputStream)]
                 (.output outputter calendar stream)
                 (new ByteArrayInputStream (.toByteArray stream)))})))
  (not-found "Calendar not found."))

(defn read-events [& old-events]
  (collapse-date-ranges (roster-events (->> (new SmbFile roster-path)
                                            (.getInputStream)
                                            (new HSSFWorkbook)))))

(defn update-events []
  (swap! events read-events))

(defn start-server []
  (update-events)
  [(run-jetty #'calendar-routes {:port 8080 :join? false})
   (doto (Executors/newSingleThreadScheduledExecutor)
     (.scheduleAtFixedRate update-events 5 5 TimeUnit/MINUTES))])
