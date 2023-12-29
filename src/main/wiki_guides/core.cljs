(ns wiki-guides.core
  (:require [reagent.dom.client :as rdom]
            [wiki-guides.guides :as guides]))

(defn app-component []
  [guides/list-view])

(defonce root (rdom/create-root (.getElementById js/document "app-container")))

(defn ^:dev/after-load force-rerender []
  (rdom/render root [app-component]))

(defn ^:export main []
  (rdom/render root [app-component]))
