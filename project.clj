(defproject thieman/async-anything "0.2.0-SNAPSHOT"
  :description "Easily use core.async with blocking APIs"
  :url "https://github.com/thieman/async-anything"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"]]
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :sign-releases false}]])
