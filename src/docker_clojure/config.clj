(ns docker-clojure.config
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [docker-clojure.core :as-alias core]))

(s/def ::non-blank-string
  (s/and string? #(not (str/blank? %))))

(s/def ::jdk-version
  (s/and pos-int? #(<= 8 %)))
(s/def ::jdk-versions (s/coll-of ::jdk-version :distinct true :into #{}))

(s/def ::base-image ::non-blank-string)
(s/def ::base-images (s/coll-of ::base-image :distinct true :into #{}))

(s/def ::docker-image-name (s/and ::non-blank-string
                                  #(re-matches #"[-\w]+(?::[-\w.]+)?" %)))
(s/def ::docker-tag (s/and ::non-blank-string
                           #(re-matches #"[-\w.]+" %)))
(s/def ::base-image-tag ::docker-image-name)

(s/def ::distro qualified-keyword?)
(s/def ::distros (s/coll-of ::distro :distinct true :into #{}))

(s/def ::build-tool (s/or ::specific-tool ::non-blank-string
                          ::all-tools #(= ::core/all %)))
(s/def ::build-tool-version
  (s/nilable (s/and ::non-blank-string #(re-matches #"[\d\.]+" %))))
(s/def ::build-tools (s/map-of ::build-tool ::build-tool-version))

(s/def ::exclusions
  (s/keys :opt-un [::jdk-version ::distro ::build-tool ::build-tool-version]))

(s/def ::maintainers
  (s/coll-of ::non-blank-string :distinct true :into #{}))

(s/def ::architecture ::non-blank-string)
(s/def ::architectures (s/coll-of ::architecture :distinct true :into #{}))

(def git-repo "https://github.com/Quantisan/docker-clojure.git")

(def jdk-versions #{8 11 17 19})

(def base-images
  "Map of JDK version to base image name(s) with :default as a fallback"
  {:default ["eclipse-temurin" "debian"]})

;; The default JDK version to use for tags that don't specify one; usually the latest LTS release
(def default-jdk-version 17)

(def distros
  "Map of base image name to set of distro tags to use, namespaced by Linux
  distro type. :default key is a fallback for base images not o/w specified."
  {:default #{:alpine/alpine :ubuntu/focal :ubuntu/jammy}
   "debian" #{:debian-slim/bullseye-slim :debian/bullseye}})

(def default-architectures
  #{"amd64" "arm64v8"})

(def distro-architectures
  "Map of distro types to architectures it supports if different from
  default-architectures."
  {:alpine #{"amd64"}})

(def default-distros
  "The default distro to use for tags that don't specify one, keyed by jdk-version.
  :default is a fallback for jdk versions not o/w specified."
  {:default :ubuntu/jammy})

(def build-tools
  {"lein"       "2.9.10"
   "boot"       "2.8.3"
   "tools-deps" "1.11.1.1200"})

(def default-build-tool "tools-deps")

(def installer-hashes
  {"lein"       {"2.9.10" "dbb84d13d6df5b85bbf7f89a39daeed103133c24a4686d037fe6bd65e38e7f32"}
   "boot"       {"2.8.3" "0ccd697f2027e7e1cd3be3d62721057cbc841585740d0aaa9fbb485d7b1f17c3"}
   "tools-deps" {"1.11.1.1182" "0e80041419bb91e142e2e8683e4dad6faf79958b603bb63b2a93bdd62c2a4f14"
                 "1.11.1.1189" "1afd91b165776615eca84cce2271e5fe5d5818c55dee0f082b1304bb1464b3e8"
                 "1.11.1.1200" "694381cb78d44f143bdcc38657507f012ebac4009bc57cec67abef1675447878"}})

(def exclusions ; don't build these for whatever reason(s)
  #{; boot on JDK 8 & Alpine is encountering a TLS handshake error trying to
    ; download boot as of 2022-11-17. Probably would deprecate one or both of
    ; JDK 8 and/or boot variants before spending much time working around an
    ; issue like this.
    {:jdk-version 8
     :build-tool  "boot"
     :distro      :alpine/alpine}
    ;; commented out example
    #_{:jdk-version 8
       :distro      :alpine/alpine}})

(def maintainers
  ["Paul Lam <paul@quantisan.com> (@Quantisan)"
   "Wes Morgan <wes@wesmorgan.me> (@cap10morgan)"])