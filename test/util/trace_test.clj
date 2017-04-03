(ns util.trace-test
  (:require [push.core :as push]
            [push.interpreter.core :as i]
            [acceptance.util :as generators])
  (:use     [midje.sweet]
            [util.trace])
            )



(fact "yaml-from-interpreter-stacks"
  (yaml-from-interpreter-stacks
    (push/interpreter :program [1 2 :scalar-add]
                      :bindings {:x 77})) =>
  "boolean:\nbooleans:\nchar:\nchars:\ncode:\ncomplex:\ncomplexes:\nerror:\nexec:\n  - 1\n  - 2\n  - :scalar-add\ngenerator:\ninterval:\nintervals:\nlog:\nprint:\nquoted:\nref:\nrefs:\nreturn:\nscalar:\nscalars:\nset:\nsnapshot:\nstring:\nstrings:\ntagspace:\nunknown:\nvector:\nx:\n  - 77\n"
  )

(def fixture-interpreter
  (push/interpreter
    :bindings {:x1 9 :x2 false}
    :config {:step-limit 10000}))


(def fixture-bindings
  (merge
    (:bindings fixture-interpreter)
    (generators/some-bindings 5 10 4/5 (push/interpreter))))

(def fixture-program
  (generators/some-program 100 10 2/5 fixture-interpreter))

;
; (println (str fixture-program))
; (println (str fixture-bindings))

(def loaded-interpreter
  (i/recycle-interpreter
    fixture-interpreter
    fixture-program
    :bindings fixture-bindings))

;
; (loop [time  0
;        state loaded-interpreter]
;   (when (and (< time 500) (not (i/is-done? state)))
;     (spit (str "test/util/yamls/test-" (format "%04d" time) ".txt")
;       (yaml-from-interpreter-stacks state))
;     (recur (inc time)
;            (i/step state))
;            ))
