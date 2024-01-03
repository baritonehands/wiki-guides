(ns wiki-guides.page.controller
  (:require [clojure.core.async :as async]
            [promesa.core :as p]
            [reagent.core :as r]
            [wiki-guides.channels :as channels]
            [wiki-guides.store :as store]))

(def params
  {:path [:page]})

(def base-url "https://www.ign.com/")

(defonce *content (r/atom nil))

(defn start [{:keys [path]}]
  (let [href (str "/" (:page path))]
    (reset! *content nil)
    (-> (store/fetch href)
        (p/then (fn [page]
                  (if page
                    (reset! *content (:html page))
                    (async/put! channels/fetch href))))
        (p/catch (fn [_]
                   (async/put! channels/fetch href))))))

(def desc
  {:parameters params
   :start      start})
