(ns wiki-guides.store
  (:require [promesa.core :as p]))

(def db-name "wiki-guides")
(def db-version 1)

(def pages-store "pages")
(def pages-store-key "href")

(defn init! []
  (let [open-req (.open js/indexedDB db-name db-version)]
    (set! (. open-req -onupgradeneeded) (fn [event]
                                          (let [db (-> event .-target .-result)
                                                store (.createObjectStore db pages-store #js{:keyPath pages-store-key})]
                                            (.createIndex store "aliases" "aliases" #js{:multiEntry true})
                                            (.createIndex store "title" "title"))))))

(defn event->result [event]
  (-> event .-target .-result))

(defn event->error [event]
  (-> event .-target .-error))

(defn promise-error-handler [p]
  (fn [event]
    (->> event event->error (p/reject! p))))

(defn- with-txn
  ([f] (with-txn f false))
  ([f writable?]
   (let [p (p/deferred)
         open-req (.open js/indexedDB db-name db-version)]
     (set! (. open-req -onsuccess)
           (fn [event]
             (let [db (event->result event)
                   tx (.transaction db pages-store (if writable? "readwrite" "readonly"))
                   store (.objectStore tx pages-store)
                   tx-req (f store)]
               (set! (. tx-req -onsuccess) #(p/resolve! p (event->result %)))
               (set! (. tx-req -onerror) (promise-error-handler p)))))
     (set! (. open-req -onerror) (promise-error-handler p))
     p)))

(defn page-merge [l r]
  (cond
    (nil? l) r
    (nil? r) l
    (and (sequential? l)
         (sequential? r)) (-> (into l r) (distinct) (vec))
    :else r))

(defn add [obj]
  (-> (with-txn #(.get % (:href obj)))
      (p/then #(js->clj (or % {}) :keywordize-keys true))
      (p/then
        (fn [orig]
          (with-txn #(.put % (clj->js (merge-with page-merge orig obj))) true)))))

(defn fetch [href]
  (-> (p/all [(with-txn #(.get % href))
              (with-txn #(-> (.index % "aliases") (.get href)))])
      (p/then (fn [[record1 record2]]
                (some-> (or record1 record2) (js->clj :keywordize-keys true))))))


