(ns wiki-guides.nav
  (:require [clojure.string :as str]
            [promesa.core :as p]
            [reagent.core :as r]
            [re-com.core :refer [box button hyperlink hyperlink-href popover-anchor-wrapper popover-content-wrapper v-box]]
            ["react" :as react]
            [reitit.frontend.easy :as rfe]
            [wiki-guides.config :as config]
            [wiki-guides.store.guide :as guide-store]
            [wiki-guides.utils :as utils]))

(defonce *root (r/atom config/base-url))
(defonce *open (r/atom false))

(defn set-root! [href]
  (reset! *root href))

(defn nav-item-view [{:keys [href label on-click]}]
  [box
   :class "nav-item"
   :child
   (cond
     on-click
     [hyperlink
      :class "nav-item-link"
      :label label
      :on-click on-click]

     href
     [hyperlink-href
      :class "nav-item-link"
      :label label
      :href href])])

(defn header-view []
  [v-box
   :children
   [[nav-item-view
     {:href  config/base-url
      :label [:<>
              [:i.zmdi.zmdi-chevron-left.rc-icon-larger]
              [:span "\u00A0\u00A0All Guides"]]}]
    [nav-item-view {:label "Guide Home"
                    :on-click (fn []
                                (reset! *open false)
                                (rfe/push-state :wiki-guides.core/page {:page @*root}))}]]])

(defn update-guide-root-fn [{:keys [path-params]}]
  (fn guide-root-fn! []
    (let [page-root (.substring (utils/guide-root (:page path-params)) 1)]
      (if (and (str/starts-with? page-root "#/wikis")
               (not= page-root @*root))
        (set-root! page-root)))
    js/undefined))

(defn nav-view []
  [box
   :size "auto"
   :class "nav-container"
   :child
   [:div.nav
    [header-view]]])

(defn view-with-hooks [route]
  (let [guide-root-fn! (react/useCallback (update-guide-root-fn route) #js [route])]
    (react/useEffect guide-root-fn! #js[guide-root-fn!])
    [nav-view]))

(defn desktop-view [route]
  [box
   :size "250px"
   :class "nav-desktop"
   :child
   [:f> view-with-hooks route]])

(defn mobile-view []
  [popover-anchor-wrapper
   :showing? @*open
   :position :below-right
   :anchor
   [box
    :class "nav-mobile-button"
    :child
    [button
     :label [:i.zmdi.zmdi-menu.rc-icon-larger]
     :on-click #(swap! *open not)]]
   :popover
   [popover-content-wrapper
    :width "250px"
    :body
    [nav-view]]])
