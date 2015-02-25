(ns textarea-to-code-editor.macros
  (:require [clojure.tools.macro :refer [name-with-attributes]]))

(defmacro defhandler
  "Defines messages handler which not fails on errors."
  [name & body]
  (let [[name [args & body]] (name-with-attributes name body)]
  `(defn ~name
     ~args
     (try (do ~@body)
          (catch :default e#
            (println e#))))))
