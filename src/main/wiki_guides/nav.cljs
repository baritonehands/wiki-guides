(ns wiki-guides.nav
  (:require [clojure.string :as str]
            [promesa.core :as p]
            [reagent.core :as r]
            [re-com.core :refer [box button checkbox hyperlink hyperlink-href line popover-anchor-wrapper popover-content-wrapper progress-bar title v-box]]
            ["react" :as react]
            [reitit.frontend.easy :as rfe]
            [wiki-guides.config :as config]
            [wiki-guides.fetch :as fetch]
            [wiki-guides.search :as search]
            [wiki-guides.store.guide :as guide-store]
            [wiki-guides.store.page :as page-store]
            [wiki-guides.dialog.confirm :as confirm]
            [wiki-guides.utils :as utils]))

(defonce *root (r/atom config/base-url))
(defonce *open (r/atom false))

(defn set-root! [href]
  (reset! *root href))

(defn nav-item-view [{:keys [href label on-click child]}]
  [box
   :class "nav-item"
   :attr (if on-click
           {:on-click on-click}
           {})
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

(defonce *progress (r/atom [0 0]))

(defn get-progress! []
  (p/let [[progress total] (page-store/progress (:href @guide-store/*current))]
    (reset! *progress [progress total])))

(defn progress-view []
  (let [cb (react/useCallback get-progress! #js[get-progress!])
        [progress total] @*progress
        percent (if (pos? total)
                  (.toFixed (* (/ progress total) 100))
                  0)
        done? (= percent "100")]
    (react/useEffect
      (fn []
        (let [timer (js/setInterval cb 1000)]
          #(js/clearInterval timer)))
      #js[cb])
    [:<>
     [:div.nav-item-subtext (str "Downloaded " progress " of " total (if-not done? "..."))]
     [progress-bar
      :model percent
      :striped? (not done?)]]))

(defn header-view [route]
  (let [*confirm (r/atom false)]
    (fn []
      [:<>
       (if @*confirm
         [confirm/view
          {:title "Delete Guide Data?"
           :text "This will delete this guide's data in your browser. Are you sure?"
           :confirm-label "Confirm"
           :cancel-label "Cancel"
           :on-confirm (fn []
                         (guide-store/set-download! false)
                         (page-store/delete-all (:href @guide-store/*current))
                         (reset! *confirm false))
           :on-cancel #(reset! *confirm false)}])
       [v-box
        :children
        [[nav-item-view
          {:on-click (fn []
                       (rfe/push-state :wiki-guides.core/root)
                       (reset! guide-store/*current nil)
                       (reset! search/*fs-document nil)
                       (reset! *open false))
           :label    [:<>
                      [:i.zmdi.zmdi-chevron-left.rc-icon-larger]
                      [:span "\u00A0\u00A0All Guides"]]}]
         [line
          :color "#CCCCCC"]
         [nav-item-view
          {:child
           [title
            :level :level2
            :class "guide-title"
            :style {:color "#337ab7"}
            :label (:title @guide-store/*current)]}]
         (if (:download @guide-store/*current)
           [:f> progress-view])
         [nav-item-view {:child [:<>
                                 [checkbox
                                  :label [:<>
                                          [:span "Download Guide"]
                                          [:br]
                                          [:span.nav-item-subtext
                                           "Enables search"]]
                                  :label-style {:cursor "pointer"}
                                  :model (:download @guide-store/*current)
                                  :on-change (fn [download]
                                               (if download
                                                 (do
                                                   (guide-store/set-download! true)
                                                   (search/init!)
                                                   (fetch/promise (str "/" (-> route :path-params :page))))
                                                 (reset! *confirm true)))]]}]
         [line
          :color "#CCCCCC"
          :style {:margin "8px 0"}]
         [nav-item-view {:label    [:<>
                                    [:i.zmdi.zmdi-home]
                                    [:span "\u00A0Guide Home"]]
                         :on-click (fn []
                                     (reset! *open false)
                                     (rfe/push-state :wiki-guides.core/page {:page @*root}))}]
         [nav-item-view
          {:on-click (fn []
                       (reset! search/*open true)
                       (reset! *open false))
           :label    [:<>
                      [:i.zmdi.zmdi-search]
                      [:span "\u00A0Search"]]}]]]])))

(defn update-guide-root-fn [{:keys [path-params]}]
  (fn guide-root-fn! []
    (let [page-root (.substring (utils/guide-root (:page path-params)) 1)]
      (if (and (str/starts-with? page-root "wikis/")
               (not= page-root @*root))
        (set-root! page-root)))
    js/undefined))

(defn nav-view [route]
  [box
   :size "auto"
   :class "nav-container"
   :child
   [:div.nav
    [header-view route]]])

(defn view-with-hooks [route]
  (let [guide-root-fn! (react/useCallback (update-guide-root-fn route) #js [route])]
    (react/useEffect guide-root-fn! #js[guide-root-fn!])
    [nav-view route]))

(defn desktop-view [route]
  [box
   :size "250px"
   :class "nav-desktop"
   :child
   [:f> view-with-hooks route]])

(defn mobile-view [route]
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
    [nav-view route]]])
