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

(defn populate-menus!
  [& menus]
  (doseq [menu (flatten menus)]
    (c/create-context-menu* (assoc menu
                              :contexts [:all]))))

(defn get-menus-for-modes
  [modes parent-id current-mode sender]
  (for [[caption mode] modes]
    {:title caption
     :parentId parent-id
     :type :checkbox
     :checked (= current-mode mode)
     :onclick (fn []
                (c/send-message-to-tab* (.-tab sender)
                                        (if (nil? current-mode)
                                          :to-code-editor
                                          :change-editor-mode)
                                        mode)
                (update-used-modes! caption))}))

(defn show-textarea-context-menu
  "Shows context menu when mouse on textarea."
  ([sender] (show-textarea-context-menu sender nil))
  ([sender current-mode]
    (c/clear-context-menu*)
    (populate-menus! {:title "Textarea to code editor"
                      :id :textarea-to-code-editor}
                     {:title "Normal textarea"
                      :parentId :textarea-to-code-editor
                      :type :checkbox
                      :checked (nil? current-mode)
                      :onclick #(when current-mode
                                 (c/send-message-to-tab* (.-tab sender)
                                                         :to-textarea))}
                     {:parentId :textarea-to-code-editor
                      :type :separator}
                     (get-menus-for-modes (get-used-modes)
                                          :textarea-to-code-editor
                                          current-mode
                                          sender)
                     {:title "More"
                      :parentId :textarea-to-code-editor
                      :id :textarea-to-editor-more}
                     (get-menus-for-modes (get-ordered-modes)
                                          :textarea-to-editor-more
                                          current-mode
                                          sender))))

(defn handle-messages!
  "Handles messages received from content."
  [msg-chan]
  (go-loop []
    (let [[request data sender] (<! msg-chan)]
      (condp = request
        :enter (show-textarea-context-menu sender)
        :editor-enter (show-textarea-context-menu sender data)
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
