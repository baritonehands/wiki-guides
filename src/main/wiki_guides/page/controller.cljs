(ns wiki-guides.page.controller
  (:require [clojure.core.async :as async]
            [hickory.render :as render]
            [promesa.core :as p]
            [reagent.core :as r]
            [wiki-guides.fetch :as fetch]
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
  (let [href (str "/" (:page path))]
    (reset! *content nil)
    (p/let [guide-href (utils/guide-root href)
            guide (guide-store/fetch guide-href)]
      (guide-store/set-current! (or guide {:href guide-href
                                           :aliases []}))
      (search/init!)
      (p/let [page (-> (page-store/fetch href)
                       (p/then (fn [page]
                                 (if (and page (pos? (:fetched page)))
                                   page
                                   (-> (fetch/promise href)
                                       (p/catch #(hash-map :html (error-content %)))))))
                       (p/catch (fn [_]
                                  (reset! *content (error-content "Unexpected Error"))
                                  (fetch/offer! href))))]
        (reset! *content (:html page))
        ; Refetch the guide if it was an alias
        (if (and (not= (:href page) href)
                 (zero? (:broken page)))
          (p/let [alias guide-href
                  guide-href (utils/guide-root (:href page))
                  guide (guide-store/fetch guide-href)]
            (guide-store/set-current! (or guide {:href guide-href
                                                 :aliases [alias]}))
            (guide-store/add-alias! alias)
            (search/init!)))))))



(def desc
  {:parameters params
   :start      start})
