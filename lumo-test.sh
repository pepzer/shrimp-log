#!/bin/bash
lumo -D org.clojars.pepzer/redlobster:0.2.2,shrimp:0.1.1-SNAPSHOT -c src/cljs:src/cljc:test/cljs -m shrimp-log.async-tests

