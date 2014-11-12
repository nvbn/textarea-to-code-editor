(defproject textarea-to-code-editor "0.1.0-SNAPSHOT"
            :description "FIXME: write description"
            :url "http://example.com/FIXME"
            :license {:name "Eclipse Public License"
                      :url "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.6.0"]
                           [org.clojure/clojurescript "0.0-2371"]
                           [com.cemerick/clojurescript.test "0.3.1"]
                           [clj-di "0.4.0"]]
            :plugins [[lein-cljsbuild "1.0.3"]
                      [com.cemerick/clojurescript.test "0.3.1"]]
            :cljsbuild {:builds [{:source-paths ["src/textarea_to_code_editor/background/"
                                                 "src/textarea_to_code_editor/chrome"]
                                  :compiler {:output-to "resources/background.js"
                                             :optimizations :whitespace
                                             :pretty-print false}}
                                 {:source-paths ["src/textarea_to_code_editor/content/"
                                                 "src/textarea_to_code_editor/chrome"]
                                  :compiler {:output-to "resources/content.js"
                                             :optimizations :whitespace
                                             :pretty-print false}}
                                 {:source-paths ["src/" "test/"]
                                  :compiler {:output-to "target/cljs-test.js"
                                             :optimizations :whitespace
                                             :pretty-print false}}]
                        :test-commands {"test" ["phantomjs" :runner
                                                "target/cljs-test.js"]}})
