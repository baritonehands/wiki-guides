(ns wiki-guides.page
  (:require ["react" :as react]
            [re-com.core :refer [line h-box scroller throbber]]
            [reagent.core :as r]
            [wiki-guides.guides.edit :as guide-edit]
            [wiki-guides.page.controller :as page-controller]
            [wiki-guides.page.img-modal :as img-modal]
            [wiki-guides.nav :as nav]
            [wiki-guides.search :as search]
            [wiki-guides.utils :as utils]))

(defn img-event? [event]
  (let [tag (-> event .-target .-tagName)
        parent-tag (-> event .-target .-parentNode .-tagName)]
    (and (= tag "IMG")
         (= parent-tag "BUTTON"))))

(defn button-handler-view [route]
  (let [*scroller-ref (r/atom nil)]
    (fn [route]
      (let [on-click (react/useCallback
                       (fn [event]
                         (if (img-event? event)
                           (if-let [url (-> event .-target (.getAttribute "data-modal-src"))]
                             (img-modal/show! url)
                             (if-let [url (-> event .-target (.getAttribute "src"))]
                               (img-modal/show! (utils/image-resize url "1280")))))))
            fragment (:fragment route)]
        (react/useEffect
          (fn []
            (.addEventListener js/window "click" on-click)
            #(.removeEventListener js/window "click" on-click))
          #js [on-click])
        (react/useLayoutEffect
          (fn []
            (if (nil? fragment)
              (set! (. @*scroller-ref -scrollTop) 0)
              (some-> (.getElementById js/document fragment)
                      (.scrollIntoView)))
            js/undefined)
          #js[@page-controller/*content fragment])
        [scroller
         :attr {:ref #(reset! *scroller-ref %)}
         :child
         [:div.page
          [nav/mobile-view route]
          (if @page-controller/*content
            [:div {:dangerouslySetInnerHTML {:__html @page-controller/*content}}]
            [throbber
             :size :large
             :style {:margin "64px auto"}])]]))))

(defn view [route]
  [:<>
   [h-box
    :children
    [[nav/desktop-view route]
     [line :size "1px" :color "#CCCCCC"]
     [:f> button-handler-view route]]]
   [img-modal/view]
   [search/dialog]
   [guide-edit/dialog]])
