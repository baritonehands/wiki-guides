(ns wiki-guides.store.page
  (:require [promesa.core :as p]
            [wiki-guides.store :as store]))

(defn add [obj]
  (-> (store/with-open-db+txn store/pages-store-name #(-> % (.index "href") (.get (:href obj))))
      (p/then #(js->clj (or % {}) :keywordize-keys true))
      (p/then
        (fn [orig]
          (store/with-open-db+txn store/pages-store-name #(.put % (clj->js (merge-with store/record-merge orig obj))) true)))))

(defn fetch [href]
  (-> (p/all [(store/with-open-db+txn store/pages-store-name #(-> % (.index "href") (.get href)))
              (store/with-open-db+txn store/pages-store-name #(-> % (.index "aliases") (.get href)))])
      (p/then (fn [[record1 record2]]
                (some-> (or record1 record2) (js->clj :keywordize-keys true))))))

(defn fetch-ids [hrefs]
  (-> (store/with-open-db+txn store/pages-store-name
                              (fn [store]
                                (for [href hrefs]
                                  (-> store (.index "href") (.get href)))))
      (p/then (fn [results]
                (js->clj results :keywordize-keys true)))))

(defn delete [href]
  (-> (store/with-open-db+txn store/pages-store-name #(-> % (.index "href") (.get href)))
      (p/then
        (fn [orig]
          (if orig
            (store/with-open-db+txn store/pages-store-name #(.delete % (.-id orig)) true))))))

(defn all-for-search [guide-href]
  (let [key-range (.bound js/IDBKeyRange guide-href (str guide-href "/~") true false)]
    (store/with-open-db+txn store/pages-store-name #(-> % (.index "href") (.getAll key-range)))))

(defn to-process []
  (-> (store/with-open-db+txn store/pages-store-name
                              #(-> % (.index "to_process") (.getAll #js[0 0])))
      (p/then #(js->clj % :keywordize-keys true))))
