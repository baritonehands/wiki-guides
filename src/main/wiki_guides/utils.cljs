(ns wiki-guides.utils
  (:require [cljs.pprint :as pprint]))

(defn ppr-str [& args]
  [:pre (with-out-str (apply pprint/pprint args))])
