(ns wiki-guides.page
  (:require [wiki-guides.page.controller :as page-controller]
            [wiki-guides.utils :as utils]
            [hickory.render :as render]))

(defn view [route]
  [:div
   [utils/ppr-str route]
   (if @page-controller/content
     [:div {:dangerouslySetInnerHTML {:__html (render/hickory-to-html @page-controller/content)}}])])
