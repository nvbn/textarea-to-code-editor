(ns textarea-to-code-editor.background.handlers
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [textarea-to-code-editor.macros :refer [defhandler]])
  (:require [cljs.core.async :refer [>! chan]]
            [textarea-to-code-editor.background.chrome :as c]
            [textarea-to-code-editor.background.modes :as m]))

(defn create-context-menus!
  "Creates context menus instances in chrome."
  [& menus]
  (doseq [menu (flatten menus)]
    (c/create-context-menu! (assoc menu
                              :contexts [:all]))))

(defn get-menus-for-modes
  "Returns params for context menus for passed modes"
  [modes current-mode parent-id sender-chan msg-chan]
  (for [mode modes]
    {:title (:caption mode)
     :parentId parent-id
     :type :checkbox
     :checked (= current-mode mode)
     :onclick #(go (>! sender-chan [:change-mode mode])
                   (>! msg-chan [:update-used-modes mode nil]))}))

(defhandler populate-context-menu!
  "Shows context menu when mouse on textarea."
  [{:keys [current-mode modes]} used-modes sender-chan msg-chan]
  (c/clear-context-menu!)
  (create-context-menus!
    {:title "Textarea to code editor"
     :id :textarea-to-code-editor}
    {:title "Normal textarea"
     :parentId :textarea-to-code-editor
     :type :checkbox
     :checked (nil? current-mode)
     :onclick #(go (>! sender-chan [:change-mode :textarea]))}
    {:parentId :textarea-to-code-editor
     :type :separator}
    (get-menus-for-modes (m/sanitize-used-modes modes used-modes)
                         current-mode :textarea-to-code-editor
                         sender-chan msg-chan)
    {:title "More"
     :parentId :textarea-to-code-editor
     :id :textarea-to-editor-more}
    (get-menus-for-modes (m/get-unused-modes modes used-modes)
                         current-mode :textarea-to-editor-more
                         sender-chan msg-chan)))

(defhandler clear-context-menu!
  "Clears context menu safely."
  []
  (c/clear-context-menu!))

(defhandler update-used-modes!
  "Updates list of used modes in local storage."
  [storage mode]
  (swap! storage update-in [:used-modes]
         m/update-used-modes mode))
