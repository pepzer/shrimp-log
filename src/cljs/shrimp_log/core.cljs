;; Copyright © 2017 Giuseppe Zerbo. All rights reserved.
;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.

(ns shrimp-log.core
  (:require [redlobster.promise :as p]
            [shrimp.core :as sc]
            [shrimp-log.utils :as u]
            [clojure.string :as cs]
            [cljs.pprint :as pp]
            [clojure.spec.alpha :as s])
  (:use-macros [redlobster.macros :only [let-realised
                                         when-realised
                                         defer-node]]
               [shrimp.macros :only [defer]]))

(def ^:private level->int {:trace 1
                           :debug 2
                           :info 3
                           :warn 4
                           :error 5
                           :none 6})

(let [atom? #(instance? Atom %)
      nil-inside? #(nil? (deref %))
      symbol-inside? #(symbol? (deref %))
      map-inside? #(map? (deref %))]

  (s/def ::init-lock (s/and atom? (s/or :nil-in nil-inside?
                                        :sym-in symbol-inside?)))
  (s/def ::log-state (s/and atom? (s/or :nil-in nil-inside?
                                        :map-in map-inside?))))
(defonce ^:private init-lock (atom nil))
(defonce ^:private init-prom (p/promise))
(defonce ^:private log-state (atom nil))

(s/def ::log-level-tag #(int? (level->int %)))
(s/def ::log-level (s/or :level-tag ::log-level-tag
                         :level-int (s/and int?
                                           pos?
                                           #(<= % (count (keys level->int))))))
(s/def ::default-file string?)
(s/def ::out-file (s/or :no-file #{:stdout :log-file}
                        :file (s/and string? u/is-writable?)))
(s/def ::log-delay int?)
(s/def ::tstamp-format #{:iso :utc :date :time
                         :locale :locale-date :locale-time})
(s/def ::buffer-size (s/and int? pos?))
(s/def ::throw-on-err? boolean?)
(s/def ::pretty-print? boolean?)
(s/def ::verbose? boolean?)
(s/def ::log-chan #(instance? sc/Chan %))

(s/def ::log-opts (s/keys :req [::buffer-size
                                ::log-level
                                ::default-file
                                ::out-file
                                ::log-delay
                                ::tstamp-format
                                ::throw-on-err?
                                ::pretty-print?
                                ::verbose?]
                          :opt [::log-chan]))

(defn- default-file []
  (str (-> (u/cwd) u/basename) ".log"))

(defn set-opts!
  "Overwrite one or more settings in the logger state.

  Validate each setting with spec before updating the value in the state.
  Accept key-value pairs, allowed keys:

  - :buffer-size, log channel's buffer size, drop logs when full; default 1024.
  - :log-level, one among :trace, :debug, :info, :warn, :error and :none;
                default :trace.
  - :out-file, filename of the log file, could be :stdout to print on screen,
               set to :log-file to use a file named after the current folder;
               default :stdout.
  - :log-delay, wait this delay (ms) between two consecutive messages; default 10.
  - :tstamp-format, one among :iso, :utc, :locale, :date, :time, :locale-date,
                    :locale-time; default :iso.
  - :throw-on-err?, if true throw an error after the :error log; default false.
  - :pretty-print?, if true pretty print the value of spy; default true.
  "
  [& {:keys [buffer-size
             log-level
             out-file
             log-delay
             tstamp-format
             throw-on-err?
             pretty-print?]}]

  (when-realised [init-prom]
    (let [new-opts (cond-> {}
                     (s/valid? ::buffer-size buffer-size) (assoc ::buffer-size buffer-size)

                     (s/valid? ::log-level log-level) (assoc ::log-level
                                                             (or (and (int? log-level) log-level)
                                                                 (level->int log-level)))

                     (s/valid? ::out-file out-file) (assoc ::out-file out-file)

                     (s/valid? ::log-delay log-delay) (assoc ::log-delay log-delay)

                     (s/valid? ::tstamp-format tstamp-format) (assoc ::tstamp-format tstamp-format)

                     (s/valid? ::throw-on-err? throw-on-err?) (assoc ::throw-on-err? throw-on-err?)

                     (s/valid? ::pretty-print? pretty-print?) (assoc ::pretty-print? pretty-print?))]
      (swap! log-state merge new-opts)
      (when (::buffer-size new-opts)
        (sc/set-buffer-size! (::log-chan @log-state) buffer-size))))
  nil)

(defn enabled?
  "True if a message of class 'level' should be printed."
  ([level] (if (p/realised? init-prom)
             (enabled? level (::log-level @log-state))
             true))
  ([level log-level]
   (>= (or (level->int level) -1) log-level)))

(defn- format-log
  "Construct the log entry with timestamp, log level, tag and message."
  [[level tag msg]]
  (u/format "%s - %s [%s] - %s\n"
            (u/tstamp (::tstamp-format @log-state))
            (-> level
                name
                cs/upper-case)
            tag
            msg))

(s/def ::bundle (s/cat :level ::log-level-tag :tag string? :msg string?))

(defn- print-log
  "Print the message to a file or to standard output."
  [bundle]
  (let [{::keys [out-file default-file]} @log-state]
    (case out-file
      :stdout (println (format-log bundle))
      :log-file (u/append-file default-file (format-log bundle))
      (u/append-file out-file (format-log bundle)))))

(defn- tag->str [tag]
  (cond
    (and (keyword? tag) (= (name tag) "-")) (namespace tag)
    (string? tag) tag
    :else (str tag)))

(defn- ppr-str [value]
  (str "\n"
       (cs/trim
        (with-out-str
          (pp/pprint value)))))

(defn spy*
  "If `level` is enabled log the `value`, then return it.

  Pretty print the value if `pretty-print?` is set to `true`.
  "
  [level tag value]
  (when-realised [init-prom]
    (let [{::keys [log-level log-chan pretty-print?]} @log-state]
      (when (enabled? level log-level)
        (if pretty-print?
          (sc/put! log-chan [level (tag->str tag) (ppr-str value)])
          (sc/put! log-chan [level (tag->str tag) (pr-str value)])))))
  value)

(defn- log
  "Internal method that puts the new message on the log channel.

  Called by trace, debug, info and warn.
  "
  [level tag msg args]
  (when-realised [init-prom]
    (let [{::keys [log-level log-chan]} @log-state]
      (when (enabled? level log-level)
        (sc/put! log-chan [level (tag->str tag)
                           (u/format* (str msg) args)]))))
  nil)

(defn trace*
  "Create a trace log with a `tag` and a `msg` formatted with all optional args.

  Discard the log if `:trace` level is not enabled.
  "
  [tag msg & args]
  (log :trace tag msg args))

(defn debug*
  "Create a debug log with a `tag` and a `msg` formatted with all optional args.

  Discard the log if `:debug` level is not enabled.
  "
  [tag msg & args]
  (log :debug tag msg args))

(defn info*
  "Create an info log with a `tag` and a `msg` formatted with all optional args.

  Discard the log if `:info` level is not enabled.
  "
  [tag msg & args]
  (log :info tag msg args))

(defn warn*
  "Create a warning log with a `tag` and a `msg` formatted with all optional args.

  Discard the log if `:warn` level is not enabled.
  "
  [tag msg & args]
  (log :warn tag msg args))

(defn error*
  "Create an error log with a `tag` and a `msg` formatted with all optional args.

  Discard the log if `:error` level is not enabled.
  If `throw-on-err?` is `true`, throw an error after the message is printed.
  "
  [tag msg & args]
  (when-realised [init-prom]
    (let [{::keys [log-chan log-level throw-on-err?]} @log-state
          f-msg (u/format* (str msg) args)
          s-tag (tag->str tag)]
      (when (enabled? :error log-level)
        (if throw-on-err?
          (when-realised [(sc/put! log-chan [:error s-tag f-msg])]
            (defer 1000
              (throw (js/Error. (u/format "%s: %s" s-tag f-msg)))))
          (sc/put! log-chan [:error s-tag f-msg])))))
  nil)

(defn- log-loop
  "Wait on a loop for new messages from the channel and print what is received.

  When bundle is nil stop the loop (i.e. close the logger).
  The loop is a recursion with a configurable delay, :loop-delay from the settings,
  useful to lower the rate of printed messages.
  "
  [chan]
  (sc/try-realise init-prom true)
  (let-realised [bundle (sc/take! chan)]
    (if @bundle
      (do
        (print-log @bundle)
        (defer (::log-delay @log-state)
          (log-loop chan)))
      (let [log-level (::log-level @log-state)]
        (when (and log-level (enabled? :trace log-level))
          (print-log [:trace (namespace ::-) "Closing logger..."]))))))

(defn- init-logger
  "Initialize the logger, load the config file if valid or use the default config.

  This method is invoked directly on load, hence initialization is implicit once this namespace is required."
  []
  (let [tag (gensym :init)
        lock (swap! init-lock #(or % tag))]
    (when (= lock tag)
      (let [conf-filename "shrimp-log"
            buffer-size 1024
            default-config {::log-level 1
                            ::default-file (default-file)
                            ::out-file :stdout
                            ::log-delay 10
                            ::tstamp-format :iso
                            ::buffer-size buffer-size
                            ::throw-on-err? false
                            ::pretty-print? true
                            ::verbose? false}
            conf-file (u/read-config conf-filename)]
        (if conf-file
          (when-realised [conf-file]
            (let [{:keys [log-level
                          out-file
                          log-delay
                          tstamp-format
                          buffer-size
                          throw-on-err?
                          pretty-print?
                          verbose?]} @conf-file
                  new-config (cond-> default-config
                               log-level (assoc ::log-level (or (and (int? log-level) log-level)
                                                                (level->int log-level)))
                               out-file (assoc ::out-file out-file)
                               log-delay (assoc ::log-delay log-delay)
                               tstamp-format (assoc ::tstamp-format tstamp-format)
                               buffer-size (assoc ::buffer-size buffer-size)
                               throw-on-err? (assoc ::throw-on-err? throw-on-err?)
                               pretty-print? (assoc ::pretty-print? pretty-print?)
                               verbose? (assoc ::verbose? verbose?))]
              (if (s/valid? ::log-opts new-config)
                (reset! log-state (assoc new-config ::log-chan
                                         (sc/chan (::buffer-size new-config) nil 0)))
                (do
                  (s/explain ::log-opts new-config)
                  (reset! log-state (assoc default-config ::log-chan
                                           (sc/chan (::buffer-size default-config) nil 0)))))
              (when (::verbose? @log-state)
                (trace* (namespace ::-) "Initializing logger..."))
              (defer 1 (log-loop (::log-chan @log-state)))))

          (let [log-chan (sc/chan buffer-size nil 0)]
            (reset! log-state (assoc default-config ::log-chan log-chan))
            (when (::verbose? default-config)
              (trace* (namespace ::-) "Initializing logger..."))
            (defer 1 (log-loop log-chan))))))))

;; Initialize the logger while loading the namespace.
(init-logger)

(defn close!
  "Close the log channel causing to exit from the log loop once the channel is empty.

  This method is *not required* to exit cleanly.
  After calling it the logger will still be alive until all queued messages are
  flushed, but new logs will be discarded.
  "
  []
  (when-realised [init-prom]
    (-> log-state deref ::log-chan sc/close!)))
