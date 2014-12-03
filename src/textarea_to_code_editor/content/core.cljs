(ns textarea-to-code-editor.content.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [cljs.core.match.macros :refer [match]])
  (:require [cljs.core.match]
            [cljs.core.async :refer [<! chan]]
            [domina.css :refer [sel]]
            [textarea-to-code-editor.content.chrome :as c]
            [textarea-to-code-editor.content.editor :as e]
            [textarea-to-code-editor.content.handlers :as h]))

(defn handle-messages!
  "Handle messages for background and events."
  [msg-chan runtime-chan]
  (go-loop [active nil]
    (recur (match (<! msg-chan)
             [:enter-editor el _] (h/populate-context-menu! el runtime-chan)
             [:leave-editor _ _] (h/clear-context-menu! runtime-chan)
             [:change-mode mode _] (h/change-mode! active mode msg-chan)
             [& msg] (println "Unmatched message:" msg)))))

(when (c/available?)
  (enable-console-print!)
  (let [msg-chan (chan)]
    (e/subscribe-to-hover! (sel "textarea") msg-chan)
    (c/subscribe-to-runtime! msg-chan)
    (handle-messages! msg-chan (c/get-runtime-chan))))
