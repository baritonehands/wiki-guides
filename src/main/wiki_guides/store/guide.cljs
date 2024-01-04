(ns wiki-guides.store.guide
  (:require [promesa.core :as p]
            [wiki-guides.store :as store]))

(defn add [obj]
  (-> (store/with-open-db+txn store/guides-store #(.get % (:href obj)))
      (p/then #(js->clj (or % {}) :keywordize-keys true))
      (p/then
        (fn [orig]
          (store/with-open-db+txn store/guides-store #(.put % (clj->js (merge-with store/record-merge orig obj))) true)))))

(defn by-title []
  (-> (store/with-open-db+txn store/guides-store #(-> % (.index "title") (.getAll)))
      (p/then #(js->clj % :keywordize-keys true))))
