(ns support-calendar.server
  (import [java.io ByteArrayInputStream ByteArrayOutputStream]
          [java.util.concurrent Executors TimeUnit]
          [jcifs.smb SmbFile]
          [net.fortuna.ical4j.data CalendarOutputter])
  (require [clojure.java.io :as io]
           [support-calendar.sheets :as sheets])
  (use compojure.core
       compojure.route
       [hiccup.core :only [html]]
       [ring.adapter.jetty :only [run-jetty]]
       [ring.util.codec :only [url-encode]]
       [support-calendar.events :only [collapse-date-ranges]]
       [support-calendar.generator :only [generate-calendar]]
       [support-calendar.xls-reader :only [roster-events]]))

(extend SmbFile io/IOFactory
  (assoc io/default-streams-impl
    :make-input-stream (fn [x opts] (.getInputStream x))
    :make-output-stream (fn [x opts] (.getOutputStream x))))

(def roster-path "smb://flroa01/shared/general/intranet/shared-documents/support roster.xls")

(defonce events (atom []))

(defn systems [events]
  (distinct (map second events)))

(defn people [events]
  (distinct (map first events)))

(defn webcal-url [request & paths]
  (apply str "webcal://" (request :server-name) ":" (request :server-port) paths))

(defn calendar-response [events]
  {:status 200
   :headers {"Content-Type" "text/calendar"}
   :body (let [calendar (generate-calendar events)
               outputter (new CalendarOutputter true)
               stream (new ByteArrayOutputStream)]
           (.output outputter calendar stream)
           (new ByteArrayInputStream (.toByteArray stream)))})

(defn link-page [title links]
  (html
   [:html
    [:head [:title title]]
    [:body
     [:ul
      (for [[link-title link-url] links]
        [:li [:a {:href link-url} link-title]])]]]))

(defroutes calendar-routes
  (GET "/" request
    (link-page "Calendars"
      [["All systems and people" (webcal-url request "/all")]
       ["Calendars by system" "systems/"]
       ["Calendars by support person" "people/"]]))
  (GET "/all" []
    (calendar-response @events))
  (GET "/people/" request
    (link-page "Calendars by support person"
      (for [person (sort (people @events))]
        [person (webcal-url request "/people/" (url-encode person))])))
  (GET "/people/:person" [person]
    (let [ev @events]
      (when (some (partial = person) (people ev))
        (calendar-response (filter (fn [[name event-system start end]]
                                     (= name person))
                                   ev)))))
  (GET "/systems/" request
    (link-page "Calendars by system"
      (for [system (sort (systems @events))]
        [system (webcal-url request "/systems/" (url-encode system))])))
  (GET "/systems/:system" [system]
    (let [ev @events]
      (when (some (partial = system) (systems ev))
        (calendar-response (filter (fn [[name event-system start end]]
                                     (= event-system system))
                                   ev)))))
  (not-found "Calendar not found."))

(defn read-events [& old-events]
  (-> (new SmbFile roster-path) (sheets/workbook) (roster-events) (collapse-date-ranges)))

(defn update-events []
  (swap! events read-events))

(defn start-server []
  (update-events)
  [(run-jetty #'calendar-routes {:port 8080 :join? false})
   (doto (Executors/newSingleThreadScheduledExecutor)
     (.scheduleAtFixedRate update-events 5 5 TimeUnit/MINUTES))])
