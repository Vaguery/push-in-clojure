(ns acceptance.stress-tests
  (:use midje.sweet)
  (:require [push.instructions.dsl :as dsl])
  (:require [push.instructions.core :as instr])
  (:require [push.types.core :as types])
  (:require [push.util.stack-manipulation :as u])
  (:require [clojure.string :as s])
  (:use [push.interpreter.core])
  (:use [push.interpreter.templates.one-with-everything])
  )


(defn all-instructions
  [interpreter]
  (keys (:instructions interpreter)))


;; some random code generators


(defn random-integer
  ([] (rand-int 1000000000))
  ([range] (rand-int range)))


(defn random-integers
  ([] (random-integers 1000000000))
  ([range] (into [] (repeatedly (random-integer 10) #(random-integer range)))))



(defn random-boolean
  []
  (> 0.5 (rand)))


(defn random-booleans
  [] (into [] (repeatedly (random-integer 10) #(random-boolean))))



(defn random-float
  ([] (random-float 100000))
  ([r] (/ (double (random-integer r)) 256)))


(defn random-floats
  ([] (random-floats 100000))
  ([range] (into [] (repeatedly (random-integer 10) #(random-float range)))))



(defn random-char
  [] (char (+ 8 (random-integer 5000))))


(defn random-chars
  [] (into [] (repeatedly (random-integer 10) #(random-char))))


(defn random-string
  [] (str (s/join (repeatedly (inc (random-integer 20)) #(random-char)))))


(defn random-strings
  [] (into [] (repeatedly (random-integer 10) #(random-string))))


(defn any-input
  [interpreter]
  (rand-nth (keys (:inputs interpreter))))


(defn any-instruction
  [interpreter]
  (rand-nth (all-instructions interpreter)))


(defn bunch-a-junk
  [interpreter how-much-junk]
  (remove nil? (repeatedly how-much-junk #(condp = (rand-int 20)
                                     0 (random-integer)
                                     1 (random-float)
                                     2 (random-boolean)
                                     3 (any-input interpreter)
                                     4 (random-char)
                                     5 (random-string)
                                     6 (into '() (bunch-a-junk interpreter 5))
                                     7 (random-integers 5000)
                                     8 (random-booleans)
                                     9 (random-floats 40)
                                     10  (random-chars)
                                     11 (random-strings)

                                     (any-instruction interpreter)))))


(defn timeout [timeout-ms callback]
 (let [fut (future (callback))
       ret (deref fut timeout-ms ::timed-out)]
   (when (= ret ::timed-out)
     (do (future-cancel fut) (throw (Exception. "timed out"))))
   ret))

;; (timeout 100 #(do (Thread/sleep 1000) (println "I finished")))


(defn random-program-interpreter
  [i len]
  (let [some-junk (into [] (remove nil? (bunch-a-junk (make-everything-interpreter) i)))
        interpreter (make-everything-interpreter 
                      :config {:step-limit 50000}
                      :inputs some-junk)]
    (assoc interpreter :program (into [] (bunch-a-junk interpreter len)))))



(defn run-with-wordy-try-block
  [interpreter]
  (try
    (do
      (println (str (:counter (run interpreter 10000)))))
    (catch Exception e 
      (do 
        (println
          (str "caught exception: " 
             (.getMessage e)
             " running "
             (pr-str (:program interpreter)) "\n" (pr-str (:inputs interpreter))))
          (throw (Exception. (.getMessage e)))))))


;; actual tests; they will run hot!

(future-fact "I can create 10000 random programs without an exception"
  :slow :acceptance 
  (do (println "creating and discarding 10000 random programs")
      (count (repeatedly 10000 #(random-program-interpreter 10 100)))) => 10000)


;; the following monstrosity is an "acceptance test" for hand-running, at the moment.
;; it's intended to give a bit more info about the inevitable bugs that
;; only appear when random programs are executed by an interpreter, in a
;; bit more of a complex context; by the time you read this, it might be
;; commented out. If you want to run it, be warned it will spew all kinds
;; of literally random text to the STDOUT stream.
(fact "I can create and step through 10000 random programs without an exception"
  :slow :acceptance
  (do (println "creating and running 10000 random programs")
      (dotimes [n 10000] 
        (let [rando (assoc-in (reset-interpreter (random-program-interpreter 10 200))
                      [:config :step-limit] 5000)] 
          (try
            (timeout 10000 #(do
              (println (str "\n\n" n " : " (pr-str (:program rando)) "\n" (pr-str (:inputs rando))))
              (loop [s rando]
                (if (is-done? s)
                  (println (str n "  " (:counter s)))
                  (recur (do 
                    ; (println (u/peek-at-stack s :log)) 
                    (step s)))))))
              (catch Exception e (do 
                                    (println 
                                      (str "caught exception: " 
                                         (.getMessage e)
                                         " running "
                                         (pr-str (:program rando)) "\n" (pr-str (:inputs rando))))
                                      (throw (Exception. (.getMessage e))))))))) =not=> (throws))




;; the following monstrosity is an "acceptance test" for hand-running, at the moment.
;; it's intended to give a bit more info about the inevitable bugs that
;; only appear when random programs are executed by an interpreter, in a
;; bit more of a complex context; by the time you read this, it might be
;; commented out. If you want to run it, be warned it will spew all kinds
;; of literally random text to the STDOUT stream.
(future-fact "I can create & run 10000 large random programs for up to 5000 steps each without an exception"
  :slow :acceptance
  (do (println "creating and running 10000 interpreters in parallel")
    (let [my-interpreters 
      (repeatedly 10000 #(reset-interpreter (random-program-interpreter 10 1000))) ]
        (doall (pmap run-with-wordy-try-block my-interpreters))
      )) =not=> (throws))
