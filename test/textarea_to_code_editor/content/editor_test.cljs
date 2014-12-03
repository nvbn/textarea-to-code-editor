(ns textarea-to-code-editor.content.editor-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cemerick.cljs.test :refer-macros [deftest testing is done use-fixtures]]
            [cljs.core.async :refer [<! >! chan]]
            [domina.css :refer [sel]]
            [domina.events :refer [dispatch! root-element is-event-target?]]
            [domina :refer [append! styles attr has-class? classes
                            destroy-children! by-id nodes value]]
            [textarea-to-code-editor.content.editor :as e]))

(use-fixtures :each
  (fn [f]
    (f)
    (destroy-children! (sel "body"))))

(def mode {:caption "Clojure"
           :mode "ace/mode/clojure"})

(def py-mode {:caption "Python"
              :mode "ace/mode/python"})

(defn add-el-with-random-id!
  ([el] (add-el-with-random-id! el ""))
  ([el content]
    (let [el-id (str (gensym))
          el-name (clj->js el)]
      (append! (sel "body") (str "<" el-name " id='" el-id "'>" content "</" el-name ">"))
      (by-id el-id))))

(deftest ^:async test-subscribe-to-hover!
  (go (let [el (add-el-with-random-id! :textarea "test")
            ch (chan)]
        (e/subscribe-to-hover! el ch)
        (testing "On mouse enter"
          (dispatch! el :mouseenter {})
          (is (= (first (<! ch)) :enter-editor)))
        (testing "On mouse leave"
          (dispatch! el :mouseleave {})
          (is (= (first (<! ch)) :leave-editor)))
        (done))))

(deftest test-init-editor!
  (let [textarea (add-el-with-random-id! :textarea "test content")
        div (add-el-with-random-id! :div)
        editor (e/init-editor! textarea div mode)]
    (testing "Text editor content should be equal to textarea content"
      (is (= (.getValue editor) (value textarea))))
    (testing "Text editor mode should be setted"
      (is (= (.. editor getSession getMode -$id) (:mode mode))))
    (testing "Textarea content should be changed when editor content changed"
      (.setValue editor "new content")
      (is (= (value textarea) "new content")))))

(deftest test-div-from-textarea!
  (let [textarea (add-el-with-random-id! :textarea "test content")
        div (e/div-from-textarea! textarea)]
    (testing "Textarea should have class equal to div id"
      (is (= (attr textarea :data-editor-id) (attr div :id))))
    (testing "Textarea shoul be hidden"
      (is (= "none" (:display (styles textarea)))))))

(deftest test-to-code-editor!
  (let [textarea (add-el-with-random-id! :textarea)]
    (testing "Should return editor"
      (is (e/to-code-editor! textarea mode (chan))))))

(deftest test-is-editor?
  (testing "Should return true for div"
    (let [div (add-el-with-random-id! :div)]
      (is (true? (e/is-editor? div)))))
  (testing "Should return false for textarea"
    (let [textarea (add-el-with-random-id! :textarea)]
      (is (false? (e/is-editor? textarea))))))

(deftest test-get-modes
  (testing "Should return all available modes in {:caption :mode} format"
    (is (-> (e/get-modes) first :mode))
    (is (-> (e/get-modes) first :caption))))

(deftest test-get-editor-mode
  (testing "nil for not editor"
    (let [textarea (add-el-with-random-id! :textarea)]
      (is (nil? (e/get-editor-mode textarea)))))
  (testing "current mode for editor"
    (let [textarea (add-el-with-random-id! :textarea)
          div (add-el-with-random-id! :div)]
      (e/init-editor! textarea div mode)
      (is (= (e/get-editor-mode div) mode)))))

(deftest test-change-editor-mode!
  (testing "Change editor mode"
    (let [textarea (add-el-with-random-id! :textarea)
          div (add-el-with-random-id! :div)]
      (e/init-editor! textarea div mode)
      (e/change-editor-mode! div py-mode)
      (is (= (e/get-editor-mode div) py-mode)))))

(deftest test-to-textarea!
  (let [textarea (add-el-with-random-id! :textarea)
        div (add-el-with-random-id! :div)]
    (e/init-editor! textarea div mode)
    (e/to-textarea! div)
    (testing "Div should be removed"
      (is (-> (sel "div") nodes count zero?)))))
