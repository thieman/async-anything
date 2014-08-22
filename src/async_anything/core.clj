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
        final-body (if (= (.indexOf body :error) -1)
                     (concat body [:error nil])
                     body)
        error-index (.indexOf final-body :error)
        success-body (first (split-at error-index final-body))
        error-body (rest (second (split-at error-index final-body)))]
    `(go (let ~let-forms
           (let [errors# (filter #(instance? Exception %) ~channels)]
             (try
               (if (seq errors#)
                 (do ~@error-body)
                 (do ~@success-body))
               (catch Exception e# ~@error-body)))))))

(defmacro let-async
  [bindings & body]
  (let [asyncify (fn [[name form]] [name `(do-async ~form)])
        async-bindings (vec (mapcat identity (map asyncify (partition 2 bindings))))
        body `(with-collect ~(vec (map first (partition 2 bindings))) ~@body)]
    `(let ~async-bindings ~body)))
