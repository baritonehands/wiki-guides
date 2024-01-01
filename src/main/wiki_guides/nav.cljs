(ns wiki-guides.nav
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [re-com.core :refer [box button hyperlink-href line popover-anchor-wrapper popover-content-wrapper v-box]]
            ["react" :as react]
            [wiki-guides.config :as config]))

(defonce *root (r/atom config/base-url))

(defn set-root! [href]
  (reset! *root href))

(defn nav-item-view [href label]
  [box
   :class "nav-item"
   :child
   [hyperlink-href
    :class "nav-item-link"
    :label label
    :href href]])

(defn header-view []
  [v-box
   :children
   [[nav-item-view
     config/base-url
     [:<>
      [:i.zmdi.zmdi-chevron-left.rc-icon-larger]
      [:span "\u00A0\u00A0All Guides"]]]
    [nav-item-view @*root "Guide Home"]]])

(defn update-guide-root-fn [{:keys [path-params]}]
  (fn guide-root-fn! []
    (let [page (:page path-params)
          parts (->> (-> (str/replace page #"^/" "")
                         (str/split #"/"))
                     (take 2))
          page-root (str "#/" (str/join "/" parts))]
      (if (and (= (first parts) "wikis")
               (not= page-root @*root))
        (reset! *root page-root)))
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
   [:<>
    [line :size "1px" :color "#CCCCCC"]
    [:f> view-with-hooks route]]])

(defn mobile-view []
  (let [*open (r/atom false)]
    (fn []
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
        [nav-view]]])))
