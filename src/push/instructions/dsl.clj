(ns push.instructions.dsl
  (:require [push.interpreter.interpreter-core :as i]))

;; a "PushDSL blob" is just a vector with an interpreter and a hashmap

;; utilities

(defn- delete-nth
  "Removes an indexed item from a seq; raises an Exception if the seq
  is empty."
  [coll idx]
  {:pre  [(seq coll)
          (not (neg? idx))
          (< idx (count coll))]}
  (concat (take idx coll) (drop 1 (drop idx coll))))


(defn- throw-empty-stack-exception
  [stackname]
  (throw (Exception. (str 
                        "Push DSL runtime error: stack "
                        stackname
                        " is empty."))))


(defn- throw-invalid-index-exception
  [k]
  (throw (Exception. (str 
                        "Push DSL argument error: " 
                        k 
                        " is not an integer"))))


(defn- throw-missing-key-exception
  [k]
  (throw  (Exception. (str 
                         "Push DSL argument error: missing key: " 
                         k))))


(defn- throw-unknown-stack-exception
  [stackname]
  (throw (Exception. (str 
                        "Push DSL argument error: no "
                        stackname
                        " stackname registered."))))


(defn- throw-function-argument-exception
  [args]
  (throw (Exception. (str 
                        "Push DSL argument error: '"
                        args
                        "' can't be parsed as vector."))))


(defn- throw-unknown-DSL-exception
  [instruction]
  (throw (Exception. (str 
                        "Push DSL parse error: '"
                        instruction
                        "' is not a known instruction."))))


(defn- index-from-scratch-ref
  "Takes a keyword and a scratch hashmap. If an integer is stored
  under that key in the hashmap, it's returned. Otherwise raises an
  exception."
  [k locals]
  (let [stored (k locals)]
    (if (integer? stored)
      stored
      (throw-invalid-index-exception k))))


(defn- valid-DSL-index
  "Takes an item (presumably part of a DSL function requiring an :at
  index) and a hashmap, and returns an integer index value, or an
  integer from the hashmap if a keyword. Blows up informatively if
  neither of those is possible."
  [item scratch]
  (cond
    (integer? item) item
    (keyword? item) (index-from-scratch-ref item scratch)
    (nil? item) (throw-missing-key-exception :at)
    :else (throw-invalid-index-exception item)))


(defn- get-nth-of
  "Abstract function invoked by all the X-nth-of DSL functions.
  Takes a PushDSL blob, a stackname, an :at index (integer or keyword)
  (but no :as keyword), and returns the index and the item at that
  index, raising any argument exceptions it finds."
  [[interpreter scratch] stackname & {:keys [at]}]
  (if-let [old-stack (i/get-stack interpreter stackname)]
    (if (empty? old-stack)
      (throw-empty-stack-exception stackname)
      (let [idx (valid-DSL-index at scratch)
            which (mod idx (count old-stack))]
        [which old-stack]))
    (throw-unknown-stack-exception stackname)))




;; DSL instructions

(defn consume-stack
  "Removes an entire named stack; if an `:as local` argument is given,
  it saves the stack in that scratch variable. If no local is given,
  it just deletes the stack.

  Exceptions when:
  - the stack doesn't exist"
  [[interpreter scratch] stackname & {:keys [as]}]
  (if-let [old-stack (i/get-stack interpreter stackname)]
    (if (nil? as)
      (throw-missing-key-exception :as)
      [(i/clear-stack interpreter stackname)
        (assoc scratch as old-stack)])
    (throw-unknown-stack-exception stackname)))


(defn consume-top-of
  "Takes an PushDSL blob, a stackname (keyword) and a scratch variable
  (keyword) in which to store the top value from that stack.

    Exceptions when:
    - the stack doesn't exist
    - the stack is empty"
  [ [interpreter scratch] stackname & {:keys [as]}]
  (if-let [old-stack (i/get-stack interpreter stackname)]
    (if-let [top-item (first old-stack)]
      (if (nil? as)
        (throw-missing-key-exception :as)
        [(i/set-stack interpreter stackname (rest old-stack))
          (assoc scratch as top-item)])
      (throw-empty-stack-exception stackname))
    (throw-unknown-stack-exception stackname)))


(defn count-of
  "Takes an PushDSL blob, a stackname (keyword) and a scratch variable
  (keyword) in which to store the count.

    Exceptions when:
    - the stack doesn't exist

    Fails silently when:
    - the local is not specified (no :as argument)"
  [[interpreter scratch] stackname & {:keys [as]}]
  (if-let [scratch-var as]
    (if-let [stack (i/get-stack interpreter stackname)]
      [interpreter (assoc scratch scratch-var (count stack))]
      (throw-unknown-stack-exception stackname))
    (throw-missing-key-exception :as)))


(defn consume-nth-of
  "Takes a PushDSL blob, a stackname (keyword), an index argument (an
  integer or a keyword), and a scratch key (also a keyword), copies
  the indicated item from that stack into the scratch variable, and
  deletes it from the stack.

  Exceptions when:
  - the stack doesn't exist
  - the stack is empty
  - no :as argument is present
  - no :at argument is present
  - the :at argument is not a keyword or integer
  - the scratch value passed as a reference is not an integer"
  [[interpreter scratch] stackname & {:keys [as at]}]
  (let [[idx old-stack]
          (get-nth-of [interpreter scratch] stackname :at at)]
    (if (nil? as)
      (throw-missing-key-exception :as)
      (let [new-stack (delete-nth old-stack idx)
            saved-item (nth old-stack idx)]
        [(i/set-stack interpreter stackname new-stack)
         (assoc scratch as saved-item)]))))


(defn delete-nth-of
  "Usage: `delete-nth-of [stackname :at where]`

  Removes item at index `where` from stack `stackname`. If `where` is 
  an integer, the index deleted is `(mod where (count stackname))`; if
  it is a scratch reference, the numerical value is looked up. If the
  item stored under key `where` is not an integer, an error occurs.

  Exceptions when:
    - the stack doesn't exist
    - the stack is empty
    - no index is given
    - the index is a :keyword that doesn't point to an integer"
  [[interpreter scratch] stackname & {:keys [as at]}]
  (let [[idx old-stack]
          (get-nth-of [interpreter scratch] stackname :at at)]
    (let [new-stack (delete-nth old-stack idx)]
      [(i/set-stack interpreter stackname new-stack) scratch])))


(defn delete-stack
  "Removes an entire named stack.

  Exceptions when:
    - the stack doesn't exist"
  [[interpreter scratch] stackname]
  (if-let [old-stack (i/get-stack interpreter stackname)]
    [(i/clear-stack interpreter stackname) scratch]
    (throw-unknown-stack-exception stackname)))


(defn delete-top-of
  "Takes an PushDSL blob and a stackname (keyword); deletes the top
  item of that stack.

    Exceptions when:
    - the stack doesn't exist
    - the stack is empty"
  [[interpreter scratch] stackname]
  (if-let [old-stack (i/get-stack interpreter stackname)]
    (if-let [top-item (first old-stack)]
      [(i/set-stack interpreter stackname (rest old-stack)) scratch]
      (throw-empty-stack-exception stackname))
    (throw-unknown-stack-exception stackname)))


(defn replace-stack
  "Takes a PushDSL blob, a stackname (keyword) and a scratch key (also
  keyword), and replaces the named stack with the item stored in the
  scratch variable. If the contents are a list, the stack is replaced
  with the entire list; if nil, the stack is emptied; if a non-list
  item the final stack will contain only that item.

  Exceptions when:
  - the stack doesn't exist
  - (does not warn when the keyword isn't defined)"
  [[interpreter scratch] stackname kwd]
  (if (some? (i/get-stack interpreter stackname))
    (let [replacement (kwd scratch)
          new-stack (cond (nil? replacement) (list)
                          (list? replacement) replacement
                          :else (list replacement))]
      [(i/set-stack interpreter stackname new-stack) scratch])
    (throw-unknown-stack-exception stackname)))


(defn push-onto
  "Takes a PushDSL blob, a stackname (keyword) and a scratch key (also
  keyword), and puts item stored in the scratch variable on top of the
  named stack. If the scratch item is nil, there is no effect (and no
  exception); if it is a list, that is pushed _as a single item_ onto
  the stack (not concatenated). No type checking is used.

  Exceptions when:
  - the stack doesn't exist
  - (does not warn when the keyword isn't defined)"
  [[interpreter scratch] stackname kwd]
  (if-let [old-stack (i/get-stack interpreter stackname)]
    (let [new-item (kwd scratch)
          new-stack (if (nil? new-item)
                      old-stack 
                      (conj old-stack new-item))]
      [(i/set-stack interpreter stackname new-stack) scratch])
    (throw-unknown-stack-exception stackname)))


(defn save-stack
  "Takes a PushDSL blob, a stackname (keyword) and a scratch key (also
  keyword), and copies that stack into the scratch variable (without
  deleting it).

  Exceptions when:
  - the stack doesn't exist
  - no :as argument is present"
  [[interpreter scratch] stackname & {:keys [as]}]
  (if-let [old-stack (i/get-stack interpreter stackname)]
    (if (some? as)
      [interpreter (assoc scratch as old-stack)]
      (throw-missing-key-exception :as))
    (throw-unknown-stack-exception stackname)))


(defn save-nth-of
  "Takes a PushDSL blob, a stackname (keyword), an index argument (an
  integer or a keyword), and a scratch key (also a keyword), and
  copies the indicated item from that stack into the scratch
  variable (without deleting it).

  Exceptions when:
  - the stack doesn't exist
  - the stack is empty
  - no :as argument is present
  - no :at argument is present
  - the :at argument is not a keyword or integer
  - the scratch value passed as a reference is not an integer"
  [[interpreter scratch] stackname & {:keys [as at]}]
  (let [[idx old-stack]
          (get-nth-of [interpreter scratch] stackname :at at)]
    (if (nil? as)
      (throw-missing-key-exception :as)
      (let [saved-item (nth old-stack idx)]
        [interpreter (assoc scratch as saved-item)]))))


(defn save-top-of
  "Takes a PushDSL blob, a stackname (keyword) and a scratch key (also
  keyword), and copies the top item from that stack into the scratch
  variable (without deleting it).

  Exceptions when:
  - the stack doesn't exist
  - no :as argument is present
  - the stack is empty"
  [[interpreter scratch] stackname & {:keys [as]}]
  (if-let [old-stack (i/get-stack interpreter stackname)]
    (if-let [top-item (first old-stack)]
      (if (some? as)
        [interpreter (assoc scratch as top-item)]
        (throw-missing-key-exception :as))
      (throw-empty-stack-exception stackname))
    (throw-unknown-stack-exception stackname)))


(defn calculate
  "Takes a PushDSL blob, a vector of keywords referring to scratch
  item keys, a function over _those keys_ (using positional notation),
  and an :as keyword in which to store the result of the function.

  Exceptions when:
  - [args] is not a vector
  - no :as argument is present
  - the wrong number of arguments are provided
  - (does not check for nil arguments)"
  [[interpreter scratch] args fxn & {:keys [as]}]
  (let [locals (map scratch args)
        result (if (vector? args)
                  (apply fxn locals)
                  (throw-function-argument-exception args))]
    (if (nil? as)
      (throw-missing-key-exception :as)
      [interpreter (assoc scratch as result)])))


;;;; integration with instruction definitions

(defn needs-of-dsl-step
  [step]
  (let [cmd (first step)]
    (condp = cmd
      'calculate {}
      'consume-nth-of {(second step) 1}
      'consume-stack {(second step) 0}
      'consume-top-of {(second step) 1}
      'count-of {(second step) 0}
      'delete-nth-of {(second step) 1}
      'delete-stack {(second step) 0}
      'delete-top-of {(second step) 1}
      'replace-stack {(second step) 0}
      'push-onto {(second step) 0}
      'save-nth-of {(second step) 1}
      'save-stack {(second step) 0}
      'save-top-of {(second step) 1}
      (throw-unknown-DSL-exception cmd)  )))


(defn total-needs
  [transaction]
  (apply (partial merge-with +)
    (map needs-of-dsl-step transaction)))


(defmacro
  def-function-from-dsl
  [& transactions]
  (let [interpreter (gensym 'interpreter)
       words &form]
    (do 
    `(fn [~interpreter] 
      (first (-> [~interpreter {}] ~@transactions))))))
