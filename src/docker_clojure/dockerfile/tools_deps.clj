(ns docker-clojure.dockerfile.tools-deps
  (:require [docker-clojure.dockerfile.shared
             :refer [copy-resource-file! install-distro-deps render-template
                     uninstall-distro-build-deps]]))

(defn prereqs [dir _variant]
  (copy-resource-file! dir "rlwrap.retry" identity
                       #(.setExecutable % true false)))

(def distro-deps
  {:debian-slim {:build   #{"curl"}
                 :runtime #{"rlwrap" "make" "git"}}
   :debian      {:build   #{"curl"}
                 :runtime #{"rlwrap" "make" "git"}}
   :ubuntu      {:build   #{}
                 ;; install curl as a runtime dep b/c we need it at build time
                 ;; but upstream includes it so we don't want to uninstall it
                 :runtime #{"rlwrap" "make" "git" "curl"}}
   :alpine      {:build   #{"curl"}
                 :runtime #{"bash" "make" "git" "rlwrap"}}})

(def install-deps (partial install-distro-deps distro-deps))

(def uninstall-build-deps (partial uninstall-distro-build-deps distro-deps))

(defn install [installer-hashes {:keys [build-tool-version] :as variant}]
  (render-template
   "templates/tools-deps.tmpl"
   {:clojure-version build-tool-version
    :install-hash    (get-in installer-hashes ["tools-deps" build-tool-version])
    :install-deps    (install-deps variant)
    :uninstall-deps  (uninstall-build-deps variant)}))

(defn command [{:keys [jdk-version]}]
  (if (>= jdk-version 16)
    "CMD [\"-M\", \"--repl\"]"
    "CMD [\"clj\"]"))
