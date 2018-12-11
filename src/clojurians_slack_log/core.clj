(ns clojurians-slack-log.core
  (:require [clj-http.client :as http]
            [net.cgrand.enlive-html :as html]
            [clojure.string :as string]
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
         (string/replace (html/text %) "# " "") ; channel name: "# clojure" -> "clojure"
         (str url-index ; construct to complete URL
              (first (html/attr-values % :href))))
       (html/select (html/html-snippet (fetch-html url-channels))
                    [:div.main :ul :li :a])))

(comment (all-channels))

