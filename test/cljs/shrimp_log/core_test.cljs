;; Copyright © 2017 Giuseppe Zerbo. All rights reserved.
;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.

(ns shrimp-log.core-test
  (:require [cljs.test :refer [deftest is]]
            [cljs.spec.alpha :as s]
            [shrimp-log.core :as l]
            [shrimp.core :as sc]
            [shrimp.test :as st]
            [cljs.pprint :as pp]
            [clojure.string :as cs])
  (:use-macros [redlobster.macros :only [when-realised]]
               [shrimp.macros :only [defer-loop]]))

(deftest log-state
  (is (s/valid? :shrimp-log.core/init-lock l/init-lock) "init-log")
  (is (s/valid? :shrimp-log.core/log-state l/log-state) "log-state")
  (is (s/valid? :shrimp-log.core/log-opts (deref l/log-state)) "log-opts")
  (st/done!))

(deftest log-loop
  (let [test-ch (sc/chan)
        spec-kw :shrimp-log.core/bundle
        chan-kw :shrimp-log.core/log-chan
        orig-ch (-> l/log-state deref (get chan-kw))
        spec-msg "check loop bundle spec"
        exp-msg "check loop bundle"
        curr-ns (namespace ::-)
        expected-vals [[:trace curr-ns "trace"]
                       [:debug curr-ns "debug"]
                       [:info curr-ns "info"]
                       [:warn curr-ns "warn"]
                       [:error curr-ns "error"]]]
    (swap! l/log-state assoc chan-kw test-ch)
    (l/trace* ::- "trace")
    (l/debug* ::- "debug")
    (l/info* ::- "info")
    (l/warn* ::- "warn")
    (l/error* ::- "error")
    (defer-loop [prom (sc/take! test-ch) [x & xs] expected-vals]
      (if x
        (when-realised [prom]
          (is (s/valid? spec-kw @prom) spec-msg)
          (is (= x @prom) exp-msg)
          (defer-recur (sc/take! test-ch) xs))
        (do
          (swap! l/log-state assoc chan-kw orig-ch)
          (sc/close! test-ch)
          (st/done!))))))

(deftest log-spy
  (let [test-ch (sc/chan)
        spec-kw :shrimp-log.core/bundle
        chan-kw :shrimp-log.core/log-chan
        orig-ch (-> l/log-state deref (get chan-kw))
        spec-msg "check spy bundle spec"
        exp-msg "check spy bundle"
        curr-ns (namespace ::-)
        foo-map {:foo 2 :bar "bar"}
        foo-map2 (assoc foo-map :new-sym 'sym)
        spy-pr (fn [v] (str "\n"
                            (cs/trim
                             (with-out-str
                               (pp/pprint v)))))
        expected-vals [[:trace curr-ns (spy-pr foo-map2)]
                       [:debug curr-ns (spy-pr foo-map2)]
                       [:warn curr-ns (spy-pr foo-map2)]]]
    (swap! l/log-state assoc chan-kw test-ch)
    (l/spy* :foo ::- :foo)
    (is (= (l/spy* :trace ::- (assoc foo-map :new-sym 'sym))
           foo-map2) "spy trace")
    (is (= (l/spy* :debug ::- (assoc foo-map :new-sym 'sym))
           foo-map2) "spy debug")
    (is (= (l/spy* :warn ::- (assoc foo-map :new-sym 'sym))
           foo-map2) "spy warn")
    (defer-loop [prom (sc/take! test-ch) [x & xs] expected-vals]
      (if x
        (when-realised [prom]
          (is (s/valid? spec-kw @prom) spec-msg)
          (is (= x @prom) exp-msg)
          (defer-recur (sc/take! test-ch) xs))
        (do
          (swap! l/log-state assoc chan-kw orig-ch)
          (sc/close! test-ch)
          (st/done!))))))
