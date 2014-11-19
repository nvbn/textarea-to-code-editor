(ns textarea-to-code-editor.background.core-test
  (:require-macros [cemerick.cljs.test :refer [deftest is use-fixtures testing done]]
                   [cljs.core.async.macros :refer [go]]
                   [clj-di.test :refer [with-fresh-dependencies with-reset]])
  (:require [cemerick.cljs.test]
            [cljs.core.async :refer [>! <! chan timeout]]
            [clj-di.core :refer [register! get-dep]]
            [textarea-to-code-editor.chrome.core :as c]
            [textarea-to-code-editor.background.core :as b]))

(use-fixtures :each #(with-fresh-dependencies (%)))

(def modes [{:caption "Python"
             :mode "ace/modes/python"}
            {:caption "JavaScript"
             :mode "ace/modes/js"}
            {:caption "Clojure"
             :mode "ace/modes/clojure"}
            {:caption "LaTeX"
             :mode "ace/modes/latex"}])

(deftest test-get-used-modes
  (register! :modes (atom modes)
             :local-storage (atom {:used-modes [{:caption "Python"
                                                 :mode "ace/modes/python"}
                                                {:caption "JavaScript"
                                                 :mode "ace/modes/js"}
                                                {:caption "Not exists lang"
                                                 :mode "ace/mode/not-eixsts-lang"}]}))
  (is (= (b/get-used-modes) [{:caption "Python"
                              :mode "ace/modes/python"}
                             {:caption "JavaScript"
                              :mode "ace/modes/js"}])))

(deftest test-update-used-modes!
  (testing "Without modes"
    (register! :local-storage (atom {:used-modes []})
               :modes (atom modes))
    (b/update-used-modes! {:caption "Python"
                           :mode "ace/modes/python"})
    (is (= (b/get-used-modes) [{:caption "Python"
                                :mode "ace/modes/python"}])))
  (testing "With more than need modes"
    (with-reset [b/used-modes-limit 2]
      (register! :local-storage (atom {:used-modes [{:caption "Python"
                                                     :mode "ace/modes/python"}
                                                    {:caption "Clojure"
                                                     :mode "ace/modes/clojure"}]}))
      (b/update-used-modes! {:caption "JavaScript"
                             :mode "ace/modes/js"})
      (is (= (b/get-used-modes) [{:caption "JavaScript"
                                  :mode "ace/modes/js"}
                                 {:caption "Python"
                                  :mode "ace/modes/python"}]))))
  (testing "When mode already in list"
    (register! :local-storage (atom {:used-modes [{:caption "Python"
                                                   :mode "ace/modes/python"}
                                                  {:caption "Clojure"
                                                   :mode "ace/modes/clojure"}
                                                  {:caption "JavaScript"
                                                   :mode "ace/modes/js"}]}))
    (b/update-used-modes! {:caption "Clojure"
                           :mode "ace/modes/clojure"})
    (is (= (b/get-used-modes) [{:caption "Clojure"
                                :mode "ace/modes/clojure"}
                               {:caption "Python"
                                :mode "ace/modes/python"}
                               {:caption "JavaScript"
                                :mode "ace/modes/js"}]))))

(deftest test-get-unused-modes
  (register! :modes (atom modes)
             :local-storage (atom {:used-modes [{:caption "Clojure"
                                                 :mode "ace/modes/clojure"}]}))
  (is (= (b/get-unused-modes)
         [{:caption "JavaScript"
           :mode "ace/modes/js"}
          {:caption "LaTeX"
           :mode "ace/modes/latex"}
          {:caption "Python"
           :mode "ace/modes/python"}])))

(deftest test-create-context-menus!
  (let [menus (atom [])]
    (register! :chrome (reify c/chrome
                         (create-context-menu [_ params] (swap! menus conj params))))
    (b/create-context-menus! {:title "first"}
                             [{:title "second"}
                              {:title "third"}]
                             {:title "fourth"})
    (is (= @menus [{:title "first"
                    :contexts [:all]}
                   {:title "second"
                    :contexts [:all]}
                   {:title "third"
                    :contexts [:all]}
                   {:title "fourth"
                    :contexts [:all]}]))))

(deftest test-get-menus-for-modes
  (is (= (map #(dissoc % :onclick) (b/get-menus-for-modes [{:caption "JavaScript"
                                                            :mode "ace/modes/js"}
                                                           {:caption "LaTeX"
                                                            :mode "ace/modes/latex"}
                                                           {:caption "Python"
                                                            :mode "ace/modes/python"}]
                                                          {:caption "Python"
                                                           :mode "ace/modes/python"}
                                                          :parent-id
                                                          nil))
         [{:title "JavaScript"
           :parentId :parent-id
           :type :checkbox
           :checked false}
          {:title "LaTeX"
           :parentId :parent-id
           :type :checkbox
           :checked false}
          {:title "Python"
           :parentId :parent-id
           :type :checkbox
           :checked true}])))

(deftest test-populate-context-menu!
  (let [menus (atom [])
        messages (atom [])
        cleared (atom false)]
    (register! :chrome (reify c/chrome
                         (create-context-menu [_ data] (swap! menus conj data))
                         (clear-context-menu [_] (reset! cleared true))
                         (send-message-to-tab [_ tab request data]
                           (swap! messages conj [tab request data]))
                         (send-message-to-tab [_ tab request]
                           (swap! messages conj [tab request nil])))
               :modes (atom modes)
               :local-storage (atom {:used-modes [{:caption "Clojure"
                                                   :mode "ace/modes/clojure"}]}))
    (b/populate-context-menu! #js {:tab "tab"})
    (testing "Menu items ordering"
      (is (= (map #(vector (:title %) (:parentId %)) @menus)
             [["Textarea to code editor" nil]
              ["Normal textarea" :textarea-to-code-editor]
              [nil :textarea-to-code-editor]
              ["Clojure" :textarea-to-code-editor]
              ["More" :textarea-to-code-editor]
              ["JavaScript" :textarea-to-editor-more]
              ["LaTeX" :textarea-to-editor-more]
              ["Python" :textarea-to-editor-more]])))
    (testing "Menu should be cleared before"
      (is @cleared))
    (testing "Checked when mode is nil"
      (is (:checked (second @menus))))
    (testing "Checked with mode"
      (reset! menus [])
      (b/populate-context-menu! #js {:tab "tab"} {:caption "Clojure"
                                                  :mode "ace/modes/clojure"})
      (is (:checked (first (drop 3 @menus)))))
    (testing "Menu items on-click event"
      ((:onclick (second @menus)))
      ((:onclick (first (drop 3 @menus))))
      (is (= @messages [["tab" :change-mode nil]
                        ["tab" :change-mode {:caption "Clojure"
                                             :mode "ace/modes/clojure"}]])))))

(deftest ^:async test-handle-messages!
  (let [clear-context-menu-called (atom false)]
    (register! :chrome (reify c/chrome
                         (clear-context-menu [_] (reset! clear-context-menu-called true)))
               :modes (atom {}))
    (go (let [msg-chan (chan)]
          (b/handle-messages! msg-chan)
          (testing ":populate-context-menu message"
            (let [called-with (atom nil)]
              (with-reset [b/populate-context-menu! #(reset! called-with [%1 %2])]
                (>! msg-chan [:populate-context-menu
                              {:caption "Clojure"
                               :mode "ace/modes/clojure"}
                              :sender])
                (<! (timeout 100))
                (is (= @called-with [:sender {:caption "Clojure"
                                              :mode "ace/modes/clojure"}])))))
          (testing ":clear-context-menu message"
            (>! msg-chan [:clear-context-menu nil nil])
            (<! (timeout 100))
            (is @clear-context-menu-called))
          (testing ":update-modes message"
            (>! msg-chan [:update-modes modes nil])
            (<! (timeout 100))
            (is (= @(get-dep :modes) modes))))
        (done))))
