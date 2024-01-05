(ns wiki-guides.store.guide
  (:require [promesa.core :as p]
            [reagent.core :as r]
            [wiki-guides.store :as store]))

(defonce *current (r/atom nil))

(defn set-current! [guide]
  (reset! *current guide))

(defn fetch [href]
  (-> (p/all [(store/with-open-db+txn store/guides-store-name #(-> % (.index "href") (.get href)))
              (store/with-open-db+txn store/guides-store-name #(-> % (.index "aliases") (.get href)))])
      (p/then (fn [[record1 record2]]
                (some-> (or record1 record2) (js->clj :keywordize-keys true))))))

(defn add [obj]
  (-> (fetch (:href obj))
      (p/then
        (fn [orig]
          (store/with-open-db+txn store/guides-store-name #(.put % (clj->js (merge-with store/record-merge orig obj))) true)))))

(defn add-alias! [href]
  (let [update-fn (fn []
                    (swap! *current update :aliases (fnil #(-> %1 (conj %2) (distinct)) []) href))
        guide @*current]
    (if-not (:id guide)
      (update-fn)
      (if-not (some-> guide :aliases (contains? href))
        (add (update-fn))))))


(defn by-title []
  (-> (store/with-open-db+txn store/guides-store-name #(-> % (.index "title") (.getAll)))
      (p/then #(js->clj % :keywordize-keys true))))
