(ns wiki-guides.guides
  (:require [promesa.core :as p]
            [re-com.core :refer [label hyperlink h-box v-box]]
            [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [wiki-guides.nav :as nav]
            [wiki-guides.store.guide :as guide-store]))

(defn guide-view [{:keys [href title icon]}]
  (let [hash-href (.substring href 1)]
    [hyperlink
     :on-click (fn []
                 (nav/set-root! hash-href)
                 (rfe/push-state :wiki-guides.core/page {:page hash-href}))
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
