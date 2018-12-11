(ns clojurians-slack-log.core
  (:require [clj-http.client :as http]
            [net.cgrand.enlive-html :as html]
            [clojure.string :as string]
            [clojure.java.io :as io]))

;;; Define URLS
(defonce url-index "https://clojurians-log.clojureverse.org/")
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
  (:body (http/get url)))

;;; get all channels
;;; https://clojurians-log.clojureverse.org/
(defn all-channels
  "Extract all channels from index page."
  []
  (map #(hash-map
         :name (string/replace (html/text %) "# " "") ; channel name: "# clojure" -> "clojure"
         :url (str url-index ; construct to complete URL
                   (first (html/attr-values % :href))))
       (html/select (html/html-snippet (fetch-html url-channels))
                    [:div.main :ul :li :a])))

(comment (all-channels))

;;; parse channel page
;;; https://clojurians-log.clojureverse.org/beginners
(defn channel-log-dates
  "Extract all date links in channel log page."
  [channel-url]
  (map #(let [date (html/text %) ; date
              url  (str url-index                ; construct to complete URL
                        (first (html/attr-values % :href)))]
          {:date date :url url})
       (html/select
        (html/html-snippet (fetch-html url-channel-beginners))
        [:div.main :ul :li :a])))

(comment
  (channel-log-dates url-channel-beginners)
  (channel-log-dates (first (all-channels))))

;;; parse channel date log page's message history
;;; https://clojurians-log.clojureverse.org/beginners/2018-12-02
(defn channel-date-log
  "Extract message history from channel's date log."
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
  (map #(map (fn [message]
               (let [filename (str "log_files/" channel-name ".txt")]
                 (io/make-parents filename)
                 (spit filename
                       (compose-message message)
                       :append true)))
             (channel-date-log %))
       (map :url (channel-log-dates channel-url)))
  (prn (format "Log of channel %s: finished." channel-name)))

(comment
  (channel-messages {:name "beginners" :url url-channel-beginners}))

(defn -main
  "Run the crawler program."
  []
  (map channel-messages (all-channels)))
