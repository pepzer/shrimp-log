(ns shrimp-log.macros
  #?(:cljs (:require [shrimp-log.core])))

(def ^:private enabled? 'shrimp-log.core/enabled?)
(def ^:private spy* 'shrimp-log.core/spy*)
(def ^:private trace* 'shrimp-log.core/trace*)
(def ^:private debug* 'shrimp-log.core/debug*)
(def ^:private info* 'shrimp-log.core/info*)
(def ^:private warn* 'shrimp-log.core/warn*)
(def ^:private error* 'shrimp-log.core/error*)

(defmacro spy
  "Evaluate `expr`, log and return the result.

  Avoid logging if `level` is not currently enabled.
  "
  [level expr]
  `(let [val# ~expr]
     (when (~enabled? ~level)
       (~spy* ~level (str *ns*) val#))
     val#))

(defmacro trace
  "If `:trace` is enabled call `trace*` to log the message.

  Avoid evaluations when the log is refused.
  "
  [msg & args]
  `(when (~enabled? :trace)
     (~trace* (str *ns*) ~msg ~@args)))

(defmacro debug
  "If `:debug` is enabled call `debug*` to log the message.

  Avoid evaluations when the log is refused.
  "
  [msg & args]
  `(when (~enabled? :debug)
     (~debug* (str *ns*) ~msg ~@args)))

(defmacro info
  "If `:info` is enabled call `info*` to log the message.

  Avoid evaluations when the log is refused.
  "
  [msg & args]
  `(when (~enabled? :info)
     (~info* (str *ns*) ~msg ~@args)))

(defmacro warn
  "If `:warn` is enabled call `warn*` to log the message.

  Avoid evaluations when the log is refused.
  "
  [msg & args]
  `(when (~enabled? :warn)
     (~warn* (str *ns*) ~msg ~@args)))

(defmacro error
  "If `:error` is enabled call `error*` to log the message.

  Avoid evaluations when the log is refused.
  "
  [msg & args]
  `(when (~enabled? :error)
     (~error* (str *ns*) ~msg ~@args)))
