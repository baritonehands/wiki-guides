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

(defonce in-progress (atom {:seen  #{}
                            :queue #queue[]}))

(defn offer! [href]
  (swap!
    in-progress
    (fn [{:keys [seen queue] :as v}]
      (if-not (contains? seen href)
        {:seen  (conj seen href)
         :queue (conj queue href)}
        v))))

(defn poll! []
  (let [mut (array nil)]
    (swap!
      in-progress
      (fn [{:keys [seen queue]}]
        (let [href (peek queue)]
          (aset mut 0 href)
          {:seen  seen
           :queue (pop queue)})))
    (aget mut 0)))

(defn- impl [url]
  (let [chan (async/chan)
        p (js/fetch (str base-url url))]
    (-> p
        (p/then (fn [response]
                  (p/let [body (.text response)]
                    {:ok          (.-ok response)
                     :status      (.-status response)
                     :status-text (.-statusText response)
                     :redirected  (.-redirected response)
                     :body        body
                     :url         (str/replace (.-url response) base-url "")})))
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
    (-> (page-store/fetch href)
        (p/then (fn [page]
                  (if-not page
                    (offer! href))))
        (p/catch (fn [_] (offer! href))))))

(defn promise [url]
  (let [p (p/deferred)]
    (go
      (let [response (<! (impl url))]
        (if (not (:ok response))
          (p/reject! p (:status-text response))
          (let [h (response->hickory response)
                title (extract-title h)
                main (->> h
                          (extract-main)
                          (page-transform/process url))
                html (render/hickory-to-html main)
                record (cond-> {:href  (if (:redirected response)
                                         (:url response)
                                         url)
                                :title title
                                :html  html
                                :text  (page-transform/hickory-to-text main)}
                               (:redirected response) (assoc :aliases [url]))]
            (doseq [href (page-transform/wiki-links main)]
              (prefetch! href))
            (page-store/add record)
            (p/resolve! p html)))))
    p))

(defn init! []
  (dotimes [n num-blocks]
    (go-loop []
      (let [start (system-time)]
        (if-let [url (poll!)]
          (let [_ (println "go-loop" n "to fetch:" url)
                response (<! (impl url))
                h (response->hickory response)
                title (extract-title h)
                main (extract-main h)
                msg (cond-> {:url     (if (:redirected response)
                                        (:url response)
                                        url)
                             :title   title
                             :hickory main}
                            (:redirected response) (assoc :aliases [url]))]
            (>! queues/web-workers ["process" msg])))
        (let [end (system-time)
              duration (- end start)]
          (when (< duration 950)
            (<! (async/timeout (- 1000 duration))))))
      (recur))))
