(ns textarea-to-code-editor.content.handlers-test
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [clj-di.test :refer [with-reset]])
  (:require [cemerick.cljs.test :refer-macros [deftest testing is done]]
            [cljs.core.async :refer [<! chan]]
            [textarea-to-code-editor.content.editor :as e]
            [textarea-to-code-editor.content.handlers :as h]))

(deftest test-change-mode!
  (testing "When el is editor and mode :textarea"
    (let [called (atom nil)]
      (with-redefs [e/is-editor? (constantly true)
                    e/to-textarea! #(reset! called %)]
        (h/change-mode! :el :textarea nil)
        (is (= @called :el)))))
  (testing "When el not editor"
    (let [called (atom nil)]
      (with-redefs [e/is-editor? (constantly false)
                    e/to-code-editor! (fn [& args] (reset! called args))]
        (h/change-mode! :el :div :chan)
        (is (= @called [:el :div :chan])))))
  (testing "When el is editor and mode is not :textarea"
    (let [called (atom nil)]
      (with-redefs [e/is-editor? (constantly true)
                    e/change-editor-mode! (fn [& args] (reset! called args))]
        (h/change-mode! :el :div :chan)
        (is (= @called [:el :div]))))))

(deftest ^:async test-populate-context-menu!
  (testing "Should put message in runtime chan"
    (go (let [ch (chan)]
          (with-reset [e/get-editor-mode (constantly :mode)
                       e/get-modes (constantly :modes)]
            (is (= :el (h/populate-context-menu! :el ch)))
            (is (= (<! ch) [:populate-context-menu {:current-mode :mode
                                                    :modes :modes}]))))
        (done))))

(deftest ^:async test-clear-context-menu!
  (testing "Should put message in runtime chan"
    (go (let [ch (chan)]
          (is (nil? (h/clear-context-menu! ch)))
          (is (= (<! ch) [:clear-context-menu nil])))
        (done))))
