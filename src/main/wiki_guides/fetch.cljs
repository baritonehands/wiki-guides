(ns wiki-guides.fetch
  (:require [cljs.core.async :as async :refer [go go-loop <! >!]]
            [cljs-http.client :as http]
            [clojure.string :as str]
            [hickory.core :as hickory]
            [hickory.render :as render]
            [hickory.select :as s]
            [promesa.core :as p]
            [wiki-guides.channels :as queues]
            [wiki-guides.page.transform :as page-transform]
            [wiki-guides.store.page :as page-store]
            [wiki-guides.utils :as utils])
  (:import goog.Uri))

(def num-blocks 4)                                          ; requests (go blocks) at a time

(def base-url "https://www.ign.com")

(defonce in-progress (atom #{}))

(defn offer! [href]
  (p/let [record (page-store/fetch href)]
    (if-not record
      (page-store/add
        {:href    href
         :broken  0
         :fetched 0}))))

(defn poll! []
  (let [chan (async/chan)]
    (p/let [records (page-store/to-process)]
      (go
        (let [found (array nil)]
          (swap!
            in-progress
            (fn [ips]
              (if-let [record (->> records
                                   (remove #(contains? ips (:href %)))
                                   (first))]
                (do
                  (aset found 0 (:href record))
                  (conj ips (:href record)))
                ips)))
          (if (aget found 0)
            (>! chan (aget found 0))
            (async/close! chan)))))
    chan))

(defn- impl [url]
  (let [chan (async/chan)
        p (js/fetch (str base-url url))]
    (-> p
        (p/then (fn [response]
                  (p/let [ok (.-ok response)
                          body (if ok
                                 (.text response))]
                    (if (= (.-status response) 500)
                      (println "500" ok body))
                    {:ok          ok
                     :status      (.-status response)
                     :status-text (.-statusText response)
                     :redirected  (.-redirected response)
                     :body        body
                     :url         (some-> (.-url response) (str/replace base-url ""))})))
        (p/then #(async/put! chan %)))
    chan))

(defn response->hickory [response]
  (->> response
       :body
       (hickory/parse)
       (hickory/as-hickory)))

(defn extract-title [h]
  (->> h
       (s/select (s/class :display-title))
       (first)
       (page-transform/hickory-to-text)
       (str/trim)))

(defn extract-main [h]
  (->> h
       (s/select (s/tag :main))
       (first)))

(defn prefetch! [url]
  (let [href (utils/url-path url)]
    (offer! href)))

(defn promise [url]
  (let [p (p/deferred)]
    (go
      (let [response (<! (impl url))]
        (if (not (:ok response))
          (when (not= (:status response) 429)
            (let [record (cond-> {:href    (if (:redirected response)
                                             (:url response)
                                             url)
                                  :broken  1
                                  :fetched 1}
                                 (:redirected response) (assoc :aliases [url]))]
              (page-store/add record)
              (if (:redirected response)
                (page-store/delete url)))
            (p/reject! p (:status-text response)))
          (let [h (response->hickory response)
                title (extract-title h)
                main (->> h
                          (extract-main)
                          (page-transform/process url))
                html (render/hickory-to-html main)
                record (cond-> {:href    (if (:redirected response)
                                           (:url response)
                                           url)
                                :broken  0
                                :fetched 1
                                :title   title
                                :html    html
                                :text    (page-transform/hickory-to-text main)}
                               (:redirected response) (assoc :aliases [url]))]
            (doseq [href (page-transform/wiki-links main)]
              (prefetch! href))
            (page-store/add record)
            (if (:redirected response)
              (page-store/delete url))
            (p/resolve! p html)))))
    p))

(defn init! []
  (dotimes [n num-blocks]
    (go-loop []
      (let [start (system-time)]
        (if-let [url (<! (poll!))]
          (let [_ (println "go-loop" n "to fetch:" url)
                response (<! (impl url))]
            (if (not (:ok response))
              (when (not= (:status response) 429)
                (let [record (cond-> {:href    (if (:redirected response)
                                                 (:url response)
                                                 url)
                                      :broken  1
                                      :fetched 1}
                                     (:redirected response) (assoc :aliases [url]))]
                  (page-store/add record)
                  (if (:redirected response)
                    (page-store/delete url))))
              (let [h (response->hickory response)
                    title (extract-title h)
                    main (extract-main h)
                    msg (cond-> {:url     (if (:redirected response)
                                            (:url response)
                                            url)
                                 :title   title
                                 :hickory main}
                                (:redirected response) (assoc :aliases [url]))]
                (if (:redirected response)
                  (page-store/delete url))
                (>! queues/web-workers ["process" msg])))))
        (let [end (system-time)
              duration (- end start)]
          (when (< duration 950)
            (<! (async/timeout (- 1000 duration))))))
      (recur))))
