(ns textarea-to-code-editor.background.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [cljs.core.match.macros :refer [match]])
  (:require [cljs.core.match]
            [cljs.core.async :refer [<! chan]]
            [alandipert.storage-atom :refer [local-storage]]
            [textarea-to-code-editor.background.chrome :as c]
            [textarea-to-code-editor.background.modes :as m]
            [textarea-to-code-editor.background.handlers :as h]))

(defn get-storage
  "Returns configured local storage atom."
  []
  (local-storage (atom {:used-modes m/default-used-modes})
                 :textarea-to-code-editor-2))

(defn handle-messages!
  "Handles messages received from content."
  [msg-chan storage]
  (go-loop []
    (match (<! msg-chan)
      [:populate-context-menu data sender-chan] (h/populate-context-menu! data
                                                                          (:used-modes @storage)
                                                                          sender-chan
                                                                          msg-chan)
      [:clear-context-menu _ _] (h/clear-context-menu!)
      [:update-used-modes mode _] (h/update-used-modes! storage mode))
    (recur)))

(when (c/available?)
  (let [msg-chan (chan)]
    (c/subscribe-to-runtime! msg-chan)
    (handle-messages! msg-chan (get-storage))))
