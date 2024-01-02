(ns wiki-guides.store
  (:require [indexed.db :as db]))

(def db-name "wiki-guides")
(def db-version 1)

(def pages-store "pages")
(def pages-store-key "href")

(defn init! []
  (let [open-req (.open js/indexedDB db-name db-version)]
    (set! (. open-req -onupgradeneeded) (fn [event]
                                          (let [db (-> event .-target .-result)
                                                store (.createObjectStore db pages-store #js{:keyPath pages-store-key})]
                                            (.createIndex store "title" "title"))))))

