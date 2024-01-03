(ns wiki-guides.channels
  (:require [clojure.core.async :as async]))

(defonce web-workers (async/chan 1000))
