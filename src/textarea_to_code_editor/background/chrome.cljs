(ns textarea-to-code-editor.background.chrome
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [cljs.core.async :refer [<! >! chan]]
            [cognitect.transit :as t]))

(defn available? [] (aget js/window "chrome"))

(defn get-sender-chan
  "Returns channel for sending response to tab."
  [sender]
  (let [tab-id (.. sender -tab -id)
        ch (chan)]
    (go-loop []
      (.. js/chrome -tabs (sendMessage tab-id (t/write (t/writer :json)
                                                       (<! ch))))
      (recur))
    ch))

(defn subscribe-to-runtime!
  "Puts all runtime message to msg-chan."
  [msg-chan]
  (.. js/chrome -runtime -onMessage
      (addListener #(go (>! msg-chan [(t/read (t/reader :json) %1)
                                      (get-sender-chan %2)])))))

(defn clear-context-menu!
  "Removes all create context menus."
  []
  (.. js/chrome -contextMenus (removeAll)))

(defn create-context-menu!
  "Creates new context menu."
  [menu]
  (.. js/chrome -contextMenus (create (clj->js menu))))
