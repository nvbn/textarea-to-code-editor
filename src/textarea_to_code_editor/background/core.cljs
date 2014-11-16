(ns textarea-to-code-editor.background.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [clj-di.core :refer [let-deps]])
  (:require [cljs.core.async :refer [<! chan alts!]]
            [clj-di.core :refer [register! get-dep]]
            [textarea-to-code-editor.chrome.core :as c]))

(def popular-modes #{"Python" "SH" "JavaScript" "Clojure" "Ruby" "CoffeeScript"})

(defn get-ordered-modes
  "Returns list with ordered modes."
  []
  (let-deps [modes :modes]
    (->> @modes
         (map (fn [[_ {:keys [caption mode]}]] [caption mode]))
         (sort-by first))))

(defn show-textarea-context-menu
  "Shows context menu when mouse on textarea."
  [sender]
  (c/create-context-menu* {:title "Convert to code editor"
                           :contexts [:all]
                           :id :textarea-to-editor})
  (doseq [[caption mode] (get-ordered-modes)
          :when (popular-modes caption)]
    (c/create-context-menu* {:title caption
                             :contexts [:all]
                             :parentId :textarea-to-editor
                             :onclick #(c/send-message-to-tab* (.-tab sender)
                                                               :to-code-editor
                                                               mode)}))
  (c/create-context-menu* {:title "More"
                           :contexts [:all]
                           :parentId :textarea-to-editor
                           :id :textarea-to-editor-more})
  (doseq [[caption mode] (get-ordered-modes)]
    (c/create-context-menu* {:title caption
                             :contexts [:all]
                             :parentId :textarea-to-editor-more
                             :onclick #(c/send-message-to-tab* (.-tab sender)
                                                               :to-code-editor
                                                               mode)})))

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
    (let [[request data sender] (<! msg-chan)]
      (condp = request
        :enter (show-textarea-context-menu sender)
        :editor-enter (show-editor-context-menu sender)
        :leave (c/clear-context-menu*)
        :update-modes (reset! (get-dep :modes) data))
      (recur))))

(when (c/available?)
  (register! :chrome (c/real-chrome.)
             :modes (atom nil))
  (handle-messages! (c/get-messages-chan)))
