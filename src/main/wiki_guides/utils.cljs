(ns wiki-guides.utils
  (:require [cljs.pprint :as pprint]
            [clojure.string :as str])
  (:import goog.Uri))

(defn ppr-str [& args]
  [:pre (with-out-str (apply pprint/pprint args))])

(defn image-resize [src width]
  (try
    (let [uri (Uri. src)]
      (doto uri
        (.removeParameter "dpr")
        (.removeParameter "quality")
        (.setParameterValue "width" width))
      (str uri))
    (catch js/URIError _
      src)))

(defn url-path [url]
  (try
    (.getPath (Uri. url))
    (catch js/URIError _
      url)))

(defn guide-root [url]
  (let [parts (->> (-> (str/replace url #"^/" "")
                       (str/split #"/"))
                   (take 2))]
    (str "/" (str/join "/" parts))))
