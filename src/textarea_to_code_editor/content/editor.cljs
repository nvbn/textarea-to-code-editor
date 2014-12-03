(ns textarea-to-code-editor.content.editor
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [>! <! timeout]]
            [domina.css :refer [sel]]
            [domina.events :refer [listen! current-target]]
            [domina :refer [insert-before! destroy! set-styles! attr by-id
                            value set-value! set-attr!]]))

(def leave-timeout 50)

(defn subscribe-to-hover!
  "Subscribes to hover and puts messages in channel."
  [el ch]
  (doto el
    (listen! :mouseenter #(go (>! ch [:enter-editor (current-target %) nil])))
    (listen! :mouseleave #(go (<! (timeout leave-timeout))
                              (>! ch [:leave-editor nil nil])))))

(defn init-editor!
  "Initializes text editor."
  [textarea editor-el {:keys [mode]}]
  (let [editor (.edit js/ace editor-el)]
    (doto editor
      (.setTheme "ace/theme/monokai")
      (.setValue (value textarea))
      (.. getSession (setMode mode))
      (.. getSession (on "change" #(set-value! textarea (.getValue editor)))))))

(defn div-from-textarea!
  "Creates div from textarea."
  [textarea]
  (let [id (str (gensym))]
    (doto textarea
      (insert-before! (str "<div id='" id "' style='
                                  width: " (.-scrollWidth textarea) "px;
                                  height: " (.-scrollHeight textarea) "px;
                                  font-size: 16px;'
                                 class='textarea-to-code-editor-block'></div>"))
      (set-attr! :data-editor-id id)
      (set-styles! {:display "none"}))
    (by-id id)))

(defn to-code-editor!
  "Converts textarea to code editor."
  [el mode hover-chan]
  (let [editor-el (div-from-textarea! el)]
    (subscribe-to-hover! editor-el hover-chan)
    (init-editor! el editor-el mode)))

(defn is-editor?
  "Returns true when element is editor."
  [el]
  (= (.-tagName el) "DIV"))

(defn get-modes
  "Returns all available editor modes."
  []
  (let [ace-modes (.. js/ace (require "ace/ext/modelist") -modes)]
    (for [mode ace-modes]
      {:caption (.-caption mode)
       :mode (.-mode mode)})))

(defn get-editor-mode
  "Returns current editor mode."
  [el]
  (when (is-editor? el)
    (let [mode-id (.. js/ace (edit el) getSession getMode -$id)]
      (first (filter #(= (:mode %) mode-id) (get-modes))))))

(defn change-editor-mode!
  "Changes editor mode."
  [el {:keys [mode]}]
  (.. js/ace (edit el) getSession (setMode mode))
  el)

(defn to-textarea!
  "Converts code editor back to textarea."
  [el]
  (set-styles! (sel (str "[data-editor-id=" (attr el :id) "]"))
               {:display "block"})
  (destroy! el))
