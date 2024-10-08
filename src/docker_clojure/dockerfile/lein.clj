(ns docker-clojure.dockerfile.lein
  (:require [clojure.string :as str]
            [docker-clojure.dockerfile.shared
             :refer [concat-commands entrypoint install-distro-deps
                     uninstall-distro-build-deps]]))

(defn prereqs [_ _] nil)

(def distro-deps
  {:debian-slim {:build   #{"wget" "gnupg"}
                 :runtime #{"git"}}
   :debian      {:build   #{"wget" "gnupg"}
                 :runtime #{"make" "git"}}
   :ubuntu      {:build   #{"wget" "gnupg"}
                 :runtime #{"make" "git"}}
   :alpine      {:build   #{"tar" "gnupg" "openssl" "ca-certificates"}
                 :runtime #{"bash" "git"}}})

(def install-deps (partial install-distro-deps distro-deps))

(def uninstall-build-deps (partial uninstall-distro-build-deps distro-deps))

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
  (let [install-dep-cmds   (install-deps variant)
        uninstall-dep-cmds (uninstall-build-deps variant)]
    (-> [(format "ENV LEIN_VERSION=%s" build-tool-version)
         "ENV LEIN_INSTALL=/usr/local/bin/"
         ""
         "WORKDIR /tmp"
         ""
         "# Download the whole repo as an archive"
         "RUN set -eux; \\"]
        (concat-commands install-dep-cmds)
        (concat-commands
          ["mkdir -p $LEIN_INSTALL"
           "wget -q https://codeberg.org/leiningen/leiningen/raw/tag/$LEIN_VERSION/bin/lein-pkg"
           "echo \"Comparing lein-pkg checksum ...\""
           "sha256sum lein-pkg"
           (str "echo \"" (get-in installer-hashes ["lein" build-tool-version]) " *lein-pkg\" | sha256sum -c -")
           "mv lein-pkg $LEIN_INSTALL/lein"
           "chmod 0755 $LEIN_INSTALL/lein"
           "export GNUPGHOME=\"$(mktemp -d)\""
           "export FILENAME_EXT=jar" ; used to be zip but hopefully it's always jar now?
           (str "gpg --batch --keyserver hkps://keyserver.ubuntu.com --recv-keys "
                (gpg-key build-tool-version))
           "wget -q https://codeberg.org/leiningen/leiningen/releases/download/$LEIN_VERSION/leiningen-$LEIN_VERSION-standalone.$FILENAME_EXT"
           "wget -q https://codeberg.org/leiningen/leiningen/releases/download/$LEIN_VERSION/leiningen-$LEIN_VERSION-standalone.$FILENAME_EXT.asc"
           "echo \"Verifying file PGP signature...\""
           "gpg --batch --verify leiningen-$LEIN_VERSION-standalone.$FILENAME_EXT.asc leiningen-$LEIN_VERSION-standalone.$FILENAME_EXT"
           "gpgconf --kill all"
           "rm -rf \"$GNUPGHOME\" leiningen-$LEIN_VERSION-standalone.$FILENAME_EXT.asc"
           "mkdir -p /usr/share/java"
           "mkdir -p /root/.lein"
           "mv leiningen-$LEIN_VERSION-standalone.$FILENAME_EXT /usr/share/java/leiningen-$LEIN_VERSION-standalone.jar"]
          (empty? uninstall-dep-cmds))
        (concat-commands uninstall-dep-cmds :end)
        (concat
          [""
           "ENV PATH=$PATH:$LEIN_INSTALL"
           "ENV LEIN_ROOT 1"
           ""
           "# Install clojure 1.12.0 so users don't have to download it every time"
           "RUN echo '(defproject dummy \"\" :dependencies [[org.clojure/clojure \"1.12.0\"]])' > project.clj \\"
           "  && lein deps && rm project.clj"])

        (->> (remove nil?)))))

(defn command [{:keys [jdk-version]}]
  (if (>= jdk-version 16)
    ["CMD [\"repl\"]"]
    ["CMD [\"lein\", \"repl\"]"]))

(defn contents [installer-hashes variant]
  (concat (install installer-hashes variant) [""] (entrypoint variant) (command variant)))
