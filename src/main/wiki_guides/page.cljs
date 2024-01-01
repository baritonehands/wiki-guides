(ns wiki-guides.page
  (:require ["react" :as react]
            [re-com.core :refer [h-box]]
            [hickory.render :as render]
            [wiki-guides.page.controller :as page-controller]
            [wiki-guides.page.img-modal :as img-modal]
            [wiki-guides.nav :as nav]
            [wiki-guides.utils :as utils]))

(defn img-event? [event]
  (let [tag (-> event .-target .-tagName)
        parent-tag (-> event .-target .-parentNode .-tagName)]
    (and (= tag "IMG")
         (= parent-tag "BUTTON"))))

(defn button-handler-view []
  (let [on-click (react/useCallback
                   (fn [event]
                     (if (img-event? event)
                       (let [url (-> event .-target (.getAttribute "src"))]
                         (img-modal/show! (utils/image-resize url "1280"))))))]
    (react/useEffect
      (fn []
        (.addEventListener js/window "click" on-click)
        #(.removeEventListener js/window "click" on-click))
      #js [on-click])
    [:div.page
     (if @page-controller/content
       [:div {:dangerouslySetInnerHTML {:__html (render/hickory-to-html @page-controller/content)}}])]))

(defn view [route]
  [:<>
   [h-box
    :children
    [[nav/view route]
     [:f> button-handler-view]]]
   [img-modal/view]])
