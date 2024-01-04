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
    (-> (page-store/fetch href)
        (p/then (fn [page]
                  (if-not page
                    (message/send! "fetch" href))))
        (p/catch (fn [_] (message/send! "fetch" href))))))

(defn init! []
  (dotimes [_ num-blocks]
    (go-loop []
      (let [{:keys [url hickory aliases]} (<! chan)
            main (page-transform/process url hickory)
            record (cond-> {:href  url
                            :title "My Title"
                            :html  (render/hickory-to-html main)
                            :text  (page-transform/hickory-to-text main)}
                           aliases (assoc :aliases aliases))]
        (doseq [href (page-transform/wiki-links main)]
          (prefetch! href))
        (page-store/add record)
        (recur)))))

(defn put! [val]
  (async/put! chan val))
