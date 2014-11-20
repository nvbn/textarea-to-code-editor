(ns textarea-to-code-editor.content.core
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]
                   [cljs.core.match.macros :refer [match]])
  (:require [cljs.core.match]
            [cljs.core.async :refer [<! >! chan]]
            [domina.css :refer [sel]]
            [textarea-to-code-editor.content.chrome :as c]
            [textarea-to-code-editor.content.editor :as e]))

(defn change-mode!
  "Changes editor mode."
  [el mode hover-chan]
  (when el
    (match [(e/is-editor? el) mode]
      [true :textarea] (e/to-textarea! el)
      [false _] (e/to-code-editor! el hover-chan mode)
      [true _] (e/change-editor-mode! el mode)
      :else el)))

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

(defn handle-messages!
  "Handle messages for background and events."
  [msg-chan runtime-chan]
  (go-loop [active nil]
    (recur (match (first (<! msg-chan))
             [:enter-editor el] (populate-context-menu! el runtime-chan)
             [:leave-editor] (clear-context-menu! runtime-chan)
             [:change-mode mode] (change-mode! active mode msg-chan)))))

(when (c/available?)
  (let [msg-chan (chan)]
    (e/subscribe-to-hover! (sel "textarea") msg-chan)
    (c/subscribe-to-runtime! msg-chan)
    (handle-messages! msg-chan (c/get-runtime-chan))))
