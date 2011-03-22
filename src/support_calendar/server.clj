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
       [support-calendar.core :only [render-calendar]]))

(def roster-path "smb://flroa01/shared/general/intranet/shared-documents/support roster.xls")

(def systems (sorted-set
              "Internet"
              "Jetbet/JESI"
              "IP Terms"
              "OFC"
              "JEQI"
              "Touchtone"
              "IPC"
              "Network"
              "IS"
              "DBA"))

(def workbook (atom nil))

(defroutes calendar-routes
  (GET "/systems/" request
    (html
     [:html
      [:head [:title "Calendars"]]
      [:body
       [:ul (for [system systems]
              [:li
               [:a {:href (str "webcal://"
                               (request :server-name) ":" (request :server-port)
                               "/systems/" (url-encode system))}
                system]])]]]))
  (GET "/systems/:system" [system]
    (when (contains? systems system)
      {:status 200
       :headers {"Content-Type" "text/calendar"}
       :body (let [calendar (render-calendar @workbook system)
                   outputter (new CalendarOutputter true)
                   stream (new ByteArrayOutputStream)]
               (.output outputter calendar stream)
               (new ByteArrayInputStream (.toByteArray stream)))}))
  (not-found "Calendar not found."))

(defn start-server []
  (let [get-workbook (fn [& old-workbook]
                       (->> (new SmbFile roster-path)
                            (.getInputStream)
                            (new HSSFWorkbook)))
        worksheet-updater (fn []
                            (swap! workbook get-workbook))]
    (worksheet-updater)
    [(run-jetty #'calendar-routes {:port 8080 :join? false})
     (doto (Executors/newSingleThreadScheduledExecutor)
       (.scheduleAtFixedRate worksheet-updater 5 5 TimeUnit/MINUTES))]))
