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
            [wiki-guides.store :as store])
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
          {:seen  (disj seen href)
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

(defn extract-main [response]
  (->> response
       :body
       (hickory/parse)
       (hickory/as-hickory)
       (s/select (s/tag :main))
       (first)))

(defn prefetch! [url]
  (let [href (.getPath (Uri. url))]
    (-> (store/fetch href)
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
          (let [main (->> response
                          (extract-main)
                          (page-transform/process url))
                html (render/hickory-to-html main)
                record (cond-> {:href  (if (:redirected response)
                                         (:url response)
                                         url)
                                :title "My Title"
                                :html  html
                                :text  (page-transform/hickory-to-text main)}
                               (:redirected response) (assoc :alias url))]
            (doseq [href (page-transform/wiki-links main)]
              (prefetch! href))
            (store/add record)
            (p/resolve! p html)))))
    p))

(defn init! []
  (dotimes [n num-blocks]
    (go-loop []
      (let [start (system-time)]
        (if-let [url (poll!)]
          (let [_ (println "go-loop" n "to fetch:" url)
                response (<! (impl url))
                main (extract-main response)
                msg (cond-> {:url     (if (:redirected response)
                                        (:url response)
                                        url)
                             :hickory main}
                            (:redirected response) (assoc :alias url))]
            (>! queues/web-workers ["process" msg])))
        (let [end (system-time)
              duration (- end start)]
          (when (< duration 950)
            (<! (async/timeout (- 1000 duration))))))
      (recur))))
