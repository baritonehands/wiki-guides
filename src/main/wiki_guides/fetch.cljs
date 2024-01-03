(ns wiki-guides.fetch
  (:require [cljs.core.async :as async :refer [go-loop <! >!]]
            [cljs-http.client :as http]
            [hickory.core :as hickory]
            [hickory.select :as s]
            [wiki-guides.channels :as channels]))

(def num-blocks 50)                                         ; requests (go blocks) at a time

(def base-url "https://www.ign.com")

(defn init! []
  (dotimes [n num-blocks]
    (go-loop []
      (let [url (<! channels/fetch)
            _ (println "go-loop" n "to fetch:" url)
            start (system-time)
            response (<! (http/get (str base-url url)
                                   {:with-credentials? false}))
            main (->> response
                      (:body)
                      (hickory/parse)
                      (hickory/as-hickory)
                      (s/select (s/tag :main))
                      (first))]
        (>! channels/web-workers ["process" {:url url
                                             :hickory main}])
        (let [end (system-time)
              duration (- end start)]
          (when (< duration 950)
            (println "Sleeping" (- 1000 duration))
            (<! (async/timeout (- 1000 duration))))))
      (recur))))

(defonce seen (atom #{}))

(defn put! [href]
  (swap!
    seen
    (fn [v]
      (if-not (contains? v href)
        (do
          (async/put! channels/fetch href)
          (conj v href))
        v))))

