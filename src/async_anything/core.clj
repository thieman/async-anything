(ns async-anything.core
  (:require [clojure.core.async :refer [go chan >!! <!]]))

(defmacro do-async
  "Perform the body in a future, returns a channel to which
  the result is pushed when it completes. If an exception is
  encountered, the exception is pushed to the channel in place
  of a correct result."
  [& body]
  `(let [channel# (chan 1)]
     (future (try
               (let [result# (do ~@body)]
                 (>!! channel# result#))
               (catch Exception e# (>!! channel# e#))))
     channel#))

(defmacro with-collect
  "Gather results from channels into variables. Takes a body
  optionally including an :error body at its tail. Executes
  the initial body if successful, or the error body if
  any channels return an exception."
  [channels & body]
  (let [let-forms (->> channels
                      (map (fn [channel] (vector channel `(<! ~channel))))
                      (mapcat identity)
                      (vec))
        body (if (= (.indexOf body :error) -1)
               (conj body :error nil)
               body)
        error-index (.indexOf body :error)
        success-body (first (split-at error-index body))
        error-body (rest (second (split-at error-index body)))]
    `(go (let ~let-forms
           (let [errors# (filter #(instance? Exception %) ~channels)]
             (try
               (if (seq errors#)
                 ~@error-body
                 ~@success-body)
               (catch Exception e# ~@error-body)))))))
