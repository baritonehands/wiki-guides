(ns wiki-guides.workbox.fx
  (:require [re-frame.core :refer [dispatch reg-fx]]
            [wiki-guides.config :as config]
            ["workbox-window" :as wb-window]))

(defn auto-update-handler [wb]
  (fn [_]
    (dispatch [:workbox/update wb])))

(reg-fx
  :workbox.fx/init
  (fn [_]
    (if (js-in "serviceWorker" js/navigator)
      (let [wb (wb-window/Workbox. "worker.js" #js {:scope (str config/base-url "/")})]
        (.addEventListener wb "waiting" (auto-update-handler wb))
        (.register wb)
        (dispatch [:workbox/set-instance wb]))
      (println "ServiceWorker is not supported"))))

(reg-fx
  :workbox.fx/update
  (fn [{:keys [instance]}]
    (.addEventListener instance "controlling" (fn [_]
                                                (.reload js/window.location)))
    (.messageSkipWaiting instance)))

