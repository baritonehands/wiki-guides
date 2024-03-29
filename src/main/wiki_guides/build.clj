(ns wiki-guides.build
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.tools.reader.edn :as edn]
            [shadow.cljs.devtools.api :as shadow]
            [clojure.string :as str])
  (:import (java.io PushbackReader PrintWriter)
           (com.github.mustachejava DefaultMustacheFactory)))

(defn copy-resource [filename]
  (let [in-path (str "public/assets/" filename)
        out-path (str "pwa/wiki-guides/assets/re-com/" filename)]
    (with-open [in (io/input-stream (io/resource in-path))]
      (io/make-parents out-path)
      (io/copy in (io/file out-path)))))

(defn copy-all-resources []
  (doseq [filename ["css/bootstrap.css"
                    "css/material-design-iconic-font.min.css"
                    "css/re-com.css"
                    "css/chosen-sprite.png"
                    "css/chosen-sprite@2x.png"
                    "fonts/Material-Design-Iconic-Font.eot"
                    "fonts/Material-Design-Iconic-Font.svg"
                    "fonts/Material-Design-Iconic-Font.ttf"
                    "fonts/Material-Design-Iconic-Font.woff"
                    "fonts/Material-Design-Iconic-Font.woff2"
                    "scripts/detect-element-resize.js"]]
    (copy-resource filename))
  (io/copy (io/file "node_modules/flexsearch/dist/flexsearch.bundle.js")
           (io/file "pwa/wiki-guides/assets/flexsearch.bundle.js")))

(defn delete-recursive
  "Recursively delete a directory."
  [^java.io.File file]
  ;; when `file` is a directory, list its entries and call this
  ;; function with each entry. can't `recur` here as it's not a tail
  ;; position, sadly. could cause a stack overflow for many entries?
  ;; thanks to @nikolavojicic for the idea to use `run!` instead of
  ;; `doseq` :)
  (when (.isDirectory file)
    (doseq [dir-file (.listFiles file)]
      (delete-recursive dir-file)))
  ;; delete the file or directory. if it it's a file, it's easily
  ;; deletable. if it's a directory, we already have deleted all its
  ;; contents with the code above (remember?)
  (when (.exists file)
    (io/delete-file file)))

(defn clean []
  (delete-recursive (io/file "pwa" "wiki-guides" "cljs-runtime"))
  (delete-recursive (io/file "pwa" "wiki-guides" "shared.js"))
  (delete-recursive (io/file "pwa" "wiki-guides" "app.js"))
  (delete-recursive (io/file "pwa" "wiki-guides" "index.html"))
  (delete-recursive (io/file "pwa" "wiki-guides" "worker.js"))
  (delete-recursive (io/file "pwa" "wiki-guides" "manifest.edn"))
  (delete-recursive (io/file "pwa" "wiki-guides" "manifest.json")))

(defn load-manifest []
  (-> (io/file "pwa/wiki-guides/manifest.edn")
      (io/reader)
      (PushbackReader.)
      (edn/read)))

(def mustach-factory (DefaultMustacheFactory.))

(defn render-mustache [filename scopes]
  (with-open [writer (io/writer (io/file "pwa" "wiki-guides" filename))]
    (let [mustache (.compile mustach-factory (str "templates/" filename ".mustache"))]
      (-> mustache
          (.execute writer scopes)
          (.flush)))))

(defn render-static-files [base-url]
  (let [scopes (->> (for [{:keys       [output-name]
                           module-name :name} (load-manifest)]
                      [(str (name module-name) "_filename") output-name])
                    (into {"base_url" base-url}))]
    (render-mustache "index.html" scopes)
    (render-mustache "manifest.json" scopes)))

(defn dev
  {:shadow/requires-server true}
  []
  (clean)
  (shadow/watch :web)
  (render-static-files "/wiki-guides"))

(defn release
  []
  (clean)
  (shadow/release
    :web
    {:config-merge
     [{:closure-defines {'wiki-guides.config/revision-hash (str/trim-newline (slurp "manifest.uuid"))}}]})
  (render-static-files "/wiki-guides"))

(defn print-sh [& args]
  (let [{:keys [out err]} (apply sh args)]
    (if out
      (println out))
    (if err
      (.println *err* err))))

(defn gh-pages [out-path]
  (print-sh "cp" "-R" (.getAbsolutePath (io/file "pwa/*")) (.getAbsolutePath (io/file out-path)))
  (print-sh "sed" "-i" "''" "'s:href=\"/\":href=\"/wiki-guides/\":g'" (.getAbsolutePath (io/file out-path "index.html"))))