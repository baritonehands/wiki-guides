(ns wiki-guides.guides
  (:require [re-com.core :refer [label hyperlink-href h-box v-box]]
            [reagent.core :as r]))

(def all
  [{:href "/wikis/the-legend-of-zelda-breath-of-the-wild"
    :title "The Legend of Zelda: Breath of the Wild"
    :icon "https://assets-prd.ignimgs.com/2022/06/14/zelda-breath-of-the-wild-1655249167687.jpg?width=240&crop=1%3A1%2Csmart&auto=webp"}
   {:href "/wikis/the-legend-of-zelda-tears-of-the-kingdom"
    :title "The Legend of Zelda: Tears of the Kingdom"
    :icon "https://assets-prd.ignimgs.com/2022/09/14/zelda-tears-of-the-kingdom-button-2k-1663127818777.jpg?width=240&crop=1%3A1%2Csmart&auto=webp"}
   {:href "/wikis/hogwarts-legacy"
    :title "Hogwarts Legacy"
    :icon "https://assets-prd.ignimgs.com/2022/05/24/hogwarts-legacy-button-fin-1653421326559.jpg?width=240&crop=1%3A1%2Csmart&auto=webp"}])

(defn guide-view [{:keys [href title icon]}]
  [hyperlink-href
   :href (str "#" href)
   :label
   [v-box
    :max-width "120px"
    :style {:text-align "center"}
    :children
    [[:img {:src icon :alt title :style {:width "120px"}}]
     [:span title]]]])

(defn list-view [_]
  [v-box
   :width "400px"
   :children
   (for [[idx row] (map-indexed vector (partition 3 3 (repeat nil) all))]
     ^{:key idx}
     [h-box
      :gap "20px"
      :children
      (for [{:keys [href] :as item} row
            :when item]
        ^{:key href}
        [guide-view item])])])
