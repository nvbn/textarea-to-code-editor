(ns textarea-to-code-editor.content.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [>! <! chan alts! timeout]]
            [domina.css :refer [sel]]
            [domina.events :refer [listen! current-target]]
            [domina :refer [insert-before! destroy! set-styles! add-class!
                            attr by-class by-id value set-value!]]
            [clj-di.core :refer [register!]]
            [textarea-to-code-editor.chrome.core :as c]))

(def leave-timeout 50)

(defn get-hover-chan
  "Returns chan in which we put hovering related events."
  []
  (let [ch (chan)]
    (doto (sel "textarea")
      (listen! :mouseenter #(go (>! ch [:enter (current-target %)])))
      (listen! :mouseleave #(go (<! (timeout leave-timeout))
                                (>! ch [:leave]))))
    ch))

(defn init-editor!
  "Initializes text editor."
  [textarea id mode]
  (let [editor (.edit js/ace id)]
    (doto editor
      (.setTheme "ace/theme/monokai")
      (.setValue (value textarea))
      (.. getSession (setMode mode))
      (.. getSession (on "change" #(set-value! textarea (.getValue editor)))))))

(defn subscribe-to-editor-events!
  "Subscribes to editor events and puts it to hover channel."
  [hover-chan editor-el]
  (doto editor-el
    (listen! :mouseenter #(go (>! hover-chan [:editor-enter (current-target %)])))
    (listen! :mouseleave #(go (<! (timeout leave-timeout))
                              (>! hover-chan [:leave])))))

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
      (add-class! id)
      (set-styles! {:display "none"}))
    (by-id id)))

(defn to-code-editor
  "Converts textarea to code editor."
  [el hover-chan mode]
  (when el
    (let [editor-el (div-from-textarea! el)
          id (attr editor-el :id)]
      (subscribe-to-editor-events! hover-chan editor-el)
      (init-editor! el id mode))))

(defn to-textarea
  "Converts code editor back to textarea."
  [el]
  (when el
    (set-styles! (by-class (attr el :id)) {:display "block"})
    (destroy! el)))

(defn update-editor-modes
  "Sends editor modes to backend."
  []
  (c/send-message* :update-modes (.. js/ace (require "ace/ext/modelist")
                                     -modesByName)))

(defn handle-messages!
  "Handle messages for background and events."
  [hover-chan msg-chan]
  (go-loop [active nil]
    (let [[[request data _] _] (alts! [hover-chan msg-chan])]
      (recur (condp = request
               :enter (do (update-editor-modes)
                          (c/send-message* :enter)
                          data)
               :leave (do (c/send-message* :leave) nil)
               :editor-enter (do (c/send-message* :editor-enter) data)
               :to-code-editor (do (to-code-editor active hover-chan data)
                                   (c/send-message* :leave)
                                   nil)
               :to-textarea (do (to-textarea active) nil))))))

(when (c/available?)
  (register! :chrome (c/real-chrome.))
  (handle-messages! (get-hover-chan) (c/get-messages-chan)))
