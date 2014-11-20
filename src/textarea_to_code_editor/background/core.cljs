(ns textarea-to-code-editor.background.core
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]
                   [cljs.core.match.macros :refer [match]])
  (:require [cljs.core.match]
            [cljs.core.async :refer [<! >! chan alts!]]
            [alandipert.storage-atom :refer [local-storage]]
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
  [modes current-mode parent-id sender-ch msg-chan]
  (for [mode modes]
    {:title (:caption mode)
     :parentId parent-id
     :type :checkbox
     :checked (= current-mode mode)
     :onclick #(go (>! sender-ch [:change-mode mode])
                   (>! msg-chan [[:update-used-modes mode]]))}))

(defn populate-context-menu!
  "Shows context menu when mouse on textarea."
  [sender-ch {:keys [current-mode modes]} used-modes msg-chan]
  (c/clear-context-menu!)
  (create-context-menus!
    {:title "Textarea to code editor"
     :id :textarea-to-code-editor}
    {:title "Normal textarea"
     :parentId :textarea-to-code-editor
     :type :checkbox
     :checked (nil? current-mode)
     :onclick #(go (>! sender-ch [:change-mode :textarea]))}
    {:parentId :textarea-to-code-editor
     :type :separator}
    (get-menus-for-modes (m/sanitize-used-modes modes used-modes)
                         current-mode :textarea-to-code-editor
                         sender-ch msg-chan)
    {:title "More"
     :parentId :textarea-to-code-editor
     :id :textarea-to-editor-more}
    (get-menus-for-modes (m/get-unused-modes modes used-modes)
                         current-mode :textarea-to-editor-more
                         sender-ch msg-chan)))

(defn get-storage
  "Returns configured local storage atom."
  []
  (local-storage (atom {:used-modes m/default-used-modes})
                 :textarea-to-code-editor-2))

(defn update-used-modes!
  "Updates list of used modes in local storage."
  [storage mode]
  (swap! storage update-in [:used-modes]
         m/update-used-modes mode))

(defn handle-messages!
  "Handles messages received from content."
  [msg-chan storage]
  (go-loop []
    (match (<! msg-chan)
      [[:populate-context-menu data] sender] (populate-context-menu! sender data
                                                                     (:used-modes @storage)
                                                                     msg-chan)
      [[:clear-context-menu] _] (c/clear-context-menu!)
      [[:update-used-modes mode]] (update-used-modes! storage mode))
    (recur)))

(when (c/available?)
  (let [msg-chan (chan)]
    (c/subscribe-to-runtime! msg-chan)
    (handle-messages! msg-chan (get-storage))))
