(ns async-anything.test.core
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.core.async :refer [<!!]]
            [clojure.core.async.impl.protocols :as impl]
            [async-anything.core :refer [do-async with-collect let-async]]))

(defn <!!!
  "Do thread blocking takes from a channel until the result
  you get back is no longer a channel. Necessary to resolve
  values from nested go blocks (e.g. nested let-async blocks)"
  [channel]
  (loop [ch channel]
    (let [result (<!! ch)]
      (if (and (satisfies? impl/ReadPort result)
               (satisfies? impl/WritePort result))
        (recur result)
        result))))

(deftest test-do-async
  (testing "puts a value if no exception"
    (is (= 3 (<!!! (do-async (+ 1 2))))))
  (testing "puts a value if top-level form is a literal"
    (is (= 1 (<!!! (do-async 1)))))
  (testing "puts an exception if one occurs"
    (let [exc (<!!! (do-async (/ 1 0)))]
      (is (instance? ArithmeticException exc))
      (is (= "Divide by zero" (.getMessage exc)))))
  (testing "does not require a do for multiple forms"
    (is (= 3 (<!!! (do-async (str "some side effect here, who knows") (+ 1 2)))))))

(deftest test-with-collect
  (testing "runs success-body in a go block with resolved do-async vars"
    (let [val (do-async 1)]
      (is (= 3 (<!!! (with-collect [val] (+ val 2)))))))
  (testing "runs error-body if any referenced do-async block fails"
    (let [val1 (do-async 1)
          val2 (do-async (/ 1 0))]
      (is (= :fail (<!!! (with-collect [val1 val2] "this is not reached" :error :fail))))))
  (testing "runs error-body if error occurs inside success-body"
    (let [val (do-async 1)]
      (is (= :fail (<!!! (with-collect [val] (/ 1 0) :error :fail))))))
  (testing "success-body does not require a do for multiple forms"
    (let [val (do-async 1)]
      (is (= 3 (<!!! (with-collect [val] (str "some side effect") (+ val 2)))))))
  (testing "error-body does not require a do for multiple forms"
    (let [val (do-async (/ 1 0))]
      (is (= :fail (<!!! (with-collect [val] nil :error (str "some side effect") :fail)))))))

(deftest test-let-async
  (testing "vars are accessible when no errors occur"
    (is (= 3 (<!!! (let-async [val1 1 val2 2] (+ val1 val2))))))
  (testing "nesting works when no errors occur"
    (is (= 3 (<!!! (let-async [val1 1] (let-async [val2 2] (+ val1 val2)))))))
  (testing "error blocks are execute as in with-collect"
    (is (= :fail (<!!! (let-async [val1 1 val2 (/ 1 0)] nil :error :fail)))))
  (testing "first error block works when nesting"
    (is (= :fail1 (<!!! (let-async [val1 1 val2 (/ 1 0)]
                          (let-async [val3 3]
                            val3
                            :error :fail2)
                          :error :fail1)))))
  (testing "second error block works when nesting"
    (is (= :fail2 (<!!! (let-async [val1 1 val2 2]
                          (let-async [val3 (/ 1 0)]
                            val3
                            :error :fail2)
                          :error :fail1))))))
