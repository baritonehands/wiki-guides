(ns wiki-guides.page.controller
  (:require [clojure.string :as str]
            [clojure.zip :as zip]
            [reagent.core :as r]
            [hickory.core :as hickory]
            [hickory.select :as s]
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

(defn rewrite-links [parent-url h]
  (loop [zip (hickory-zip h)
         znode (s/select-next-loc (s/tag :a) zip)]
    (if znode
      (let [a (zip/node znode)
            updated (zip/replace znode (update-href parent-url a))
            znext (s/select-next-loc (s/tag :a) (zip/next updated))]
        (recur updated znext))
      (zip/root zip))))

(defn start [{:keys [path]}]
  (let [url (str base-url (:page path))]
    (-> (.fetch js/window url)
        (.then #(.text %))
        (.then #(->> (hickory/parse %)
                     (hickory/as-hickory)
                     (s/select (s/tag :main))
                     (first)))
        (.then #(reset! content (rewrite-links (:page path) %))))))

(def desc
  {:parameters params
   :start      start})
