(ns wiki-guides.core
  (:require [reagent.dom :as rdom]))

(defn app-component []
  [:h2 "Hello, PWA!"])

(defn ^:dev/after-load force-rerender []
  (rdom/force-update-all))

(defn ^:export main []
  (rdom/render [app-component] (.getElementById js/document "app-container")))
