(ns wiki-guides.fetch
  (:require [cljs.core.async :as async :refer [go go-loop <!]]
            [hickory.core :as hickory]
            [hickory.select :as s]
            [wiki-guides.channels :as channels]))

(def num-blocks 12)                                         ; 12 requests (go blocks) at a time

(defn init! []
  (dotimes [_ num-blocks]
    (go-loop []
      (let [url (<! channels/fetch)]
        (println "To fetch:" url))
        ;(-> (js/fetch (str url))
        ;    (.then #(.text %))
        ;    (.then #(->> (hickory/parse %)
        ;                 (hickory/as-hickory)
        ;                 (s/select (s/tag :main))
        ;                 (first)))
        ;    (.then #(async/put! channels/web-workers ["process" {:url     url
        ;                                                         :hickory %}]))))
      (recur))))
