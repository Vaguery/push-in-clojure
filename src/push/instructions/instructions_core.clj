(ns push.instructions.instructions-core
  (:require [push.interpreter.interpreter-core :as i])
  (:use [push.instructions.dsl]))


(defrecord Instruction [token tags needs makes transaction])


(defn make-instruction
  "creates a new Instruction record instance"
  [token & {
    :keys [tags needs makes transaction] 
    :or { tags #{}
          needs {}
          makes {}
          transaction identity }}]
  (->Instruction token tags needs makes transaction))


(defmacro build-instruction
  "Takes a token and zero or more transaction steps, and
  creates the named instruction from those steps."
  [new-name & steps]
  (if (= :tags (first steps))
    (let [tags (second steps)
          steps (drop 2 steps)]
    `(make-instruction (keyword ~(name new-name))
      :tags ~tags
      :needs ~(total-needs steps)
      :transaction (def-function-from-dsl ~@steps)))
    `(make-instruction (keyword ~(name new-name))
      :needs ~(total-needs steps)
      :transaction (def-function-from-dsl ~@steps))))

