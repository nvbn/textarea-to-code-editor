(ns textarea-to-code-editor.background.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [clj-di.core :refer [let-deps]])
  (:require [cljs.core.async :refer [<! chan alts!]]
            [clj-di.core :refer [register! get-dep]]
            [alandipert.storage-atom :refer [local-storage]]
            [textarea-to-code-editor.chrome.core :as c]))

(def used-modes-limit 5)

(defn get-mode-by-caption
  "Returns mode by caption."
  [caption]
  (some->> @(get-dep :modes)
           vals
           (filter #(= (:caption %) caption))
           first
           (#(vector (:caption %) (:mode %)))))

(defn get-used-modes
  "Returns list of used modes."
  []
  (->> @(get-dep :local-storage)
       :used-modes
       (map get-mode-by-caption)
       (remove nil?)))

(defn update-used-modes!
  "Update list of used modes."
  [caption]
  (let-deps [storage :local-storage]
    (swap! storage update-in [:used-modes]
           (fn [used-modes]
             (->> used-modes
                  (remove #(= caption %))
                  (into [caption])
                  (take used-modes-limit))))))

(defn get-ordered-modes
  "Returns list with ordered modes."
  []
  (->> @(get-dep :modes)
       (map (fn [[_ {:keys [caption mode]}]] [caption mode]))
       (sort-by first)))

(defn show-textarea-context-menu
  "Shows context menu when mouse on textarea."
  [sender]
  (let [on-click (fn [[caption mode]] (fn []
                                        (c/send-message-to-tab* (.-tab sender)
                                                                :to-code-editor
                                                                mode)
                                        (update-used-modes! caption)))]
    (c/create-context-menu! "Convert to code editor"
                            :id :textarea-to-editor)
    (doseq [mode (get-used-modes)]
      (c/create-context-menu! (first mode)
                              :parentId :textarea-to-editor
                              :onclick (on-click mode)))
    (c/create-context-menu! "More"
                            :parentId :textarea-to-editor
                            :id :textarea-to-editor-more)
    (doseq [mode (get-ordered-modes)]
      (c/create-context-menu! (first mode)
                              :parentId :textarea-to-editor-more
                              :onclick (on-click mode)))))

(defn show-editor-context-menu
  "Shows context menu when mouse on code editor."
  [sender]
  (c/create-context-menu* {:title "Convert to textarea"
                           :contexts [:all]
                           :onclick #(c/send-message-to-tab* (.-tab sender)
                                                             :to-textarea)}))
(defn handle-messages!
  "Handles messages received from content."
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
             :modes (atom nil)
             :local-storage (local-storage (atom {:used-modes ["Python"
                                                               "SH"
                                                               "JavaScript"
                                                               "HTML"
                                                               "Markdown"]})
                                           :textarea-to-code-editor))
  (handle-messages! (c/get-messages-chan)))
