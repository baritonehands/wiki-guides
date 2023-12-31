(ns wiki-guides.utils
  (:require [cljs.pprint :as pprint])
  (:import goog.Uri))

(defn ppr-str [& args]
  [:pre (with-out-str (apply pprint/pprint args))])

(defn image-resize [src width]
  (let [uri (Uri. src)]
    (doto uri
      (.removeParameter "dpr")
      (.removeParameter "quality")
      (.setParameterValue "width" width))
    (str uri)))
