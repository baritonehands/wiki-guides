(ns wiki-guides.search
  (:require [promesa.core :as p]
            ["flexsearch" :as flexsearch]
            [wiki-guides.store.guide :as guide-store]
            [wiki-guides.store.page :as page-store]))

(defonce fs-document
         (flexsearch/Document.
           #js {:worker   true
                :document #js {:id    "href"
                               :index #js ["title" "text"]}}))

(defn init! []
  (let [guide @guide-store/*current]
    (if (:download guide)
      (-> (page-store/all-for-search (:href guide))
          (p/then #(doseq [page %]
                     (.add fs-document (.-href page) page)))))))

(defn search [term]
  (-> (.searchAsync fs-document term)
      (p/then #(js->clj % :keywordize-keys true))
      (p/then
        (fn [results]
          (p/all (for [{:keys [field result]} results]
                   (p/let [pages (page-store/fetch-ids result)]
                     [field pages])))))))
