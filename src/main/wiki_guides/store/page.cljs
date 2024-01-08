(ns wiki-guides.store.page
  (:require [promesa.core :as p]
            [wiki-guides.store :as store]))

(defn add [obj]
  (-> (store/with-open-db+txn store/pages-store-name
                              (fn [store]
                                (if-not (:href obj)
                                  (println obj))
                                (-> store
                                    (.index "href")
                                    (.get (:href obj)))))
      (p/then #(js->clj (or % {}) :keywordize-keys true))
      (p/then
        (fn [orig]
          (store/with-open-db+txn store/pages-store-name #(.put % (clj->js (merge-with store/record-merge orig obj))) true)))))

(defn fetch
  ([href] (fetch href true))
  ([href clj?]
   (-> (p/all [(store/with-open-db+txn store/pages-store-name #(-> % (.index "href") (.get href)))
               (store/with-open-db+txn store/pages-store-name #(-> % (.index "aliases") (.get href)))])
       (p/then (fn [[record1 record2]]
                 (cond-> (or record1 record2)
                         clj? (some-> (js->clj :keywordize-keys true))))))))

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

(defn delete-all [guide-href]
  (-> (store/with-open-db+txn store/pages-store-name #(-> % (.index "guideHref") (.getAll guide-href)))
      (p/then
        (fn [pages]
          (store/with-open-db+txn store/pages-store-name
                                  (fn [store]
                                    (for [id (map #(.-id %) (seq pages))]
                                      (.delete store id)))
                                  true)))))

(defn all-for-search [guide-href]
  (store/with-open-db+txn store/pages-store-name #(-> % (.index "guideHref") (.getAll guide-href))))

(defn to-process [guide-href]
  (-> (store/with-open-db+txn store/pages-store-name
                              #(-> % (.index "guideToProcess") (.getAll #js[0 0 guide-href])))
      (p/then #(js->clj % :keywordize-keys true))))

(defn progress [guide-href]
  (p/let [progress (to-process guide-href)
          total (store/with-open-db+txn store/pages-store-name
                                        #(-> % (.index "guideHref") (.getAll guide-href)))]
    [(- (.-length total) (count progress)) (.-length total)]))
