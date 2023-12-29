(ns wiki-guides.page.controller
  (:require [clojure.string :as str]
            [clojure.zip :as zip]
            [reagent.core :as r]
            [hickory.core :as hickory]
            [hickory.select :as s]
            [hickory.convert :as hc]
            [hickory.zip :refer [hickory-zip]]))

(def params
  {:path [:page]})

(def base-url "https://www.ign.com/")

(defonce content (r/atom nil))

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
    (cond-> a
            true (assoc-in [:attrs :href] new-href)
            (and (not wiki)
                 (not relative)) (assoc-in [:attrs :target] "_blank"))))

(defn update-tags [h selector-fn update-fn]
  (loop [zip (hickory-zip h)
         znode (s/select-next-loc selector-fn zip)]
    (if znode
      (let [a (zip/node znode)
            updated (zip/replace znode (update-fn a))
            znext (s/select-next-loc selector-fn (zip/next updated))]
        (recur updated znext))
      (zip/root zip))))

(defonce video-placeholder
         {:type    :element,
          :attrs   {:class "alert alert-info"},
          :tag     :div,
          :content ["Video has been removed"]})

(defn start [{:keys [path]}]
  (let [url (str base-url (:page path))]
    (-> (.fetch js/window url)
        (.then #(.text %))
        (.then #(->> (hickory/parse %)
                     (hickory/as-hickory)
                     (s/select (s/tag :main))
                     (first)))
        (.then #(-> %
                    (update-tags (s/tag :a) (partial update-href (:page path)))
                    (update-tags (s/class :wiki-video) (constantly video-placeholder))))
        (.then #(reset! content %)))))

(def desc
  {:parameters params
   :start      start})
