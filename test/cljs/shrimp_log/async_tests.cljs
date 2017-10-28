;; Copyright © 2017 Giuseppe Zerbo. All rights reserved.
;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.

(ns shrimp-log.async-tests
  (:require  [cljs.nodejs :as nodejs]
             [shrimp-log.core :as l]
             [shrimp-log.core-test])
  (:use-macros [redlobster.macros :only [when-realised]]
               [shrimp.test.macros :only [run-async-tests]]))

(nodejs/enable-util-print!)

(defn -main [& args]
  (l/set-opts! :out-file "async-tests.log"
               :buffer-size 1024
               :loop-delay 10
               :log-level :trace
               :throw-on-err? false
               :pretty-print? true)

  (when-realised [l/init-prom]
    (run-async-tests
     shrimp-log.core-test)))

(set! *main-cli-fn* -main)

