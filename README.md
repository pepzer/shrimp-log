
Shrimp-Log is small [ClojureScript](https://clojurescript.org/) logging library.  
It requires [Node.js](https://nodejs.org/en/) and is built on top of [Shrimp](https://github.com/pepzer/shrimp) and [Red Lobster](https://github.com/whamtet/redlobster).  
Anyone looking for a mature logging library full of features should look elsewhere (e.g. [Timbre](https://github.com/ptaoussanis/timbre)).  
The idea behind Shrimp-Log is to have a lightweight library with enough features for small projects on [Lumo](https://github.com/anmonteiro/lumo).  

## Leiningen/Clojars/Lumo

[![Clojars Project](https://img.shields.io/clojars/v/shrimp-log.svg)](https://clojars.org/shrimp-log)

If you use [Leiningen](https://github.com/technomancy/leiningen) add shrimp-log to the dependencies in your `project.clj` file.

```clojure
:dependencies [... 
               [shrimp-log "0.1.0-SNAPSHOT"]]
```
    
For Lumo you could download the dependencies with Leiningen/Maven and specify the libraries on the CLI this way:

    $ lumo -D org.clojars.pepzer/redlobster:0.2.2,shrimp:0.1.1-SNAPSHOT,shrimp-log:0.1.0-SNAPSHOT

## REPL

To run a REPL you could either use `lein figwheel` (optionally with rlwrap):
   
    $ rlwrap lein figwheel

With Node.js and npm installed open a shell, navigate to the root of the project and run:

    $ npm install ws
    $ node target/out/shrimp-log.js

Then the REPL should connect in the `lein figwheel` window.

With Lumo installed just run the `lumo-repl.cljsh` script:
   
    $ bash lumo-repl.cljsh
    
This will run the REPL and will also listen on the port `12345` and host `localhost` for connections.  
You could connect with Emacs and Clojure Minor Mode.

## Usage

To use the library require core and desired macros:
 
```clojure
(require '[shrimp-log.core :refer [trace* debug* info* warn* error* spy*]])
(use-macros '[shrimp-log.macros :only [trace debug info warn error spy]])

(trace "test trace %d" 1)

;; => 2017-10-27T20:35:24.135Z - TRACE [cljs.user] - test trace 1

(trace* "readme" "test trace %d" 2)

;; => 2017-10-27T20:35:27.221Z - TRACE [readme] - test trace 2
```

All logging methods in shrimp-log have two variants, a macro and a function (that ends with `*`). A call to `trace`, `debug`, `info`, `warn` and `error` only requires one argument, a message, that supports the format syntax and could be followed by any number of additional arguments.  
The functions `trace*`, `debug*`, `info*`, `warn*`and `error*` require a tag as first argument, it could be anything, it's converted to string and the only "special" value is the qualified keyword `::-` that gets converted to just the namespace.
    
```clojure
(let [r (range 10)]
  (info* ::- "count is %d" (count r)))
  
;; => 2017-10-27T20:47:44.935Z - INFO [cljs.user] - count is 10
```

`error`/`error*` by default behave like the others, but the setting `throw-on-err?` could be enabled to cause a throw after the log.  
`spy` wraps an expression and logs the result besides returning it. The first argument to `spy` must be the log level, for the functions `spy*` a tag is required as second argument before the expression. Pretty printing for the logged value is enabled by default and could be controlled through the `pretty-print?` setting.

```clojure
(spy :debug (range 10))

;; => 2017-10-27T21:01:27.691Z - DEBUG [cljs.user] - 
;; => (0 1 2 3 4 5 6 7 8 9)

(spy* :info ::spy (reduce #(assoc %1 %2 %1) {} (range 4)))

;; => ...
;; => 2017-10-27T21:00:13.365Z - INFO [:cljs.user/spy] - 
;; => {0 {},
;; =>  1 {0 {}}, 
;; =>  2 {0 {}, 1 {0 {}}}, 
;; =>  3 {0 {}, 1 {0 {}}, 2 {0 {}, 1 {0 {}}}}}

```

## Settings

Settings could be modified either by calling the method `set-opts!` with key-value pairs:

```clojure
(require '[shrimp-log.core :as l])

(l/set-opts! :buffer-size 100
             :out-file :log-file
             :pretty-print false)
```

Or by creating a file in the current working directory called `shrimp-log.clj` and containing a Clojure map:

```clojure
;; shrimp-log.clj

{:buffer-size 100
 :pretty-print? false
 :throw-on-err? true
 :log-level :warn}
```

Current available settings are the following:

### :log-level

This defines what logs are allowed, by default all levels are printed so this value is set to `:trace`. Possible values are `:trace`, `:debug`, `:info`, `warn`, `:error` and `:none`.

### :out-file

Currently the logger could only print to screen or to a file. This setting specifies the filename of the log file, there are two special keywords that could be used instead of a filename:

- `:stdout` which is the default and means that messages are printed on the console,
- `:log-file` that will cause the creation of a log file with the same name of the current folder and extension `.log`.

### :pretty-print?

A boolean flag to enable/disable pretty printing for `spy`/`spy*`, defaults to `true`.

### :throw-on-err?

A boolean flag, when `true` an error is thrown when calling `error`/`error*` after the log has been printed, but only if the log level is not equal to `:none`.   
The throw is asynchronous and couldn't be handled with a try/catch. This setting should be enabled only if the intended behaviour for an error message is to force the application to exit. Defaults to `false`.

### :tstamp-format

This setting allows to change the format of the timestamp for logs, supported values are:

* `:iso` - ISO date string format, the default,
* `:utc` - UTC date string format,
* `:locale` - locale date string format,
* `:date` - only the date,
* `:time` - only the time,
* `:locale-date` - only the date (locale format).
* `:locale-time` - only the time (locale format).

### :buffer-size

This controls the dimension of the buffer for the log channel where all logs are sent before de-queue and print, any non-negative integer is allowed and it defaults to `1024`.   
If the buffer is full subsequent log messages will be silently dropped, hence this setting should be modified accordingly to the rate log messages are produced.

### :log-delay

This value in milliseconds defines the amount of time the logger will pause between two subsequent prints. `:log-delay` limits the rate of logging, an high value could cause the buffer to be filled hence `:buffer-size` should be modified accordingly. The default value for `:log-delay` is 10 milliseconds.

## Tests

To run the tests with Leiningen use:

```
$ lein cljsbuild test
```

With Lumo:

```
$ bash lumo-test.sh
```

## Code Maturity

This is an early release, bugs should be expected and future releases could break the current API.

## Contacts

[Giuseppe Zerbo](https://github.com/pepzer), [giuseppe (dot) zerbo (at) gmail (dot) com](mailto:giuseppe.zerbo@gmail.com).

## License

Copyright © 2017 Giuseppe Zerbo.  
Distributed under the [Mozilla Public License, v. 2.0](http://mozilla.org/MPL/2.0/).
