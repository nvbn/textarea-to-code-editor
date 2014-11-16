(ns textarea-to-code-editor.background.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [<! chan alts!]]
            [clj-di.core :refer [register!]]
            [textarea-to-code-editor.chrome.core :as c]))

(defn show-textarea-context-menu
  "Shows context menu when mouse on textarea."
  [sender]
  (c/create-context-menu* {:title "Convert to code editor"
                           :contexts [:all]
                           :onclick #(c/send-message-to-tab* (.-tab sender)
                                                             :to-code-editor
                                                             nil)}))

(defn show-editor-context-menu
  "Shows context menu when mouse on code editor."
  [sender]
  (c/create-context-menu* {:title "Convert to textarea"
                           :contexts [:all]
                           :onclick #(c/send-message-to-tab* (.-tab sender)
                                                             :to-textarea
                                                             nil)}))
(defn handle-messages!
  [msg-chan]
  (go-loop []
    (let [[request _ sender] (<! msg-chan)]
      (condp = request
        :enter (show-textarea-context-menu sender)
        :editor-enter (show-editor-context-menu sender)
        :leave (c/clear-context-menu*))
      (recur))))

(when (c/available?)
  (register! :chrome (c/real-chrome.))
  (handle-messages! (c/get-messages-chan)))
