;; Copyright © 2017 Giuseppe Zerbo. All rights reserved.
;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.

(ns shrimp-log.utils
  (:require [cljs.reader :refer [read-string]]
            [redlobster.promise :as p])
  (:use-macros [redlobster.macros :only [defer-node let-realised]]))

(defonce ^:private n-fs (js/require "fs"))
(defonce ^:private n-util (js/require "util"))
(defonce ^:private n-path (js/require "path"))
(defonce ^:private n-proc (js/require "process"))

(defn cwd
  "Return the Current Working Directory."
  []
  (.cwd n-proc))

(defn basename
  "Extract the basename from path."
  [path]
  (.basename n-path path))

(defn format*
  "Apply format to a string and a list of arguments."
  [s args]
  (apply (.-format n-util) s args))

(defn format
  "Format a string with a variable number of args."
  [s & args]
  (format* s args))

(defn tstamp
  "Return a string representation of the current date.

  Use tstamp-format to chose the method to invoke.
  "
  [tstamp-format]
  (let [date (js/Date.)]
    (case tstamp-format
      :locale (.toLocaleString date)
      :locale-date (.toLocaleDateString date)
      :locale-time (.toLocaleTimeString date)
      :date (.toDateString date)
      :time (.toTimeString date)
      :utc (.toUTCString date)
      :iso (.toISOString date)
      (.toISOString date))))

(defn append-file
  "Append asynchronously to a file and return the result in a promise."
  [path data]
  (defer-node (.appendFile n-fs path data)))

(defn is-writable?
  "Try a zero byte write on the log file to verify write permissions.

  Return true on success, false if an error occurs.
  "
  [file]
  (try
    (.appendFileSync n-fs file "")
    true
    (catch js/Error e
      false)))

(defn read-config
  "Try to read a config file from the current folder and assuming a clj extension.

  Return nil if the file does not exist or a promise with the Clojure data.
  "
  [filename]
  (let [path (.join n-path (cwd) (str filename ".clj"))]
    (when (.existsSync n-fs path)
      (let-realised [file-prom (defer-node (.readFile n-fs path) str)]
        (read-string @file-prom)))))
