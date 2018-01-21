(ns queues.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [queues.core-test]))

(doo-tests 'queues.core-test)
