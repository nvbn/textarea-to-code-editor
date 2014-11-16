(ns textarea-to-code-editor.chrome.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [>! chan]]
            [clj-di.core :refer-macros [defprotocol*]]))

(defn available?
  "Returns true when real chrome abailable."
  []
  (aget js/window "chrome"))

(defprotocol* chrome
  "Simple protocol which adds ability to mock chrome api in tests."
  (on-message [_ listener])
  (send-message [_ request data])
  (send-message-to-tab [_ tab request data])
  (clear-context-menu [_])
  (create-context-menu [_ params]))

(deftype real-chrome []
  chrome
  (on-message [_ listener] (.. js/chrome -runtime -onMessage
                               (addListener listener)))
  (send-message [_ request data] (.. js/chrome -runtime
                                     (sendMessage (clj->js {:request request
                                                            :data data}))))
  (send-message-to-tab [_ tab request data] (.. js/chrome -tabs
                                                (sendMessage (.-id tab)
                                                             (clj->js {:request request
                                                                       :data data}))))
  (clear-context-menu [_] (.. js/chrome -contextMenus (removeAll)))
  (create-context-menu [_ params] (.. js/chrome -contextMenus
                                      (create (clj->js params)))))

(defn get-messages-chan
  "Returns chan with messages from backend."
  []
  (let [ch (chan)]
    (on-message* #(go (let [msg (js->clj %1 :keywordize-keys true)]
                        (>! ch [(keyword (:request msg))
                                (:data msg)
                                %2]))))
    ch))
