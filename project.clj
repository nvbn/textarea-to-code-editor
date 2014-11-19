(defproject textarea-to-code-editor "0.3"
            :description "Chrome extension for converting textarea to code editor"
            :url "https://github.com/nvbn/textarea-to-code-editor"
            :license {:name "Eclipse Public License"
                      :url "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.6.0"]
                           [org.clojure/clojurescript "0.0-2371"]
                           [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                           [com.cemerick/clojurescript.test "0.3.1"]
                           [domina "1.0.3"]
                           [clj-di "0.5.0"]
                           [selmer "0.7.3"]
                           [alandipert/storage-atom "1.2.3"]]
            :plugins [[lein-cljsbuild "1.0.3"]
                      [com.cemerick/clojurescript.test "0.3.1"]
                      [lein-bower "0.5.1"]]
            :bower-dependencies [[ace-builds "~1.1.8"]]
            :bower {:directory "resources/components/"}
            :jvm-opts ["-Xss16m"]
            :content-scripts ["content/main.js"
                              "components/ace-builds/src/"
                              "components/ace-builds/src/snippets/"]
            :background-scripts ["background/main.js"]
            :main textarea-to-code-editor.core
            :cljsbuild {:builds {:background {:source-paths ["src/textarea_to_code_editor/background/"
                                                             "src/textarea_to_code_editor/chrome"]
                                              :compiler {:output-to "resources/background/main.js"
                                                         :output-dir "resources/background/"
                                                         :source-map "resources/background/main.js.map"
                                                         :optimizations :whitespace
                                                         :pretty-print true}}
                                 :content {:source-paths ["src/textarea_to_code_editor/content/"
                                                          "src/textarea_to_code_editor/chrome"]
                                           :compiler {:output-to "resources/content/main.js"
                                                      :output-dir "resources/content/"
                                                      :source-map "resources/content/main.js.map"
                                                      :optimizations :whitespace
                                                      :pretty-print true}}
                                 :test {:source-paths ["src/" "test/"]
                                        :compiler {:output-to "target/cljs-test.js"
                                                   :optimizations :whitespace
                                                   :pretty-print false}}}
                        :test-commands {"test" ["phantomjs" :runner
                                                "resources/components/ace-builds/src/ace.js"
                                                "resources/components/ace-builds/src/mode-clojure.js"
                                                "resources/components/ace-builds/src/theme-monokai.js"
                                                "resources/components/ace-builds/src/ext-modelist.js"
                                                "target/cljs-test.js"]}})
