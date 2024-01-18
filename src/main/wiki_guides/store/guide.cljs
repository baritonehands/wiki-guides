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

(defn delete [href]
  (-> (store/with-open-db+txn store/guides-store-name #(-> % (.index "href") (.get href)))
      (p/then
        (fn [orig]
          (if orig
            (store/with-open-db+txn store/guides-store-name #(.delete % (.-id orig)) true))))))

(defn update-guide! [pred f & args]
  (let [update-fn (fn []
                    (apply swap! *current f args))
        guide @*current]
    (if-not (:id guide)
      (update-fn)
      (if (pred guide)
        (add (update-fn))))))

(defn add-alias! [href]
  (update-guide!
    #(not (some-> % :aliases (contains? href)))
    update :aliases (fnil #(-> %1 (conj %2) (distinct)) []) href))

(defn set-download! [v]
  (update-guide!
    (constantly true)
    assoc :download v))

(defn set-title! [v]
  (update-guide!
    (constantly true)
    assoc :title v))

(defn by-title []
  (-> (store/with-open-db+txn store/guides-store-name #(-> % (.index "title") (.getAll)))
      (p/then #(js->clj % :keywordize-keys true))))
