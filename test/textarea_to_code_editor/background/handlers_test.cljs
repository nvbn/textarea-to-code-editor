(ns textarea-to-code-editor.background.handlers-test
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [clj-di.test :refer [with-reset]])
  (:require [cemerick.cljs.test :refer-macros [deftest is testing done]]
            [cljs.core.async :refer [<! chan]]
            [textarea-to-code-editor.background.chrome :as c]
            [textarea-to-code-editor.background.handlers :as h]))

(deftest test-create-context-menus!
  (testing "List of menus should be flatten and context should be defined"
    (let [menus (atom [])]
      (with-redefs [c/create-context-menu! #(swap! menus conj %)]
        (h/create-context-menus! {:id 0}
                                 [{:id 1} {:id 2}]
                                 {:id 3})
        (is (= @menus (map #(hash-map :id % :contexts [:all]) (range 4))))))))

(deftest ^:async test-get-menus-for-modes
  (let [sender-chan (chan)
        msg-chan (chan)
        menus (h/get-menus-for-modes [{:caption "Python"
                                       :mode "ace/mode/python"}
                                      {:caption "SH"
                                       :mode "ace/mode/sh"}]
                                     {:caption "Python"
                                      :mode "ace/mode/python"}
                                     :parent
                                     sender-chan
                                     msg-chan)]
    (testing "`title` should be from mode"
      (is (= (map :title menus) ["Python" "SH"])))
    (testing "Current mode should be `checked`"
      (is (= (map :checked menus) [true false])))
    (testing "Messages should be sended on click"
      (go ((:onclick (first menus)))                        ; call onclick of first menu entry
          (is (= (<! sender-chan) [:change-mode {:caption "Python"
                                                 :mode "ace/mode/python"}]))
          (is (= (<! msg-chan) [:update-used-modes
                                {:caption "Python"
                                 :mode "ace/mode/python"}
                                nil]))
          (done)))))

(deftest ^:async test-populate-context-menu!
  (go (let [context-menu-cleared (atom 0)
            menus (atom [])
            sender-chan (chan)
            msg-chan (chan)]
        (with-reset [c/clear-context-menu! #(swap! context-menu-cleared inc)
                     h/create-context-menus! (fn [& args] (reset! menus (flatten args)))]
          (h/populate-context-menu! {:current-mode {:caption "Python"
                                                    :mode "ace/mode/python"}
                                     :modes [{:caption "Python"
                                              :mode "ace/mode/python"}
                                             {:caption "SH"
                                              :mode "ace/mode/sh"}]}
                                    [{:caption "SH"
                                      :mode "ace/mode/sh"}]
                                    sender-chan
                                    msg-chan)
          (testing "Menu should be cleared once"
            (is (= 1 @context-menu-cleared)))
          (testing "Ordering of items"
            (is (= (map #(vector (:title %) (:parentId %)) @menus)
                   [["Textarea to code editor" nil]
                    ["Normal textarea" :textarea-to-code-editor]
                    [nil :textarea-to-code-editor]
                    ["SH" :textarea-to-code-editor]
                    ["More" :textarea-to-code-editor]
                    ["Python" :textarea-to-editor-more]])))
          (testing "Convert to narmal textarea"
            ((:onclick (second @menus)))                    ; call onclick of "Normal textarea"
            (is (= (<! sender-chan) [:change-mode :textarea])))))
      (done)))

(deftest test-update-used-modes!
  (testing "Used modes in local storage atom should be changed"
    (let [storage (atom {:used-modes [{:caption "Python"
                                       :mode "ace/mode/python"}
                                      {:caption "SH"
                                       :mode "ace/mode/sh"}]})]
      (h/update-used-modes! storage {:caption "HTML"
                                     :mode "ace/mode/html"})
      (is (= @storage {:used-modes [{:caption "HTML"
                                     :mode "ace/mode/html"}
                                    {:caption "Python"
                                     :mode "ace/mode/python"}
                                    {:caption "SH"
                                     :mode "ace/mode/sh"}]})))))
