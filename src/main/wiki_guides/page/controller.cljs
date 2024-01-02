(ns wiki-guides.page.controller
  (:require [clojure.string :as str]
            [clojure.zip :as zip]
            [reagent.core :as r]
            [hickory.core :as hickory]
            [hickory.select :as s]
            [hickory.convert :as hc]
            [hickory.zip :refer [hickory-zip]]
            [wiki-guides.utils :as utils]
            [wiki-guides.web-workers :as web-workers]))

(def params
  {:path [:page]})

(def base-url "https://www.ign.com/")

(defonce *content (r/atom nil))

(defn relative-url? [url]
  (not (str/starts-with? url "/")))

(defn wiki-url? [url]
  (str/starts-with? url "/wikis/"))

(defn update-href [parent-url a]
  (let [href (get-in a [:attrs :href])
        wiki (wiki-url? href)
        relative (relative-url? href)
        new-href (cond
                   wiki (str "#" href)
                   relative (str "#/" parent-url href)
                   :else (str base-url (.substring href 1)))]
    ;(when (or wiki relative)
    ;  (web-workers/send-message! "fetch" #js {:parent-url parent-url
    ;                                          :url        href}))
    (cond-> a
            true (assoc-in [:attrs :href] new-href)
            (and (not wiki)
                 (not relative)) (assoc-in [:attrs :target] "_blank"))))

(defn update-tags [h selector-fn update-fn]
  (loop [zip (hickory-zip h)
         loc (s/select-next-loc selector-fn zip)]
    (if loc
      (let [node (zip/node loc)
            updated (update-fn loc node)
            znext (s/select-next-loc selector-fn (zip/next updated))]
        (recur updated znext))
      (zip/root zip))))

(defonce video-placeholder
         {:type    :element,
          :attrs   {:class "alert alert-video"},
          :tag     :div,
          :content ["Video has been removed"]})

(defn update-a-fn [parent-url]
  (fn [loc node]
    (zip/replace loc (update-href parent-url node))))

(defn update-video [loc _]
  (zip/replace loc video-placeholder))

(defn update-blue-box [loc node]
  (zip/replace loc (assoc-in node [:attrs :class] "alert alert-success")))

(defn remove-loc [loc _]
  (zip/remove loc))

(defn update-images [loc node]
  (let [src (get-in node [:attrs :src])]
    (zip/replace loc (assoc-in node [:attrs :src] (utils/image-resize src "640")))))

(defn start [{:keys [path]}]
  (let [url (str base-url (:page path))]
    (reset! *content nil)
    (-> (.fetch js/window url)
        (.then #(.text %))
        (.then #(->> (hickory/parse %)
                     (hickory/as-hickory)
                     (s/select (s/tag :main))
                     (first)))
        (.then (fn [h]
                 (web-workers/send-message! "process" {:url     (:page path)
                                                       :hickory h})
                 h))
        (.then #(-> %
                    (update-tags (s/tag :a) (update-a-fn (:page path)))
                    (update-tags (s/class :wiki-video) update-video)
                    (update-tags (s/class :gh-blue-box) update-blue-box)
                    (update-tags (s/or (s/class :wiki-bobble)
                                       (s/class :feedback-container)) remove-loc)
                    (update-tags (s/descendant (s/tag :button) (s/tag :img)) update-images)))
        (.then #(reset! *content %)))))

(def desc
  {:parameters params
   :start      start})
