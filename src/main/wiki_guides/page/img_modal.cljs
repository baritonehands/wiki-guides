(ns wiki-guides.page.img-modal
  (:require [re-com.core :refer [modal-panel]]
            [reagent.core :as r]))

(defonce *url (r/atom nil))

(defn show! [url]
  (reset! *url url))

(defn hide! []
  (reset! *url nil))

(defn view []
  (if @*url
    [modal-panel
     :backdrop-on-click hide!
     :wrap-nicely? false
     :child
     [:img {:src @*url :style {:max-width "80vw"}}]]))
