(ns textarea-to-code-editor.content.core-test
  (:require-macros [cemerick.cljs.test :refer [deftest is use-fixtures testing done]]
                   [cljs.core.async.macros :refer [go]]
                   [clj-di.test :refer [with-fresh-dependencies]]
                   [clj-di.core :refer [with-reset]])
  (:require [cemerick.cljs.test]
            [cljs.core.async :refer [<! >! chan timeout]]
            [clj-di.core :refer [register!]]
            [domina.css :refer [sel]]
            [domina.events :refer [dispatch! root-element is-event-target?]]
            [domina :refer [append! styles text attr has-class? classes
                            destroy-children! by-id nodes]]
            [textarea-to-code-editor.chrome.core :as c]
            [textarea-to-code-editor.content.core :as ct]))

(use-fixtures :each
  #(with-fresh-dependencies
    (%)
    (destroy-children! (sel "body"))))

(def mode "ace/mode/clojure")

(deftest ^:async test-get-hover-chan
  (go (let [id (str (gensym))]
        (append! (sel "body") (str "<textarea id='" id "'>test</textarea>"))
        (let [el (sel (str "#" id))
              ch (ct/get-hover-chan)]
          (testing "On mouse enter"
            (dispatch! el :mouseenter {})
            (is (= (first (<! ch)) :enter)))
          (testing "On mouse leave"
            (dispatch! el :mouseleave {})
            (is (= (first (<! ch)) :leave)))
          (done)))))

(deftest test-init-editor!
  (let [textarea-id (str (gensym))
        div-id (str (gensym))]
    (append! (sel "body") (str "<textarea id='" textarea-id "'>test content</textarea>"))
    (append! (sel "body") (str "<div id='" div-id "'></div>"))
    (let [textarea (by-id textarea-id)
          editor (ct/init-editor! textarea div-id mode)]
      (testing "Text editor content should be equal to textarea content"
        (is (= (.getValue editor) (text textarea))))
      (testing "Text editor mode should be setted"
        (is (= (.. editor getSession getMode -$id) mode)))
      (testing "Textarea content should be changed when editor content changed"
        (.setValue editor "new content")
        (is (= (text textarea) "new content"))))))

(deftest ^:async test-subscribe-to-editor-events!
  (go (let [ch (chan)
            div-id (str (gensym))]
        (append! (sel "body") (str "<div id='" div-id "'></div>"))
        (let [div (by-id div-id)]
          (ct/subscribe-to-editor-events! ch div)
          (testing "On mouse enter"
            (dispatch! div :mouseenter {})
            (is (= (first (<! ch)) :editor-enter)))
          (testing "On mouse leave"
            (dispatch! div :mouseleave {})
            (is (= (first (<! ch)) :leave)))))
      (done)))

(deftest test-div-from-textarea!
  (let [textarea-id (str (gensym))]
    (append! (sel "body") (str "<textarea style='width: 300px; height: 600px;'
                                          id='" textarea-id "'>test content</textarea>"))
    (let [textarea (by-id textarea-id)
          div (ct/div-from-textarea! textarea)]
      (testing "Should have same width and height"
        (is (= (attr textarea :scrollWidth)
               (attr div :scrollWidth)))
        (is (= (attr textarea :scrollHeight)
               (attr div :scrollHeight))))
      (testing "Textarea should have class equal to div id"
        (is (has-class? textarea (attr div :id))))
      (testing "Textarea shoul be hidden"
        (is (= "none" (:display (styles textarea))))))))

(deftest test-to-code-editor
  (let [textarea-id (str (gensym))]
    (append! (sel "body") (str "<textarea id='" textarea-id "'></textarea>"))
    (testing "Should return editor when el is not nil"
      (is (ct/to-code-editor (by-id textarea-id) (chan) mode)))
    (testing "Should return nil when el is nil"
      (is (nil? (ct/to-code-editor nil (chan) mode))))))

(deftest test-to-textarea
  (let [textarea-id (str (gensym))]
    (append! (sel "body") (str "<textarea id='" textarea-id "'></textarea>"))
    (let [textarea (by-id textarea-id)]
      (ct/to-code-editor textarea (chan) mode)
      (ct/to-textarea (-> textarea classes first by-id))
      (testing "Textarea should be visible"
        (is (= "block" (:display (styles textarea)))))
      (testing "Div should be removed"
        (is (-> (sel "div") nodes count zero?))))))

(deftest test-update-editor-modes
  (let [msg (atom {})]
    (register! :chrome (reify c/chrome
                         (send-message [_ request data] (reset! msg [request data]))))
    (ct/update-editor-modes)
    (is (= :update-modes (first @msg)))
    (is (= (last @msg) (.. js/ace (require "ace/ext/modelist") -modesByName)))))

(deftest ^:async test-handle-messages!
  (let [msgs (atom [])
        hover-chan (chan)
        msg-chan (chan)]
    (register! :chrome (reify c/chrome
                         (send-message [_ request data] (swap! msgs conj [request data]))
                         (send-message [_ request] (swap! msgs conj [request nil]))))
    (ct/handle-messages! hover-chan msg-chan)
    (go (testing ":enter message"
          (let [update-modes-called (atom false)]
            (with-reset [ct/update-editor-modes #(reset! update-modes-called true)]
              (>! hover-chan [:enter :el])
              (<! (timeout 500))
              (is @update-modes-called)
              (is (= (last @msgs) [:enter nil])))))
        (reset! msgs [])
        (testing ":to-code-editor message"
          (let [to-code-editor-args (atom nil)]
            (with-reset [ct/to-code-editor (fn [& args] (reset! to-code-editor-args args))]
              (>! msg-chan [:to-code-editor mode])
              (<! (timeout 500))
              (is (= @to-code-editor-args [:el hover-chan mode]))
              (is (= (last @msgs) [:leave nil])))))
        (reset! msgs [])
        (testing ":leave message"
          (>! hover-chan [:leave nil])
          (<! (timeout 500))
          (is (= (last @msgs) [:leave nil])))
        (reset! msgs [])
        (testing ":editor-enter message"
          (>! hover-chan [:editor-enter :editor])
          (<! (timeout 500))
          (is (= (last @msgs) [:editor-enter nil])))
        (reset! msgs [])
        (testing ":to-textarea message"
          (let [to-textarea-args (atom nil)]
            (with-reset [ct/to-textarea (fn [& args] (reset! to-textarea-args args))]
              (>! msg-chan [:to-textarea nil])
              (<! (timeout 500))
              (is (= @to-textarea-args [:editor])))))
        (done))))
