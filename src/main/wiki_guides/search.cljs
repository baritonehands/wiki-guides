(ns wiki-guides.search
  (:require [promesa.core :as p]
            [re-com.core :refer [button line input-text hyperlink modal-panel scroller h-box v-box]]
            ["flexsearch" :as flexsearch]
            [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [wiki-guides.store.guide :as guide-store]
            [wiki-guides.store.page :as page-store]))

(defonce *fs-document (atom nil))
(defonce *open (r/atom false))

(defn add [href page]
  (println "Adding" href)
  (.add @*fs-document href page))

(defn delete [href]
  (println "Removing" href)
  (.remove @*fs-document href))

(defn init! []
  (let [guide @guide-store/*current]
    (when (and (:download guide)
               (nil? @*fs-document))
      (reset! *fs-document (flexsearch/Document.
                             #js {:worker   true
                                  :tokenize "forward"
                                  :stemmer true
                                  :document #js {:id    "href"
                                                 :index #js ["title" "text"]}}))
      (-> (page-store/all-for-search (:href guide))
          (p/then #(doseq [page %]
                     (add (.-href page) page)))))))

(defn search [term]
  (-> (.searchAsync ^flexsearch/Document @*fs-document term)
      (p/then #(js->clj % :keywordize-keys true))
      (p/then
        (fn [results]
          (p/all (for [{:keys [field result]} results]
                   (p/let [pages (page-store/fetch-ids result)]
                     [field pages])))))))

(defn dialog []
  (let [*term (r/atom nil)
        *results (r/atom [])]
    (fn []
      (if @*open
        [modal-panel
         :backdrop-on-click (fn []
                              (reset! *open false)
                              (reset! *term nil)
                              (reset! *results []))
         :child
         [v-box
          :width "250px"
          :height "80vh"
          :children
          [[input-text
            :placeholder "Search..."
            :model *term
            :change-on-blur? false
            :on-change (fn [term]
                         (reset! *term term)
                         (p/let [results (search term)
                                 m (into {} results)
                                 flattened (->> (for [field ["title" "text"]
                                                      page (get m field)]
                                                  (select-keys page [:href :title]))
                                                (distinct)
                                                (take 50))]
                           (reset! *results flattened)))]
           [:div.small {:style {:padding-top "4px"
                                :color "#666666"}}
            (str (count @*results) " results")]
           [line :color "#CCCCCC"
            :style {:margin "8px 0"}]
           [scroller
            :child
            [v-box
             :children
             (for [result @*results]
               [hyperlink
                :on-click (fn []
                            (reset! *open false)
                            (reset! *term nil)
                            (reset! *results [])
                            (rfe/push-state :wiki-guides.core/page {:page (.substring (:href result) 1)}))
                :label (:title result)])]]]]]))))
