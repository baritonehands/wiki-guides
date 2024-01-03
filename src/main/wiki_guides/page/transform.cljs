(ns wiki-guides.page.transform
  (:require [clojure.string :as str]
            [clojure.zip :as zip]
            [hickory.select :as s]
            [hickory.zip :refer [hickory-zip]]
            [wiki-guides.utils :as utils]))

(def base-url "https://www.ign.com/")

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
                   relative (str "#" parent-url href)
                   :else (str base-url (.substring href 1)))]
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
          :attrs   {:class          "alert alert-video"
                    :data-omit-text "true"},
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

(defn process [url h]
  (-> h
      (update-tags (s/tag :a) (update-a-fn url))
      (update-tags (s/class :wiki-video) update-video)
      (update-tags (s/class :gh-blue-box) update-blue-box)
      (update-tags (s/or (s/class :wiki-bobble)
                         (s/class :feedback-container)) remove-loc)
      (update-tags (s/descendant (s/tag :button) (s/tag :img)) update-images)))

(def inline-element
  "Elements that should not add whitespace"
  #{:a :abbr :acronym :b :bdo :big :br :button :cite :code :dfn :em :i :img :input :kbd :label :map :object :output :q
    :samp :script :select :small :span :strong :sub :sup :textarea :time :tt :var})

(defn hickory-to-text
  "Given a hickory format dom object, returns an equivalent text
   representation."
  [dom]
  (let [content->str (fn [content]
                       (-> (apply str (map hickory-to-text content))
                           (str/replace-all #" {2,}" " ")))]
    (if (string? dom)
      dom
      (case (:type dom)
        :document
        (content->str (:content dom))
        :element
        (cond
          (= (get-in dom [:attrs :data-omit-text]) "true") ""
          (not (inline-element (:tag dom))) (str " " (content->str (:content dom)) " ")
          :else (content->str (:content dom)))
        ""))))

(defn wiki-links [h]
  (for [a (s/select (s/tag :a) h)
        :let [href (get-in a [:attrs :href])]
        :when (str/starts-with? href "#/")]
    (.substring href 1)))
