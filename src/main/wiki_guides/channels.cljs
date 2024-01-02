(ns wiki-guides.channels
  (:require [clojure.core.async :as async]))

(defonce fetch (async/chan 1000))

(defonce web-workers (async/chan 1000))
