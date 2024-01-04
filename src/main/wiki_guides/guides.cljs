(ns wiki-guides.guides
  (:require [promesa.core :as p]
            [re-com.core :refer [label hyperlink h-box v-box]]
            [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [wiki-guides.nav :as nav]
            [wiki-guides.store.guide :as guide-store]))

(def all
  [{:href  "/wikis/the-legend-of-zelda-breath-of-the-wild"
    :title "The Legend of Zelda: Breath of the Wild"
    :icon  "https://assets-prd.ignimgs.com/2022/06/14/zelda-breath-of-the-wild-1655249167687.jpg?width=240&crop=1%3A1%2Csmart&auto=webp"}
   {:href  "/wikis/the-legend-of-zelda-tears-of-the-kingdom"
    :title "The Legend of Zelda: Tears of the Kingdom"
    :icon  "https://assets-prd.ignimgs.com/2022/09/14/zelda-tears-of-the-kingdom-button-2k-1663127818777.jpg?width=240&crop=1%3A1%2Csmart&auto=webp"}
   {:href  "/wikis/hogwarts-legacy"
    :title "Hogwarts Legacy"
    :icon  "https://assets-prd.ignimgs.com/2022/05/24/hogwarts-legacy-button-fin-1653421326559.jpg?width=240&crop=1%3A1%2Csmart&auto=webp"}])

(defn guide-view [{:keys [href title icon]}]
  (let [hash-href (str "#" href)]
    [hyperlink
     :on-click (fn []
                 (nav/set-root! hash-href)
                 (rfe/push-state :wiki-guides.core/page {:page (.substring href 1)}))
     :label
     [v-box
      :class "guide-item"
      :style {:text-align "center"}
      :children
      [[:img.guide-icon {:src icon :alt title}]
       [:span.guide-title title]]]]))


(defn list-view [_]
  (let [*all (r/atom nil)]
    (-> (guide-store/by-title)
        (p/then #(reset! *all %)))
    (fn []
      [h-box
       :gap "20px"
       :width "400px"
       :class "guide-list"
       :children
       (for [{:keys [href] :as item} @*all]
         ^{:key href}
         [guide-view item])])))
