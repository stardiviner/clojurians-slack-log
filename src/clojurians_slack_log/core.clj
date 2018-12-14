(ns clojurians-slack-log.core
  (:require [clj-http.client :as http]
            [net.cgrand.enlive-html :as html]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [taoensso.carmine :as redis :refer (wcar)])
  (:import (java.lang Thread)))

;;; Define URLS
(defonce url-index "https://clojurians-log.clojureverse.org")
(defonce url-channels "https://clojurians-log.clojureverse.org/")
;;; favourite channels
(defonce url-channel-beginners "https://clojurians-log.clojureverse.org/beginners")
(defonce url-channel-clojure "https://clojurians-log.clojureverse.org/clojure")
(defonce url-channel-clojurescript "https://clojurians-log.clojureverse.org/clojurescript")
(defonce url-channel-sql "https://clojurians-log.clojureverse.org/sql")
(defonce url-channel-data-science "https://clojurians-log.clojureverse.org/data-science")
(defonce url-channel-datavis "https://clojurians-log.clojureverse.org/datavis")
(defonce url-channel-cider "https://clojurians-log.clojureverse.org/cider")
(defonce url-channel-emacs "https://clojurians-log.clojureverse.org/emacs")
(defonce url-channel-ring "https://clojurians-log.clojureverse.org/ring")
(defonce url-channel-reagent "https://clojurians-log.clojureverse.org/reagent")

(defn fetch-html
  "A helper function to fetch URL's page as HTML"
  [url]
  (try
    (:body (http/get url {:follow-redirects true}))
    (catch Exception e
      (println (format "The URL: %s is not available." url)))))

(comment
  (http/get "https://clojurians-log.clojureverse.org/beginners/2018-10-27")
  (fetch-html "https://clojurians-log.clojureverse.org/beginners/2018-10-27"))

;;; get all channels
;;; https://clojurians-log.clojureverse.org/
(defn get-all-channels
  "Extract all channels from index page and return map."
  []
  (map #(hash-map
         :name (string/replace (html/text %) "# " "") ; channel name: "# clojure" -> "clojure"
         :url (str url-index ; construct to complete URL
                   (first (html/attr-values % :href))))
       (html/select (html/html-snippet (fetch-html url-channels))
                    [:div.main :ul :li :a])))

(defonce all-channels-list (doall (get-all-channels)))

(comment (get-all-channels))

;;; parse channel page
;;; https://clojurians-log.clojureverse.org/beginners
(defn channel-log-dates
  "Extract all date links in channel log page and return map."
  [channel-url]
  (map #(let [date (html/text %) ; date
              url  (str url-index                ; construct to complete URL
                        (first (html/attr-values % :href)))]
          {:date date :url url})
       (html/select
        (html/html-snippet (fetch-html channel-url))
        [:div.main :ul :li :a])))

(comment
  (channel-log-dates (first all-channels-list)))

;;; parse channel date log page's message history
;;; https://clojurians-log.clojureverse.org/beginners/2018-12-02
(defn channel-date-log
  "Extract message history from channel's date log and return map."
  [date-url]
  (map
   #(let [username  (html/text (first (html/select % [:a.message_username])))
          timestamp (html/text (first (html/select % [:span.message_timestamp :a])))
          content   (html/text (second (html/select % [:p])))]
      {:username  username
       :timestamp timestamp
       :content   content})
   (html/select                         ; all messages
    (html/html-snippet (fetch-html date-url))
    [:div.message-history :div.message])))

;;; extract info from all messages
(comment
  (channel-date-log "https://clojurians-log.clojureverse.org/beginners/2018-12-02"))

(defn compose-message
  "Compose username, timestamp and message content into a formatted message."
  [{username :username timestamp :timestamp content :content}]
  (str "---------------------------------------------------------------\n"
       (str "> " username "  " timestamp "\n")
       "---------------------------------------------------------------\n"
       (str content "\n")))

;;; write all extracted messages into channel-named file.
(defn channel-messages
  "Extract all message a in channel's all dates and write into channel-named file."
  [{channel-name :name channel-url :url}]
  (doseq [date (map #(->> %
                          :date
                          (re-find #"\d{4}-\d{2}-\d{2}"))
                    (channel-log-dates channel-url))]
    (let [filename (str (format "log_files/%s/%s" channel-name date) ".txt")]
      (io/make-parents filename)
      (doseq [message (channel-date-log (str channel-url "/" date))]
        (spit filename (compose-message message) :append true))))
  (prn (format "Channel [%s] exported." channel-name))
  (Thread/sleep (* 2 1000)))

(comment
  (channel-messages {:name "datavis" :url url-channel-datavis}))

;;; crawler sequence with Redis.
(defonce redis-conn-pool {:pool {}
                          :spec {:host "127.0.0.1" :port 6379}})

(defmacro wcar* [& body] `(wcar redis-conn-pool ~@body))

;;; detect Redis started?
(defn check-redis-alive
  "Check Redis server alive?"
  []
  (try
    (wcar* (redis/ping))
    (catch java.net.ConnectException e
      (println "Can't connect to Redis server, it might not started, please start it."))
    (catch Exception e
      (println "Can't access Redis server, check it out."))
    (finally true)))

(comment
  (wcar*
   (redis/lset :slack/channels all-channels-list))
  (wcar*
   (redis/lpush :slack/kk ["a" "b"])))

(defn -main
  "Run the crawler program."
  []
  (when (check-redis-alive)
    (run! io/delete-file (fs/glob (java.io.File. "log_files/") "*.txt"))
    (doseq [channel-url all-channels-list]
      (channel-messages channel-url))))
