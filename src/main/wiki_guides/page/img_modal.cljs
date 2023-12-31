(ns wiki-guides.page.img-modal
  (:require [re-com.core :refer [md-icon-button modal-panel]]
            [reagent.core :as r]))

(defonce *url (r/atom nil))

(defn show! [url]
  (reset! *url url))

(defn hide! []
  (reset! *url nil))

(defn view []
  (if @*url
    [modal-panel
     :backdrop-opacity 0.8
     :backdrop-on-click hide!
     :wrap-nicely? false
     :child
     [:div.img-modal
      [md-icon-button
       :md-icon-name "zmdi-close"
       :size :larger
       :on-click hide!]
      [:img {:src @*url :style {:max-width "80vw"}}]]]))
