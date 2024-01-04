(ns wiki-guides.store
  (:require [promesa.core :as p]))

(def db-name "wiki-guides")
(def db-version 1)

(def pages-store "pages")
(def pages-store-key "href")

(def guides-store "guides")
(def guides-store-key "id")

(defn event->result [event]
  (-> event .-target .-result))

(defn event->error [event]
  (-> event .-target .-error))

(defn promise-error-handler [p]
  (fn [event]
    (->> event event->error (p/reject! p))))

(defn with-txn
  ([db store-name f] (with-txn db store-name f false))
  ([db store-name f writeable?] (with-txn (p/deferred) db store-name f writeable?))
  ([p db store-name f writeable?]
   (let [tx (.transaction db store-name (if writeable? "readwrite" "readonly"))
         store (.objectStore tx store-name)
         tx-req (f store)]
     (set! (. tx-req -onsuccess) #(p/resolve! p (event->result %)))
     (set! (. tx-req -onerror) (promise-error-handler p))
     p)))

(defn with-open-db+txn
  ([store-name f] (with-open-db+txn store-name f false))
  ([store-name f writeable?]
   (let [p (p/deferred)
         open-req (.open js/indexedDB db-name db-version)]
     (set! (. open-req -onsuccess)
           (fn [event]
             (let [db (event->result event)]
               (with-txn p db store-name f writeable?))))
     (set! (. open-req -onerror) (promise-error-handler p))
     p)))

(defn record-merge [l r]
  (cond
    (nil? l) r
    (nil? r) l
    (and (sequential? l)
         (sequential? r)) (-> (into l r) (distinct) (vec))
    :else r))

(def guide-init-data
  #js[#js{:href "/wikis/the-legend-of-zelda-breath-of-the-wild"
          :title "The Legend of Zelda: Breath of the Wild"
          :icon "https://assets-prd.ignimgs.com/2022/06/14/zelda-breath-of-the-wild-1655249167687.jpg?width=240&crop=1%3A1%2Csmart&auto=webp"}
      #js{:href "/wikis/the-legend-of-zelda-tears-of-the-kingdom"
          :title "The Legend of Zelda: Tears of the Kingdom"
          :icon "https://assets-prd.ignimgs.com/2022/09/14/zelda-tears-of-the-kingdom-button-2k-1663127818777.jpg?width=240&crop=1%3A1%2Csmart&auto=webp"}
      #js{:href "/wikis/hogwarts-legacy"
          :title "Hogwarts Legacy"
          :icon "https://assets-prd.ignimgs.com/2022/05/24/hogwarts-legacy-button-fin-1653421326559.jpg?width=240&crop=1%3A1%2Csmart&auto=webp"}])

(defn init! []
  (let [open-req (.open js/indexedDB db-name db-version)]
    (set! (. open-req -onupgradeneeded)
          (fn [event]
            (let [db (event->result event)
                  page-store (.createObjectStore db pages-store #js{:keyPath pages-store-key})
                  guide-store (.createObjectStore db guides-store #js{:keyPath guides-store-key
                                                                      :autoIncrement true})]
              (.createIndex page-store "aliases" "aliases" #js{:multiEntry true})
              (.createIndex page-store "title" "title")

              (.createIndex guide-store "aliases" "aliases" #js{:multiEntry true})
              (.createIndex guide-store "title" "title")

              (set! (.. guide-store -transaction -oncomplete)
                    (fn [_]
                      (with-txn db guides-store
                                (fn [store]
                                  (->> (for [guide guide-init-data]
                                         (.add store guide))
                                       (p/all)))
                                true))))))))
