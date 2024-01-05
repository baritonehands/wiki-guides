(ns wiki-guides.web-workers
  (:require [cljs.core.async :as async :refer [go-loop <!]]
            [cognitect.transit :as transit]
            [wiki-guides.config :as config]
            [wiki-guides.channels :as queues]
            [wiki-guides.fetch :as fetch]))

(def worker-file (str config/base-url "/web-worker.js"))
(def num-workers (.-hardwareConcurrency js/navigator))

(defonce workers (atom []))

(def reader (transit/reader :json))
(def writer (transit/writer :json))

(defn send-message!
  ([type payload]
   (send-message! (rand-nth @workers) type payload))
  ([worker type payload]
   (.postMessage worker #js {:type type :payload (transit/write writer payload)})))

(defn init! []
  (dotimes [n num-workers]
    (let [worker (js/Worker. worker-file)
          _ (go-loop []
              (let [[type payload] (<! queues/web-workers)]
                (println "Sending message" type "to worker" n)
                (send-message! worker type payload))
              (recur))]
      (.addEventListener
        worker
        "message"
        (fn [event]
          (let [type (-> event .-data .-type)
                payload (->> event .-data .-payload (transit/read reader))]
            (case type
              "fetch" (fetch/offer! payload)
              :default nil))))
      (swap! workers conj worker))))
