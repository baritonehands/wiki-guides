(ns wiki-guides.nav
  (:require [clojure.string :as str]
            [promesa.core :as p]
            [reagent.core :as r]
            [re-com.core :refer [box button checkbox hyperlink hyperlink-href line popover-anchor-wrapper popover-content-wrapper title v-box]]
            ["react" :as react]
            [reitit.frontend.easy :as rfe]
            [wiki-guides.config :as config]
            [wiki-guides.store.guide :as guide-store]
            [wiki-guides.utils :as utils]))

(defonce *root (r/atom config/base-url))
(defonce *open (r/atom false))

(defn set-root! [href]
  (reset! *root href))

(defn nav-item-view [{:keys [href label on-click child]}]
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
      :href href]

     :else child)])

(defn header-view []
  [v-box
   :children
   [[nav-item-view
     {:on-click  (fn []
                   (rfe/push-state :wiki-guides.core/root)
                   (reset! guide-store/*current nil))
      :label [:<>
              [:i.zmdi.zmdi-chevron-left.rc-icon-larger]
              [:span "\u00A0\u00A0All Guides"]]}]
    [line
     :color "#CCCCCC"]
    [title
     :level :level2
     :style {:color "#337ab7"}
     :label (:title @guide-store/*current)]
    [nav-item-view {:label "Guide Home"
                    :on-click (fn []
                                (reset! *open false)
                                (rfe/push-state :wiki-guides.core/page {:page @*root}))}]
    [nav-item-view {:child [:<>
                            [checkbox
                               :label [:<>
                                       [:span "Download Guide"]
                                       [:br]
                                       [:span.nav-item-subtext
                                        "Enables search"]]
                               :model (:download @guide-store/*current)
                               :on-change #(guide-store/set-download! %)]]}]]])


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
