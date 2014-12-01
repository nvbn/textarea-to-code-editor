(ns textarea-to-code-editor.background.modes-test
  (:require [cemerick.cljs.test :refer-macros [deftest is testing]]
            [textarea-to-code-editor.background.modes :as m]))

(deftest test-sanitize-modes
  (testing "Invalid used modes should be removed"
    (is (= (m/sanitize-used-modes [{:caption "Python"
                                    :mode "ace/mode/python"}]
                                  [{:caption "Python"
                                    :mode "ace/mode/python"}
                                   {:caption "HTML"
                                    :mode "ace/mode/html"}])
           [{:caption "Python"
             :mode "ace/mode/python"}]))))

(deftest test-update-used-modes
  (testing "Used mode should be added to list when it not used before"
    (is (= (m/update-used-modes [{:caption "Python"
                                  :mode "ace/mode/python"}
                                 {:caption "HTML"
                                  :mode "ace/mode/html"}]
                                {:caption "JavaScript"
                                 :mode "ace/mode/javascript"})
           [{:caption "JavaScript"
             :mode "ace/mode/javascript"}
            {:caption "Python"
             :mode "ace/mode/python"}
            {:caption "HTML"
             :mode "ace/mode/html"}])))
  (testing "Used mode should be moved to first place in list if it used before"
    (is (= (m/update-used-modes [{:caption "Python"
                                  :mode "ace/mode/python"}
                                 {:caption "HTML"
                                  :mode "ace/mode/html"}]
                                {:caption "HTML"
                                 :mode "ace/mode/html"})
           [{:caption "HTML"
             :mode "ace/mode/html"}
            {:caption "Python"
             :mode "ace/mode/python"}])))
  (testing "Oldest used mode should be removed if count of modes > `used-modes-limit`"
    (with-redefs [m/used-modes-limit 1]
      (is (= (m/update-used-modes [{:caption "Python"
                                    :mode "ace/mode/python"}]
                                  {:caption "HTML"
                                   :mode "ace/mode/html"})
             [{:caption "HTML"
               :mode "ace/mode/html"}])))))

(deftest test-get-unused-modes
  (testing "Should return only unused modes"
    (is (= (m/get-unused-modes [{:caption "Python"
                                 :mode "ace/mode/python"}
                                {:caption "HTML"
                                 :mode "ace/mode/html"}]
                               [{:caption "HTML"
                                 :mode "ace/mode/html"}])
           [{:caption "Python"
             :mode "ace/mode/python"}]))))
