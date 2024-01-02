(ns wiki-guides.web-worker.message
  (:require [cognitect.transit :as transit]))

(def writer (transit/writer :json))

(defn send! [type obj]
  (.postMessage js/self #js {:type type :payload (transit/write writer obj)}))
