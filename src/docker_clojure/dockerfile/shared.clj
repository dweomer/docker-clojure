(ns docker-clojure.dockerfile.shared
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [selmer.parser :as selmer]
            [selmer.util :as selmer-util]))

;; Dockerfiles aren't HTML, so don't let Selmer escape any of the values we
;; interpolate (quotes, brackets, etc. must pass through verbatim).
(selmer-util/turn-off-escaping!)

(defn render-template
  "Render the Selmer template at resource path `tmpl` with `context`, trimming
  any trailing newline so callers control the surrounding whitespace."
  [tmpl context]
  (-> tmpl io/resource slurp (selmer/render context) str/trim-newline))

(defn get-deps [type distro-deps distro]
  (some->> distro namespace keyword (get distro-deps) type))

(def build-deps (partial get-deps :build))

(def runtime-deps (partial get-deps :runtime))

(defn all-deps [distro-deps distro]
  (set (concat (build-deps distro-deps distro)
               (runtime-deps distro-deps distro))))

(defn install-distro-deps [distro-deps {:keys [distro]}]
  (let [deps (all-deps distro-deps distro)]
    (when (seq deps)
      (case (-> distro namespace keyword)
        (:debian :debian-slim :ubuntu)
        ["apt-get update"
         (str/join " " (concat ["apt-get install -y"] deps))
         "rm -rf /var/lib/apt/lists/*"]

        :alpine
        [(str/join " " (concat ["apk add --no-cache"] deps))]

        nil))))

(defn uninstall-distro-build-deps [distro-deps {:keys [distro]}]
  (let [deps (build-deps distro-deps distro)]
    (when (seq deps)
      (case (-> distro namespace keyword)
        (:debian :debian-slim :ubuntu)
        [(str/join " " (concat ["apt-get purge -y --auto-remove"] deps))]

        :alpine
        [(str/join " " (concat ["apk del"] deps))]

        nil))))

(defn copy-resource-file!
  "Copy a file named `filename` from resources to a specified `build-dir`.
  The file contents will be passed to the `processor` fn and whatever that
  returns used in the image (default processor is `identity`)."
  ([build-dir filename]
   (copy-resource-file! build-dir filename identity identity))
  ([build-dir filename contents-processor]
   (copy-resource-file! build-dir filename contents-processor identity))
  ([build-dir filename contents-processor file-processor]
   (let [src  (-> filename io/resource io/file)
         dest (io/file build-dir filename)]
     (->> src slurp contents-processor (spit dest))
     (file-processor dest))))
