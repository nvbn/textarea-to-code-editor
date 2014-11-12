(ns textarea-to-code-editor.chrome.core
  (:require [clj-di.core :refer-macros [defprotocol*]]))

(defn available?
  "Returns true when real chrome abailable."
  []
  (aget js/window "chrome"))

(defprotocol* chrome
  "Simple protocol which adds ability to mock chrome api in tests."
  (on-message [_ listener])
  (send-message [_ params])
  (clear-context-menu [_])
  (create-context-menu [_ params]))

(deftype real-chrome []
  chrome
  (on-message [_ listener] (.. js/chrome -extension -onMessage
                               (addListener listener)))
  (send-message [_ params] (.. js/chrome -extension
                               (sendMessage (clj->js params))))
  (clear-context-menu [_] (.. js/chrome -contextMenus (removeAll)))
  (create-context-menu [_ params] (.. js/chrome -contextMenus
                                      (create (clj->js params)))))
