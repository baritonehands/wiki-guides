(ns wiki-guides.web-worker.core
  (:require [cognitect.transit :as transit]
            [wiki-guides.store :as store]
            [wiki-guides.web-worker.page :as page]))

(def reader (transit/reader :json))

(defn main []
  (page/init!)

  (.addEventListener
    js/self
    "message"
    (fn [event]
      (let [type (-> event .-data .-type)
            payload (->> event .-data .-payload (transit/read reader))]
        (case type
          "process" (page/put! payload)
          :default nil)))))
