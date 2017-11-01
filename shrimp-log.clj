;; Configuration file for shrimp-log, absent keys will be set to default values.
;; Allowed keys:
;; :buffer-size, log channel's buffer size, drop logs when full; default 1024.
;; :log-level, one among :trace, :debug, :info, :warn, :error and :none;
;;             default :trace.
;; :out-file, filename of the log file, could be :stdout to print on screen,
;;            set to :log-file to use a file named after the current folder;
;;            default :stdout.
;; :log-delay, wait this delay (ms) between any two messages; default 10.
;; :tstamp-format, one among :iso, :utc, :locale, :date, :time, :locale-date,
;;                 :locale-time; default :iso.
;; :throw-on-err?, if true throw an error after the :error log; default false.
;; :pretty-print?, if true pretty print the value of spy; default true.
;; :verbose?, if true print logs on events (e.g. during init); default false.

{:buffer-size 1024
 :log-level :trace
 :out-file :stdout
 :log-delay 10
 :tstamp-format :iso
 :throw-on-err? false
 :pretty-print? true
 :verbose? false}
