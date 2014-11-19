(ns textarea-to-code-editor.background.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [clj-di.core :refer [let-deps]])
  (:require [cljs.core.async :refer [<! chan alts!]]
            [clj-di.core :refer [register! get-dep]]
            [alandipert.storage-atom :refer [local-storage]]
            [textarea-to-code-editor.chrome.core :as c]))

(def used-modes-limit 5)

(defn get-used-modes
  "Returns list of used modes."
  []
  (let [modes (->> @(get-dep :modes) set)]
    (->> @(get-dep :local-storage)
         :used-modes
         (filter modes))))

(defn update-used-modes!
  "Update list of used modes."
  [mode]
  (let-deps [storage :local-storage]
    (swap! storage update-in [:used-modes]
           (fn [used-modes]
             (->> used-modes
                  (remove #(= mode %))
                  (into [mode])
                  (take used-modes-limit))))))

(defn get-unused-modes
  "Returns list with ordered unused modes."
  []
  (let [used-modes (set (get-used-modes))]
    (->> @(get-dep :modes)
         (filter (complement used-modes))
         (sort-by :caption))))

(defn create-context-menus!
  "Creates context menus instances in chrome."
  [& menus]
  (doseq [menu (flatten menus)]
    (c/create-context-menu* (assoc menu
                              :contexts [:all]))))

(defn get-menus-for-modes
  "Returns params for context menus for passed modes"
  [modes current-mode parent-id sender]
  (for [mode modes]
    {:title (:caption mode)
     :parentId parent-id
     :type :checkbox
     :checked (= current-mode mode)
     :onclick (fn []
                (c/send-message-to-tab* (.-tab sender)
                                        :change-mode mode)
                (update-used-modes! mode))}))

(defn populate-context-menu!
  "Shows context menu when mouse on textarea."
  ([sender] (populate-context-menu! sender nil))
  ([sender current-mode]
    (c/clear-context-menu*)
    (create-context-menus!
      {:title "Textarea to code editor"
       :id :textarea-to-code-editor}
      {:title "Normal textarea"
       :parentId :textarea-to-code-editor
       :type :checkbox
       :checked (nil? current-mode)
       :onclick #(c/send-message-to-tab* (.-tab sender)
                                         :change-mode)}
      {:parentId :textarea-to-code-editor
       :type :separator}
      (get-menus-for-modes (get-used-modes)  current-mode
                           :textarea-to-code-editor sender)
      {:title "More"
       :parentId :textarea-to-code-editor
       :id :textarea-to-editor-more}
      (get-menus-for-modes (get-unused-modes) current-mode
                           :textarea-to-editor-more sender))))

(defn handle-messages!
  "Handles messages received from content."
  [msg-chan]
  (go-loop []
    (let [[request data sender] (<! msg-chan)]
      (condp = request
        :populate-context-menu (populate-context-menu! sender data)
        :clear-context-menu (c/clear-context-menu*)
        :update-modes (reset! (get-dep :modes) data))
      (recur))))

(when (c/available?)
  (register! :chrome (c/real-chrome.)
             :modes (atom [])
             :local-storage (local-storage (atom
                                             {:used-modes [{:caption "Python"
                                                            :mode "ace/mode/python"}
                                                           {:caption "SH"
                                                            :mode "ace/mode/sh"}
                                                           {:caption "JavaScript"
                                                            :mode "ace/mode/javascript"}
                                                           {:caption "HTML"
                                                            :mode "ace/mode/html"}
                                                           {:caption "Markdown"
                                                            :mode "ace/mode/markdown"}]})
                                           :textarea-to-code-editor-2))
  (handle-messages! (c/get-messages-chan)))
