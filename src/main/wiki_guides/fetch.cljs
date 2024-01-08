(ns wiki-guides.fetch
  (:require [cljs.core.async :as async :refer [go go-loop <! >!]]
            [clojure.string :as str]
            [hickory.core :as hickory]
            [hickory.render :as render]
            [hickory.select :as s]
            [promesa.core :as p]
            [wiki-guides.channels :as queues]
            [wiki-guides.page.transform :as page-transform]
            [wiki-guides.search :as search]
            [wiki-guides.store.guide :as guide-store]
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
        {:href      href
         :guideHref (utils/guide-root href)
         :broken    0
         :fetched   0}))))

(defn poll! []
  (if-not (:download @guide-store/*current)
    (async/timeout 9000)                                    ; + 1000 for regular loop
    (let [chan (async/chan)]
      (p/let [records (page-store/to-process (:href @guide-store/*current))]
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
      chan)))

(defn- impl [url]
  (let [chan (async/chan)
        p (js/fetch (str base-url url))]
    (-> p
        (p/then (fn [response]
                  (p/let [ok (.-ok response)
                          body (if ok
                                 (.text response))]
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

(defn handle-alias! [response url]
  (when (:redirected response)
    (page-store/delete url)
    (search/delete url)
    (guide-store/add-alias! (utils/guide-root url))))

(defn response->record! [url response process?]
  (if (not (:ok response))
    (when (not= (:status response) 429)
      (let [record (cond-> {:href      (if (:redirected response)
                                         (:url response)
                                         url)
                            :guideHref (if (:redirected response)
                                         (utils/guide-root (:url response))
                                         (utils/guide-root url))
                            :broken    1
                            :fetched   1}
                           (:redirected response) (assoc :aliases [url]))]
        (page-store/add record)
        (handle-alias! response url)
        [true record]))
    [false (:status-text response)])
  (let [h (response->hickory response)
        title (extract-title h)
        main (-> h
                 (extract-main)
                 (cond->>
                   process? (page-transform/process url)))
        record (cond-> {:href      (if (:redirected response)
                                     (:url response)
                                     url)
                        :guideHref (if (:redirected response)
                                     (utils/guide-root (:url response))
                                     (utils/guide-root url))
                        :broken    0
                        :fetched   1
                        :title     title}
                       process? (assoc :html (render/hickory-to-html main)
                                       :text (page-transform/hickory-to-text main))
                       (not process?) (assoc :main main)
                       (:redirected response) (assoc :aliases [url]))]
    (if (and process? (:download @guide-store/*current))
      (doseq [href (page-transform/wiki-links @guide-store/*current main)]
        (prefetch! href)))
    (when process?
      (page-store/add record)
      (if (:download @guide-store/*current)
        (search/add (:href record) (clj->js record))))
    (handle-alias! response url)
    [true record]))

(defn promise [url]
  (let [p (p/deferred)]
    (go
      (let [response (<! (impl url))]
        (let [[success? record-or-error] (response->record! url response true)]
          (if success?
            (p/resolve! p record-or-error)
            (p/reject! p record-or-error)))))
    p))

(defn init! []
  (dotimes [n num-blocks]
    (go-loop []
      (let [start (system-time)]
        (if-let [url (<! (poll!))]
          (let [response (<! (impl url))]
            (let [[_ record-or-error] (response->record! url response false)]
              (if (:ok response)
                (let [msg (cond-> {:url     (:href record-or-error)
                                   :title   (:title record-or-error)
                                   :hickory (:main record-or-error)}
                                  (:aliases record-or-error) (assoc :aliases (:aliases record-or-error)))]
                  (>! queues/web-workers ["process" {:guide @guide-store/*current
                                                     :page  msg}])))
              (swap! in-progress disj url))))
        (let [end (system-time)
              duration (- end start)]
          (when (< duration 950)
            (<! (async/timeout (- 1000 duration))))))
      (recur))))
