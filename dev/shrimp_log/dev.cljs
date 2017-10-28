(ns shrimp-log.dev
  (:require [shrimp-log.core :as core]
            [figwheel.client :as fw]))

(defn -main []
  (fw/start { }))

(set! *main-cli-fn* -main)
