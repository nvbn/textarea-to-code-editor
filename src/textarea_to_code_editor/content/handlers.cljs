(ns textarea-to-code-editor.content.handlers
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cljs.core.match.macros :refer [match]])
  (:require [cljs.core.match]
            [cljs.core.async :refer [>!]]
            [textarea-to-code-editor.content.editor :as e]))

(defn change-mode!
  "Changes editor mode."
  [el mode hover-chan]
  (when el
    (match [(e/is-editor? el) mode]
           [true :textarea] (e/to-textarea! el)
           [false _] (e/to-code-editor! el mode hover-chan)
           [true _] (e/change-editor-mode! el mode))))

(defn populate-context-menu!
  "Populates context menu with available modes."
  [el runtime-chan]
  (go (>! runtime-chan [:populate-context-menu
                        {:current-mode (e/get-editor-mode el)
                         :modes (e/get-modes)}]))
  el)

(defn clear-context-menu!
  "Clears context menu."
  [runtime-chan]
  (go (>! runtime-chan [:clear-context-menu]))
  nil)
