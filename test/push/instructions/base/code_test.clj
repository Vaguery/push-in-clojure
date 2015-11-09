(ns push.instructions.base.code_test
  (:use midje.sweet)
  (:use [push.util.test-helpers])
  (:require [push.interpreter.core :as i])
  (:use [push.types.base.code])            ;; sets up classic-code-type
  )


;; these are tests of an Interpreter with the classic-code-type registered
;; the instructions under test are those stored IN THAT TYPE

;; work in progress
;; these instructions from Clojush are yet to be implemented:

; assemblers and disassemblers

; code_fromboolean
; code_fromfloat
; code_frominteger
; code_quote
; code_wrap
; code_fromzipnode
; code_fromziproot
; code_fromzipchildren
; code_fromziplefts
; code_fromziprights


; getters and setters


; code_length
; code_nth
; code_nthcdr
; code_size
; code_extract
; code_insert
; code_container


; code methods qua methods


; code_discrepancy
; code_overlap
; code_append
; code_atom
; code_car
; code_cdr
; code_cons
; code_do
; code_do*
; code_do*range
; code_do*count
; code_do*times
; code_map
; code_if
; code_list
; code_member
; code_null
; code_subst
; code_contains
; code_position


(tabular
  (fact ":code-noop returns the number of items on the :code stack (to :integer)"
    (register-type-and-check-instruction
        ?set-stack ?items classic-code-type ?instruction ?get-stack) => ?expected)

    ?set-stack  ?items            ?instruction      ?get-stack     ?expected
    ;; how many?
    :code    '(1.1 '(8 9))         :code-noop        :code        '(1.1 '(8 9))
    :code    '()                   :code-noop        :code        '())
     


(tabular
  (fact ":code-quote moves the top :exec item to :code"
    (check-instruction-with-all-kinds-of-stack-stuff
        ?new-stacks classic-code-type ?instruction) => (contains ?expected))

    ?new-stacks                ?instruction       ?expected

    {:exec '(1 2 3)
     :code '(false)}           :code-quote            {:exec '(2 3)
                                                       :code '(1 false)} 
    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
    {:exec '((1 2) 3)
     :code '(true)}            :code-quote            {:exec '(3)
                                                       :code '((1 2) true)} 
    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
    ;; missing arguments
    {:exec '()
     :code '(true)}            :code-quote            {:exec '()
                                                       :code '(true)})


;; visible


(tabular
  (fact ":code-stackdepth returns the number of items on the :code stack (to :integer)"
    (register-type-and-check-instruction
        ?set-stack ?items classic-code-type ?instruction ?get-stack) => ?expected)

    ?set-stack  ?items            ?instruction      ?get-stack     ?expected
    ;; how many?
    :code    '(1.1 2.2 3.3)      :code-stackdepth   :integer      '(3)
    :code    '(1.0)              :code-stackdepth   :integer      '(1)
    :code    '()                 :code-stackdepth   :integer      '(0))
   

(tabular
  (fact ":code-empty? returns the true (to :boolean stack) if the stack is empty"
    (register-type-and-check-instruction
        ?set-stack ?items classic-code-type ?instruction ?get-stack) => ?expected)

    ?set-stack  ?items          ?instruction  ?get-stack     ?expected
    ;; none?
    :code    '(0.2 1.3e7)        :code-empty?   :boolean     '(false)
    :code    '()                 :code-empty?   :boolean     '(true))


; ;; equatable


(tabular
  (fact ":code-equal? returns a :boolean indicating whether :first = :second"
    (register-type-and-check-instruction
        ?set-stack ?items classic-code-type ?instruction ?get-stack) => ?expected)

    ?set-stack  ?items         ?instruction      ?get-stack     ?expected
    ;; same?
    :code    '((1 2) (3 4))     :code-equal?      :boolean        '(false)
    :code    '((3 4) (1 2))     :code-equal?      :boolean        '(false)
    :code    '((1 2) (1 2))     :code-equal?      :boolean        '(true)
    ;; missing args     
    :code    '((3 4))           :code-equal?      :boolean        '()
    :code    '((3 4))           :code-equal?      :code           '((3 4))
    :code    '()                :code-equal?      :boolean        '()
    :code    '()                :code-equal?      :code           '())


(tabular
  (fact ":code-notequal? returns a :boolean indicating whether :first ≠ :second"
    (register-type-and-check-instruction
        ?set-stack ?items classic-code-type ?instruction ?get-stack) => ?expected)

    ?set-stack  ?items           ?instruction      ?get-stack     ?expected
    ;; different
    :code    '((1) (88))       :code-notequal?      :boolean      '(true)
    :code    '((88) (1))       :code-notequal?      :boolean      '(true)
    :code    '((1) (1))        :code-notequal?      :boolean      '(false)
    ;; missing args    
    :code    '((88))           :code-notequal?      :boolean      '()
    :code    '((88))           :code-notequal?      :code         '((88))
    :code    '()               :code-notequal?      :boolean      '()
    :code    '()               :code-notequal?      :code         '())

; ;; movable