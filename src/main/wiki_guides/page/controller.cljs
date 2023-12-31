(ns wiki-guides.page.controller
  (:require [clojure.core.async :as async]
            [hickory.render :as render]
            [promesa.core :as p]
            [reagent.core :as r]
            [wiki-guides.fetch :as fetch]
            [wiki-guides.store.page :as page-store]))

(def params
  {:path [:page]})

(def base-url "https://www.ign.com/")

(defonce *content (r/atom nil))

(defn start [{:keys [path]}]
  (let [href (str "/" (:page path))]
    (reset! *content nil)
    (-> (page-store/fetch href)
        (p/then (fn [page]
                  (if (and page (pos? (:fetched page)))
                    (:html page)
                    (-> (fetch/promise href)
                        (p/catch (fn [error]
                                   (str "<h1>Page not found: " error "</h1>")))))))
        (p/then (fn [html]
                  (reset! *content html)))
        (p/catch (fn [_]
                   (fetch/offer! href))))))

(def desc
  {:parameters params
   :start      start})
