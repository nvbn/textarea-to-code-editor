(ns textarea-to-code-editor.background.core-test
  (:require-macros [cemerick.cljs.test :refer [deftest is use-fixtures testing done]]
                   [cljs.core.async.macros :refer [go]]
                   [clj-di.test :refer [with-fresh-dependencies]]
                   [clj-di.core :refer [with-reset]])
  (:require [cemerick.cljs.test]
            [cljs.core.async :refer [>! <! chan timeout]]
            [clj-di.core :refer [register! get-dep]]
            [textarea-to-code-editor.chrome.core :as c]
            [textarea-to-code-editor.background.core :as b]))

(use-fixtures :each #(with-fresh-dependencies (%)))

(def modes {:python {:caption "Python"
                     :mode "ace/modes/python"}
            :js {:caption "JavaScript"
                 :mode "ace/modes/js"}
            :clojure {:caption "Clojure"
                      :mode "ace/modes/clojure"}
            :latex {:caption "LaTeX"
                    :mode "ace/modes/latex"}})

(deftest test-get-ordered-modes
  (register! :modes (atom modes))
  (is (= (b/get-ordered-modes)
         [["Clojure" "ace/modes/clojure"]
          ["JavaScript" "ace/modes/js"]
          ["LaTeX" "ace/modes/latex"]
          ["Python" "ace/modes/python"]])))

(deftest test-show-textarea-context-menu
  (let [menus (atom [])
        messages (atom [])]
    (register! :chrome (reify c/chrome
                         (create-context-menu [_ data] (swap! menus conj data))
                         (send-message-to-tab [_ tab request data]
                           (swap! messages conj [tab request data])))
               :modes (atom modes))
    (b/show-textarea-context-menu #js {:tab "tab"})
    (testing "Menu items ordering"
      (is (= (map #(dissoc % :onclick) @menus) [{:title "Convert to code editor"
                                                 :contexts [:all]
                                                 :id :textarea-to-editor}
                                                {:title "Clojure"
                                                 :contexts [:all]
                                                 :parentId :textarea-to-editor}
                                                {:title "JavaScript"
                                                 :contexts [:all]
                                                 :parentId :textarea-to-editor}
                                                {:title "Python"
                                                 :contexts [:all]
                                                 :parentId :textarea-to-editor}
                                                {:title "More"
                                                 :contexts [:all]
                                                 :parentId :textarea-to-editor
                                                 :id :textarea-to-editor-more}
                                                {:title "Clojure"
                                                 :contexts [:all]
                                                 :parentId :textarea-to-editor-more}
                                                {:title "JavaScript"
                                                 :contexts [:all]
                                                 :parentId :textarea-to-editor-more}
                                                {:title "LaTeX"
                                                 :contexts [:all]
                                                 :parentId :textarea-to-editor-more}
                                                {:title "Python"
                                                 :contexts [:all]
                                                 :parentId :textarea-to-editor-more}])))
    (testing "Menu items on-click event"
      ((:onclick (second @menus)))
      (is (= @messages [["tab" :to-code-editor "ace/modes/clojure"]])))))

(deftest test-show-editor-context-menu
  (let [menus (atom [])
        messages (atom [])]
    (register! :chrome (reify c/chrome
                         (create-context-menu [_ data] (swap! menus conj data))
                         (send-message-to-tab [_ tab request]
                           (swap! messages conj [tab request]))))
    (b/show-editor-context-menu #js {:tab "tab"})
    (testing "Menu items ordering"
      (is (= (map #(dissoc % :onclick) @menus) [{:title "Convert to textarea"
                                                 :contexts [:all]}])))
    (testing "Menu items on-click event"
      ((:onclick (first @menus)))
      (is (= @messages [["tab" :to-textarea]])))))

(deftest ^:async test-handle-messages!
  (let [clear-context-menu-called (atom false)]
    (register! :chrome (reify c/chrome
                         (clear-context-menu [_] (reset! clear-context-menu-called true)))
               :modes (atom {}))
    (go (let [msg-chan (chan)]
          (b/handle-messages! msg-chan)
          (testing ":enter message"
            (let [called-with (atom nil)]
              (with-reset [b/show-textarea-context-menu #(reset! called-with %)]
                (>! msg-chan [:enter nil :sender])
                (<! (timeout 100))
                (is (= @called-with :sender)))))
          (testing ":editor-enter message"
            (let [called-with (atom nil)]
              (with-reset [b/show-editor-context-menu #(reset! called-with %)]
                (>! msg-chan [:editor-enter nil :sender])
                (<! (timeout 100))
                (is (= @called-with :sender)))))
          (testing ":leave message"
            (>! msg-chan [:leave nil nil])
            (<! (timeout 100))
            (is @clear-context-menu-called))
          (testing ":update-modes message"
            (>! msg-chan [:update-modes modes nil])
            (<! (timeout 100))
            (is (= @(get-dep :modes) modes))))
        (done))))
