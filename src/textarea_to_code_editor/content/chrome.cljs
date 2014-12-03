(ns textarea-to-code-editor.content.chrome
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [cljs.core.async :refer [<! >! chan]]
            [cognitect.transit :as t]))

(defn available? [] (aget js/window "chrome"))

(defn subscribe-to-runtime!
  "Puts all runtime messages to msg-chan."
  [msg-chan]
  (.. js/chrome -runtime -onMessage
      (addListener #(go (>! msg-chan
                            (conj (t/read (t/reader :json) %1)
                                  %2))))))

(defn get-runtime-chan
  "Returns channel for send message to runtime."
  []
  (let [ch (chan)]
    (go-loop []
      (.. js/chrome -runtime
          (sendMessage (t/write (t/writer :json) (<! ch))))
      (recur))
    ch))
