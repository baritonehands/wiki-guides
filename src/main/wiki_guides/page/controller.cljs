(ns wiki-guides.page.controller
  (:require [reagent.core :as r]
            [hickory.core :as hickory]
            [hickory.select :as s]))

(def params
  {:path [:page]})

(def base-url "https://www.ign.com/")

(defonce content (r/atom nil))

(defn start [{:keys [path]}]
  (let [url (str base-url (:page path))]
    (-> (.fetch js/window url)
        (.then #(.text %))
        (.then #(->> (hickory/parse %)
                     (hickory/as-hickory)
                     (s/select (s/tag :main))
                     (first)))
        (.then #(reset! content %)))))
