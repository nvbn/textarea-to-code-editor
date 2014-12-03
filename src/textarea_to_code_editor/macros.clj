(ns textarea-to-code-editor.macros)

(defmacro defhandler
  "Defines messages handler which not fails on errors."
  [name docstring args & body]
  `(defn ~name
     ~docstring
     ~args
     (try (do ~@body)
          (catch :default e#
            (js/console.error e#)))))
