(ns wiki-guides.store
  (:require [promesa.core :as p]))

(def db-name "wiki-guides")
(def db-version 2)

(def pages-store-name "pages")
(def pages-store-key "id")

(def guides-store-name "guides")
(def guides-store-key "id")

(defn event->result [event]
  (-> event .-target .-result))

(defn event->error [event]
  (-> event .-target .-error))

(defn promise-error-handler [p]
  (fn [event]
    (->> event event->error (p/reject! p))))

(defn request->promise
  ([tx-req] (request->promise (p/deferred) tx-req))
  ([p tx-req]
   (set! (. tx-req -onsuccess) #(p/resolve! p (event->result %)))
   (set! (. tx-req -onerror) (promise-error-handler p))
   p))

(defn with-txn
  ([db store-name f] (with-txn db store-name f false))
  ([db store-name f writeable?] (with-txn (p/deferred) db store-name f writeable?))
  ([p db store-name f writeable?]
   (let [tx (.transaction db store-name (if writeable? "readwrite" "readonly"))
         store (.objectStore tx store-name)
         tx-req (f store)]
     (if (sequential? tx-req)
       (-> (p/all (map request->promise tx-req))
           (p/then #(p/resolve! p %)))
       (request->promise p tx-req))
     p)))

(defn with-open-db
  ([f] (with-open-db (p/deferred) f))
  ([p f]
   (let [open-req (.open js/indexedDB db-name db-version)]
     (set! (. open-req -onsuccess) #(let [db (event->result %)]
                                      (f db)))
     (set! (. open-req -onerror) (promise-error-handler p))
     p)))

(defn with-open-db+txn
  ([store-name f] (with-open-db+txn store-name f false))
  ([store-name f writeable?]
   (let [p (p/deferred)]
     (with-open-db p #(with-txn p % store-name f writeable?)))))

(defn record-merge [l r]
  (cond
    (nil? l) r
    (nil? r) l
    (and (sequential? l)
         (sequential? r)) (-> (into l r) (distinct) (vec))
    :else r))

(def guide-init-data
  #js[#js{:href  "/wikis/the-legend-of-zelda-breath-of-the-wild"
          :title "The Legend of Zelda: Breath of the Wild"
          :icon  "https://assets-prd.ignimgs.com/2022/06/14/zelda-breath-of-the-wild-1655249167687.jpg?width=240&crop=1%3A1%2Csmart&auto=webp"}
      #js{:href  "/wikis/the-legend-of-zelda-tears-of-the-kingdom"
          :title "The Legend of Zelda: Tears of the Kingdom"
          :icon  "https://assets-prd.ignimgs.com/2022/09/14/zelda-tears-of-the-kingdom-button-2k-1663127818777.jpg?width=240&crop=1%3A1%2Csmart&auto=webp"}
      #js{:href  "/wikis/hogwarts-legacy"
          :title "Hogwarts Legacy"
          :icon  "https://assets-prd.ignimgs.com/2022/05/24/hogwarts-legacy-button-fin-1653421326559.jpg?width=240&crop=1%3A1%2Csmart&auto=webp"}])

(defn create-index
  ([store name keyPath] (create-index store name keyPath #js{}))
  ([store name keyPath opts]
   (let [indexes (-> store (.-indexNames) (set))]
     (if-not (contains? indexes name)
       (.createIndex store name keyPath opts)))))


(defn init! []
  (let [open-req (.open js/indexedDB db-name db-version)]
    (set! (. open-req -onupgradeneeded)
          (fn [event]
            (let [db (event->result event)
                  old-version (.-oldVersion event)
                  txn (.-transaction open-req)]
              (try
                (let [
                      stores (-> db (.-objectStoreNames) (seq) (set))
                      page-store (if-not (contains? stores pages-store-name)
                                   (.createObjectStore db pages-store-name #js{:keyPath       pages-store-key
                                                                               :autoIncrement true})
                                   (.objectStore txn pages-store-name))
                      guide-store (if-not (contains? stores guides-store-name)
                                    (.createObjectStore db guides-store-name #js{:keyPath       guides-store-key
                                                                                 :autoIncrement true})
                                    (.objectStore txn guides-store-name))]
                  (when (< old-version 1)
                    (create-index page-store "href" "href" #js{:unique true})
                    (create-index page-store "aliases" "aliases" #js{:multiEntry true})
                    (create-index page-store "to_process" #js["broken" "fetched"])

                    (create-index guide-store "aliases" "aliases" #js{:multiEntry true})
                    (create-index guide-store "title" "title"))
                  (create-index guide-store "href" "href" #js{:unique true})

                  (when (< old-version 1)
                    (set! (.. guide-store -transaction -oncomplete)
                          (fn [_]
                            (with-txn db guides-store-name
                                      (fn [store]
                                        (->> (for [guide guide-init-data]
                                               (.add store guide))
                                             (p/all)))
                                      true)))))
                (catch js/Object ex
                  (.error js/console ex)
                  (if (< old-version 1)
                    (.deleteDatabase js/indexedDB db-name))
                  (-> open-req (.-transaction) (.abort)))))))))
