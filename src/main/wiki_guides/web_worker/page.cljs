(ns wiki-guides.web-worker.page
  (:require [cljs.core.async :as async :refer [go-loop <! >!]]
            [hickory.render :as render]
            [promesa.core :as p]
            [wiki-guides.store.page :as page-store]
            [wiki-guides.utils :as utils]
            [wiki-guides.web-worker.message :as message]
            [wiki-guides.page.transform :as page-transform])
  (:import goog.Uri))

(def num-blocks 12)                                         ; 12 requests (go blocks) per web worker

(defonce chan (async/chan 1000))

(defn prefetch! [url]
  (let [href (utils/url-path url)]
    (message/send! "fetch" href)))

(defn search-add! [url]
  (message/send! "search-add" url))

(defn init! []
  (dotimes [_ num-blocks]
    (go-loop []
      (let [{{:keys [url title hickory aliases]} :page
             guide                               :guide} (<! chan)
            main (page-transform/process url hickory)
            record (cond-> {:href    url
                            :broken  0
                            :fetched 1
                            :title   title
                            :html    (render/hickory-to-html main)
                            :text    (page-transform/hickory-to-text main)}
                           aliases (assoc :aliases aliases))]
        (doseq [href (page-transform/wiki-links guide main)]
          (prefetch! href))
        (page-store/add record)
        (search-add! url))
      (recur))))

(defn put! [val]
  (async/put! chan val))
