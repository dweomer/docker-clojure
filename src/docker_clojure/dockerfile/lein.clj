(ns docker-clojure.dockerfile.lein
  (:require [clojure.string :as str]
            [docker-clojure.dockerfile.shared
             :refer [install-distro-deps render-template
                     uninstall-distro-build-deps]]))

(defn prereqs [_ _] nil)

(def distro-deps
  {:debian-slim {:build   #{"wget" "gnupg"}
                 :runtime #{}}
   :debian      {:build   #{"wget" "gnupg"}
                 :runtime #{"make"}}
   :ubuntu      {:build   #{"wget" "gnupg"}
                 :runtime #{"make"}}
   :alpine      {:build   #{"tar" "gnupg" "openssl" "ca-certificates"}
                 :runtime #{"bash"}}})

(def install-deps (partial install-distro-deps distro-deps))

(def uninstall-build-deps (partial uninstall-distro-build-deps distro-deps))

;; Clojure version pre-installed into lein images so users don't download it on
;; first use.
(def ^:const bundled-clojure-version "1.12.1")

(def ^:const old-key "6A2D483DB59437EBB97D09B1040193357D0606ED")
(def ^:const new-key "9D13D9426A0814B3373CF5E3D8A8243577A7859F")

(defn gpg-key
  [version]
  (let [[major minor] (map #(Integer/parseInt %) (str/split version #"\."))]
    (cond
      (< 2 major) new-key
      (and (= 2 major) (< 10 minor)) new-key
      :else old-key)))

(defn install [installer-hashes {:keys [build-tool-version] :as variant}]
  (render-template
   "templates/lein.tmpl"
   {:lein-version    build-tool-version
    :clojure-version bundled-clojure-version
    :lein-pkg-hash   (get-in installer-hashes ["lein" build-tool-version])
    :gpg-key         (gpg-key build-tool-version)
    :install-deps    (install-deps variant)
    :uninstall-deps  (uninstall-build-deps variant)}))

(defn command [{:keys [jdk-version]}]
  (if (>= jdk-version 16)
    "CMD [\"repl\"]"
    "CMD [\"lein\", \"repl\"]"))
