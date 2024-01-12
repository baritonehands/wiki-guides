(ns wiki-guides.guides
  (:require [promesa.core :as p]
            [re-com.core :refer [alert-box button label line input-text hyperlink modal-panel box h-box v-box]]
            [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [wiki-guides.dialog.confirm :as confirm]
            [wiki-guides.fetch :as fetch]
            [wiki-guides.nav :as nav]
            [wiki-guides.store.guide :as guide-store]
            [wiki-guides.utils :as utils])
  (:import goog.Uri))

(defn open-guide [href]
  (let [hash-href (.substring href 1)]
    (nav/set-root! hash-href)
    (rfe/push-state :wiki-guides.core/page {:page hash-href})))

(defn guide-view [{:keys [href title icon]}]
  [hyperlink
   :on-click #(open-guide href)
   :label
   [v-box
    :class "guide-item"
    :style {:text-align "center"}
    :children
    [[:img.guide-icon {:src icon :alt title}]
     [:span.guide-title title]]]])

(defonce *open (r/atom false))

(defn add-guide-button-view []
  [v-box
   :class "guide-item"
   :style {:text-align "center"}
   :children
   [[button
     :class "guide-item-add"
     :on-click #(reset! *open true)
     :label [:i.zmdi.zmdi-plus.rc-icon-larger]]
    [hyperlink
     :parts {:wrapper {:style {:align-items "center"}}}
     :on-click #(reset! *open true)
     :label [:span.guide-title "Add Guide"]]]])

(defn guide-filler []
  [box
   :class "guide-item"
   :child
   [:div]])

(defn valid-string [ratom]
  (if (and (string? @ratom)
           (empty? @ratom))
    :error))

(defn valid-guide-url [ratom]
  (if (= (valid-string ratom) :error)
    :error
    (try
      (let [url (Uri. @ratom)]
        (utils/guide-root (.getPath url)))
      (catch js/URIError _
        :error))))

(defn add-guide-dialog []
  (let [*title (r/atom nil)
        *url (r/atom nil)
        *error (r/atom false)]
    (fn []
      (if @*open
        [modal-panel
         :backdrop-on-click #(reset! *open false)
         :child
         [v-box
           :gap "5px"
           :children
          [[label :label "Title:"]
           [input-text
            :model *title
            :placeholder "Title (required)"
            :attr {:required true}
            :status (valid-string *title)
            :on-change #(reset! *title %)]
           [label :label "Guide Url:"]
           [input-text
            :model *url
            :placeholder "Guide address (required, full or just the /wikis/* part)"
            :attr {:required true}
            :status (valid-string *url)
            :on-change (fn [v]
                         (reset! *error false)
                         (reset! *url v))]
           [line :size "5px" :color "transparent"]
           (if @*error
             [alert-box
              :alert-type :danger
              :body "Error loading guide. Please edit url and try again."])
           [h-box
            :gap "10px"
            :children
            [[box
              :size "1"
              :child
              [confirm/prompt-button
               :label "Add"
               :class "btn-primary btn-lg btn-block"
               :on-click (fn []
                           (let [url (valid-guide-url *url)]
                             (if (and (not= (valid-string *title) :error)
                                      (not= url :error))
                               (-> (fetch/guide url @*title)
                                   (p/then (fn []
                                             (reset! *title nil)
                                             (reset! *url nil)
                                             (reset! *error false)
                                             (reset! *open false)
                                             (open-guide url)))
                                   (p/catch (fn []
                                              (println "Error fetching guide")
                                              (reset! *error true)))))))]]
             [box
              :size "1"
              :child
              [confirm/prompt-button
               :label "Cancel"
               :class "btn-lg btn-block"
               :on-click (fn []
                           (reset! *title nil)
                           (reset! *url nil)
                           (reset! *error false)
                           (reset! *open false))]]]]]]]))))


(defn list-view [_]
  (let [*all (r/atom nil)]
    (-> (guide-store/by-title)
        (p/then #(reset! *all %)))
    (fn []
      [h-box
       ;:gap "20px"
       :class "guide-list"
       :justify :between
       :style {:flex-flow "wrap"}
       :children
       (concat
         (for [{:keys [href] :as item} @*all]
           ^{:key href}
           [guide-view item])
         [[add-guide-button-view]
          [guide-filler]
          [guide-filler]
          [guide-filler]
          [add-guide-dialog]])])))
