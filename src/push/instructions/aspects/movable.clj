(ns push.instructions.aspects.movable
  (:require [push.util.code-wrangling
              :as util
              :refer [list! count-collection-points]]
            [push.util.numerics
              :as num
              :refer [scalar-to-index]])
  (:use     [push.instructions.core
              :only (build-instruction)]
            [push.instructions.dsl]))



(defn againlater-instruction
  "returns a new x-againlater instruction for a PushType"
  [pushtype]
  (let [typename (:name pushtype)
        instruction-name (str (name typename) "-againlater")]
    (eval (list
      `build-instruction
      instruction-name
      (str "`:" instruction-name "` places a copy of the top `" typename "` item at the tail of the `:exec` stack. If the resulting `:exec` stack would be oversized, the item is discarded and an `:error` is pushed.")
      :tags #{:combinator}

      `(consume-top-of ~typename :as :item)
      `(consume-stack :exec :as :oldstack)
      `(save-max-collection-size :as :limit)
      `(calculate [:oldstack :item]
          #(util/list! (concat %1 (list %2))) :as :newstack)
      `(calculate [:limit :newstack]
        #(< %1 (util/count-collection-points %2)) :as :oversized)
      `(calculate [:oversized :oldstack :newstack]
        #(if %1 %2 %3) :as :finalstack)
      `(calculate [:oversized]
        #(when %1 (str ~instruction-name
                       " produced an oversized result")) :as :message)
      `(calculate [:oversized :item] #(when-not %1 %2) :as :replacement)
      `(replace-stack :exec :finalstack)
      `(push-onto ~typename :replacement)
      `(record-an-error :from :message)
      ))))



(defn cutflip-instruction
  "returns a new x-cutflip instruction for a PushType"
  [pushtype]
  (let [typename (:name pushtype)
        instruction-name (str (name typename) "-cutflip")]
    (eval (list
      `build-instruction
      instruction-name
      (str "`:" instruction-name "` consumes a `:scalar` argument and the entire `" typename "` stack. It takes the first [idx] items of the stack and reverses the order of that segment. It returns a nested code block, containing two interior code blocks holding the 'remaining' part of the stack (reversed) and the 'flipped' part of the stack.")
      :tags #{:combinator}

      `(consume-top-of :scalar :as :where)
      `(consume-stack ~typename :as :old-stack)
      `(calculate [:where :old-stack]
        #(if (empty? %2) 0 (num/scalar-to-index %1 (count %2))) :as :idx)
      `(calculate [:old-stack :idx]
        #(util/list! (take %2 %1)) :as :topchunk)
      `(calculate [:old-stack :idx]
        #(util/list! (reverse (drop %2 %1))) :as :leftovers)
      `(calculate [:leftovers :topchunk] #(list %1 %2) :as :result)
      `(push-onto :exec :result)
      ))))



(defn cutstack-instruction
  "returns a new x-cutstack instruction for a PushType"
  [pushtype]
  (let [typename (:name pushtype)
        instruction-name (str (name typename) "-cutstack")]
    (eval (list
      `build-instruction
      instruction-name
      (str "`:" instruction-name "` takes an `:scalar` argument, makes that into an index modulo the `" typename "` stack size, takes the first [idx] items and moves that to the bottom of the stack.")
      :tags #{:combinator}

      `(consume-top-of :scalar :as :where)
      `(consume-stack ~typename :as :old-stack)
      `(calculate [:where :old-stack]
        #(if (empty? %2) 0 (num/scalar-to-index %1 (count %2))) :as :idx)
      `(calculate [:old-stack :idx]
        #(util/list! (reverse (take %2 %1))) :as :topchunk)
      `(calculate [:old-stack :idx]
        #(util/list! (reverse (drop %2 %1))) :as :leftovers)
      `(calculate [:leftovers :topchunk] #(list %2 %1) :as :result)
      `(push-onto :exec :result)
      ))))



(defn dup-instruction
  "returns a new x-dup instruction for a PushType"
  [pushtype]
  (let [typename (:name pushtype)
        instruction-name (str (name typename) "-dup")]
    (eval (list
      `build-instruction
      instruction-name
      (str "`:" instruction-name "` examines the top `" typename "` item and pushes a code block containing two copies to `:exec`.")
      :tags #{:combinator}

      `(save-top-of ~typename :as :arg1)
      `(calculate [:arg1] #(list %1 %1) :as :duplicated)
      `(push-onto :exec :duplicated)
      ))))



(defn flipstack-instruction
  "returns a new x-flipstack instruction for a PushType"
  [pushtype]
  (let [typename (:name pushtype)
        instruction-name (str (name typename) "-flipstack")]
    (eval (list
      `build-instruction
      instruction-name
      (str "`:" instruction-name "` reverses the entire `" typename "` stack, pushing it as a code block onto `:exec`.")
      :tags #{:combinator}

      `(consume-stack ~typename :as :old)
      `(calculate [:old] #(util/list! (reverse %1)) :as :new)
      `(push-onto :exec :new)
      ))))



(defn flush-instruction
  "returns a new x-flush instruction for a PushType"
  [pushtype]
  (let [typename (:name pushtype)
        instruction-name (str (name typename) "-flush")]
    (eval (list
      `build-instruction
      instruction-name
      (str "`:" instruction-name "` discards all items from the `" typename "` stack.")
      :tags #{:combinator}

      `(delete-stack ~typename)
      ))))



(defn later-instruction
  "returns a new x-later instruction for a PushType"
  [pushtype]
  (let [typename (:name pushtype)
        instruction-name (str (name typename) "-later")]
    (eval (list
      `build-instruction
      instruction-name
      (str "`:" instruction-name "` pops the top `" typename "` item and places it at the tail of the `:exec` stack. If the result would be oversized, the item is discarded and an `:error` is pushed.")
      :tags #{:combinator}

      `(consume-top-of ~typename :as :item)
      `(consume-stack :exec :as :oldstack)
      `(save-max-collection-size :as :limit)
      `(calculate [:oldstack :item]
          #(util/list! (concat %1 (list %2))) :as :newstack)
      `(calculate [:limit :newstack]
        #(< %1 (util/count-collection-points %2)) :as :oversized)
      `(calculate [:oversized :oldstack :newstack]
        #(if %1 %2 %3) :as :finalstack)
      `(calculate [:oversized]
        #(when %1 (str ~instruction-name
                       " produced an oversized result")) :as :message)
      `(calculate [:oversized :item] #(when-not %1 %2) :as :replacement)
      `(replace-stack :exec :finalstack)
      `(record-an-error :from :message)
      ))))



(defn liftstack-instruction
  "returns a new x-liftstack instruction for a PushType"
  [pushtype]
  (let [typename (:name pushtype)
        instruction-name (str (name typename) "-liftstack")]
    (eval (list
      `build-instruction
      instruction-name
      (str "`:" instruction-name "` takes an `:scalar` argument, makes that into an index modulo the `" typename "` stack size, 'cuts' the stack after the first [idx] items and _copies_ everything below that point onto the top of the stack. If the result would have more points (at any level) than `max-collection-size` the result is _discarded_ (!) and an :error is pushed.")
      :tags #{:combinator}

      `(consume-top-of :scalar :as :where)
      `(consume-stack ~typename :as :old-stack)
      `(calculate [:old-stack :where]
        #(if (empty? %1) 0 (num/scalar-to-index %2 (count %1))) :as :idx)
      `(calculate [:old-stack :idx]
        #(util/list! (reverse (drop %2 %1))) :as :duplicated)
      `(calculate [:old-stack :duplicated]
        #(list (util/list! (reverse %1)) %2) :as :results)
      `(push-onto :exec :results)
      ))))



(defn pop-instruction
  "returns a new x-pop instruction for a PushType"
  [pushtype]
  (let [typename (:name pushtype)
        instruction-name (str (name typename) "-pop")]
    (eval (list
      `build-instruction
      instruction-name
      (str "`:" instruction-name "` discards the top item from the `" typename "` stack.")
      :tags #{:combinator}

      `(delete-top-of ~typename)))))



(defn rotate-instruction
  "returns a new x-rotate instruction for a PushType"
  [pushtype]
  (let [typename (:name pushtype)
        instruction-name (str (name typename) "-rotate")]
    (eval (list
      `build-instruction
      instruction-name
      (str "`:" instruction-name "` pops the top three items from the `" typename "` stack; call them `A`, `B` and `C`, respectively. It pushes them back so that top-to-bottom order is now `'(C A B ...)`")
      :tags #{:combinator}

      `(consume-top-of ~typename :as :arg1)
      `(consume-top-of ~typename :as :arg2)
      `(consume-top-of ~typename :as :arg3)
      `(push-onto ~typename :arg2)
      `(push-onto ~typename :arg1)
      `(push-onto ~typename :arg3)))))



(defn shove-instruction
  "returns a new x-shove instruction for a PushType"
  [pushtype]
  (let [typename (:name pushtype)
        instruction-name (str (name typename) "-shove")]
    (eval (list
      `build-instruction
      instruction-name
      (str "`:" instruction-name "` pops the top item from the `" typename
        "` stack and the top `:scalar`. The `:scalar` is used to select a valid index; unlike most other indexed arguments, it is thresholded. The top item on the stack is _moved_ so that it is in the indexed position in the resulting stack.")
      :tags #{:combinator}

      `(consume-top-of :scalar :as :which)
      `(consume-top-of ~typename :as :shoved-item)
      `(count-of ~typename :as :how-many)
      `(calculate [:which :how-many]
        #(if (zero? %2) 0 (max 0 (min (Math/ceil %1) %2))) :as :index)
      `(consume-stack ~typename :as :oldstack)
      `(calculate [:index :shoved-item :oldstack]
        #(util/list! (concat (take %1 %3) (list %2) (drop %1 %3)))
          :as :newstack)
      `(replace-stack ~typename :newstack)
      ))))



(defn swap-instruction
  "returns a new x-swap instruction for a PushType"
  [pushtype]
  (let [typename (:name pushtype)
        instruction-name (str (name typename) "-swap")]
    (eval (list
      `build-instruction
      instruction-name
      (str "`:" instruction-name "` swaps the positions of the top two `" typename
        "` items, pushing the result to `:exec` as a code block.")
      :tags #{:combinator}

      `(consume-top-of ~typename :as :arg1)
      `(consume-top-of ~typename :as :arg2)
      `(calculate [:arg1 :arg2] #(list %1 %2) :as :reversed)
      `(push-onto :exec :reversed)
      ))))



(defn yank-instruction
  "returns a new x-yank instruction for a PushType"
  [pushtype]
  (let [typename (:name pushtype)
        instruction-name (str (name typename) "-yank")]
    (eval (list
      `build-instruction
      instruction-name
      (str "`:" instruction-name "` pops the top `:scalar`. The `:scalar` is brought into range as an index by forcing it into the range `[0,stack_length-1]` (inclusive), and then the item _currently_ found in the indexed position in the `" typename "` stack is _moved_ so that it is on top.")
      :tags #{:combinator}

      `(consume-top-of :scalar :as :which)
      `(count-of ~typename :as :how-many)
      `(calculate [:which :how-many]
          #(if (zero? %2)
            0
            (max 0 (min (Math/ceil %1) (dec %2)))) :as :index)
      `(consume-nth-of ~typename :at :index :as :yanked-item)
      `(push-onto ~typename :yanked-item)))))



(defn yankdup-instruction
  "returns a new x-yankdup instruction for a PushType"
  [pushtype]
  (let [typename (:name pushtype)
        instruction-name (str (name typename) "-yankdup")]
    (eval (list
      `build-instruction
      instruction-name
      (str "`:" instruction-name "` pops the top `:scalar`. The `:scalar` is brought into range as an index by forcing it into the range `[0,stack_length-1]` (inclusive), and then the item _currently_ found in the indexed position in the `" typename "` stack is _copied_ so that a duplicate of it is on top.")
      :tags #{:combinator}

      `(consume-top-of :scalar :as :which)
      `(count-of ~typename :as :how-many)
      `(calculate [:which :how-many] #(min (max (Math/ceil %1) 0) (dec %2)) :as :index)
      `(save-nth-of ~typename :at :index :as :yanked-item)
      `(push-onto ~typename :yanked-item)))))
