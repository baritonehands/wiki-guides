(ns wiki-guides.core
  (:require [reagent.core :as r]
            [reagent.dom.client :as rdom]
            [re-frame.core :refer [dispatch-sync]]
            [reitit.core :as router]
            [reitit.spec :as rs]
            [reitit.frontend.easy :as rfe]
            [reitit.frontend.controllers :as rfc]
            [wiki-guides.guides :as guides]
            [wiki-guides.page :as page]
            [wiki-guides.page.controller :as page-controller]
            [wiki-guides.workbox.events]
            [wiki-guides.web-workers :as web-workers]
            [wiki-guides.fetch :as fetch]))

(defonce current-route (r/atom nil))

(def routes
  [["/" {:name ::root
         :view guides/list-view}]
   ["/*page" {:name        ::page
              :view        page/view
              :controllers [page-controller/desc]}]])

(def router
  (router/router routes {:validate rs/validate}))

(defn app-component []
  (if-let [route @current-route]
    [(:view (:data route)) route]))

(defonce root (rdom/create-root (.getElementById js/document "app-container")))

(defn ^:dev/after-load force-rerender []
  (rdom/render root [app-component]))

(defn ^:export main []
  (web-workers/init!)
  (fetch/init!)
  (dispatch-sync [:workbox/init])
  (rdom/render root [app-component])
  (rfe/start!
    router
    (fn [new-match]
      (swap! current-route (fn [old-match]
                             (if new-match
                               (assoc new-match :controllers (rfc/apply-controllers (:controllers old-match) new-match))))))
    {}))
