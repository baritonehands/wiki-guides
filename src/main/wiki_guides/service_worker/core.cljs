(ns wiki-guides.service-worker.core
  (:require ["regenerator-runtime/runtime"]
            ["workbox-core" :as wb-core]
            ["workbox-routing" :as wb-routing]
            ["workbox-precaching" :as wb-precaching]
            ["workbox-strategies" :as wb-strategies]
            [wiki-guides.config :as config]))

(def cache-urls
  (->> ["/index.html"
        "/assets/re-com/css/bootstrap.css"
        "/assets/re-com/css/material-design-iconic-font.min.css"
        "/assets/re-com/css/re-com.css"
        "/assets/re-com/css/chosen-sprite.png"
        "/assets/re-com/css/chosen-sprite@2x.png"
        "/assets/re-com/fonts/Material-Design-Iconic-Font.eot"
        "/assets/re-com/fonts/Material-Design-Iconic-Font.svg"
        "/assets/re-com/fonts/Material-Design-Iconic-Font.ttf"
        "/assets/re-com/fonts/Material-Design-Iconic-Font.woff"
        "/assets/re-com/fonts/Material-Design-Iconic-Font.woff2"
        "/assets/re-com/scripts/detect-element-resize.js"
        "/assets/css/app.css"
        "/shared.js"
        "/app.js"]
       (map #(str config/base-url %))
       (clj->js)))

(defn main []
  (.addEventListener
    js/self
    "install"
    (fn [event]
      (let [cache-name (.-runtime wb-core/cacheNames)]
        (-> (.waitUntil
              event
              (-> js/caches
                  (.open cache-name)
                  (.then #(.addAll % cache-urls))))))))

  (.addEventListener
    js/self
    "message"
    (fn [event]
      (if (= (some-> event .-data .-type) "SKIP_WAITING")
        (.skipWaiting js/self))))

  (wb-routing/registerRoute
    #(= (.. % -request -destination) "image")
    (wb-strategies/CacheFirst.))

  (wb-routing/setDefaultHandler (wb-strategies/StaleWhileRevalidate.))

  (wb-precaching/precacheAndRoute #js [#js {:url (str config/base-url "/index.html") :revision config/revision-hash}])

  (wb-routing/registerRoute
    (wb-routing/NavigationRoute. (wb-precaching/createHandlerBoundToURL (str config/base-url "/index.html")))))