(ns wiki-guides.guides.edit
  (:require [promesa.core :as p]
            [reagent.core :as r]
            [re-com.core :refer [alert-box button label line input-text hyperlink modal-panel box h-box v-box]]
            ["react" :as react]
            [reitit.frontend.easy :as rfe]
            [wiki-guides.dialog.confirm :as confirm]
            [wiki-guides.fetch :as fetch]
            [wiki-guides.search :as search]
            [wiki-guides.store.guide :as guide-store]
            [wiki-guides.store.page :as page-store]))

(defonce *open (r/atom false))

(defn valid-string [ratom]
  (if (and (string? @ratom)
           (empty? @ratom))
    :error))

(defn- dialog-with-hooks []
  (let [*title (r/atom nil)
        *confirm (r/atom false)]
    (fn []
      (react/useEffect
        (fn []
          (reset! *title (:title @guide-store/*current))
          js/undefined)
        #js[(:title @guide-store/*current)])
      [:<>
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
            [line :size "5px" :color "transparent"]
            [confirm/prompt-button
             :label "Delete Guide"
             :class "btn-danger btn-lg btn-block"
             :on-click (fn []
                         (reset! *confirm true))]
            [line :size "5px" :color "transparent"]
            [h-box
             :gap "10px"
             :children
             [[box
               :size "1"
               :child
               [confirm/prompt-button
                :label "Update"
                :class "btn-primary btn-lg btn-block"
                :on-click (fn []
                            (guide-store/set-title! @*title)
                            (reset! *open false))]]
              [box
               :size "1"
               :child
               [confirm/prompt-button
                :label "Cancel"
                :class "btn-lg btn-block"
                :on-click (fn []
                            (reset! *title (:title @guide-store/*current))
                            (reset! *open false))]]]]]]])
       (if @*confirm
         [confirm/view
          {:title "Delete Guide?"
           :text "This will delete this guide's data in your browser and remove it from the list of guides. Are you sure?"
           :confirm-label "Confirm"
           :confirm-class "btn-danger"
           :cancel-label "Cancel"
           :on-confirm (fn []
                         (let [guide-href (:href @guide-store/*current)]
                           (page-store/delete-all guide-href)
                           (-> (guide-store/delete guide-href)
                               (p/then
                                 (fn []
                                   (reset! *confirm false)
                                   (reset! *open false)
                                   (rfe/push-state :wiki-guides.core/root)
                                   (reset! guide-store/*current nil)
                                   (reset! search/*fs-document nil))))))
           :on-cancel #(reset! *confirm false)}])])))

(defn dialog []
  [:f> dialog-with-hooks])
