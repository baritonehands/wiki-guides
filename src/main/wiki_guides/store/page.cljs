(ns wiki-guides.store.page
  (:require [promesa.core :as p]
            [wiki-guides.store :as store]))

(defn add [obj]
  (-> (store/with-open-db+txn store/pages-store #(.get % (:href obj)))
      (p/then #(js->clj (or % {}) :keywordize-keys true))
      (p/then
        (fn [orig]
          (store/with-open-db+txn store/pages-store #(.put % (clj->js (merge-with store/record-merge orig obj))) true)))))

(defn fetch [href]
  (-> (p/all [(store/with-open-db+txn store/pages-store #(.get % href))
              (store/with-open-db+txn store/pages-store #(-> (.index % "aliases") (.get href)))])
      (p/then (fn [[record1 record2]]
                (some-> (or record1 record2) (js->clj :keywordize-keys true))))))

(defn fetch-ids [hrefs]
  (-> (store/with-open-db+txn store/pages-store
                              (fn [store]
                                (for [href hrefs]
                                  (.get store href))))
      (p/then (fn [results]
                (js->clj results :keywordize-keys true)))))

(defn all-for-search []
  (store/with-open-db+txn store/pages-store #(.getAll %)))
