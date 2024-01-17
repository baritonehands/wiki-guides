(ns wiki-guides.page.controller
  (:require [promesa.core :as p]
            [reagent.core :as r]
            [wiki-guides.fetch :as fetch]
            [wiki-guides.page.transform :as page-transform]
            [wiki-guides.search :as search]
            [wiki-guides.store.guide :as guide-store]
            [wiki-guides.store.page :as page-store]
            [wiki-guides.utils :as utils]))

(def params
  {:path [:page]})

(defonce *content (r/atom nil))

(defn error-content [error]
  (str "<h1>Error on page load: " error "</h1>"))

(defn start [{:keys [path]}]
  (let [href (str "/" (:page path))
        guide-href (utils/guide-root href)]
    (reset! *content nil)
    (p/let [{:keys [aliases] :as guide} (guide-store/fetch guide-href)
            clean-href (-> href
                           (page-transform/coalesce-url)
                           (page-transform/replace-aliases (:href guide) aliases))
            clean-guide-href (utils/guide-root clean-href)]
      (guide-store/set-current! (or guide {:href clean-guide-href
                                           :aliases []}))
      (search/init!)
      (p/let [page (-> (page-store/fetch clean-href)
                       (p/then (fn [page]
                                 (if (and page (pos? (:fetched page)))
                                   page
                                   (-> (fetch/promise clean-href)
                                       (p/catch #(hash-map :html (error-content %)))))))
                       (p/catch (fn [_]
                                  (reset! *content (error-content "Unexpected Error"))
                                  (fetch/offer! clean-href))))]
        (reset! *content (:html page))
        (if (and (not= (:href page) clean-href)
                 (zero? (:broken page)))
          (guide-store/add-alias! clean-guide-href))))))



(def desc
  {:parameters params
   :start      start})
