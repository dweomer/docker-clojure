(ns docker-clojure.dockerfile-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [docker-clojure.dockerfile :refer [build-dir contents]]
            [docker-clojure.config :as cfg]
            [docker-clojure.dockerfile.lein :as lein]
            [docker-clojure.dockerfile.tools-deps :as tools-deps]))

(deftest build-dir-test
  (testing "replaces colons with hyphens in tag"
    (is (= "target/openjdk-11-alpine/lein"
           (build-dir {:base-image-tag "openjdk:11-alpine"
                       :build-tool     "lein"})))))

(deftest contents-test
  (testing "includes 'FROM base-image'"
    (is (str/includes? (contents cfg/installer-hashes
                                 {:base-image-tag "base:foo"
                                  :distro         :distro/distro
                                  :build-tool     "tools-deps"
                                  :jdk-version    11})
                       "FROM base:foo")))
  (testing "has no labels (Docker recommends against for base images)"
    (is (not (str/includes? (contents cfg/installer-hashes
                                      {:base-image-tag "base:foo"
                                       :distro         :distro/distro
                                       :build-tool     "tools-deps"
                                       :maintainer     "Me Myself"
                                       :jdk-version    11})
                            "LABEL "))))
  (testing "lein variant includes lein-specific contents"
    (with-redefs [lein/install (constantly "leiningen vs. the ants")]
      (is (str/includes? (contents cfg/installer-hashes
                                   {:base-image-tag "base:foo"
                                    :distro         :distro/distro
                                    :build-tool     "lein"
                                    :maintainer     "Me Myself"
                                    :jdk-version    11})
                         "leiningen vs. the ants"))))
  (testing "tools-deps variant includes tools-deps-specific contents"
    (with-redefs [tools-deps/install (constantly
                                      "Tools Deps is not a build tool")]
      (is (str/includes? (contents cfg/installer-hashes
                                   {:base-image-tag "base:foo"
                                    :distro         :distro/distro
                                    :build-tool     "tools-deps"
                                    :maintainer     "Me Myself"
                                    :jdk-version    11})
                         "Tools Deps is not a build tool")))))
