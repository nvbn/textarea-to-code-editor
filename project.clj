(defproject textarea-to-code-editor "0.4"
            :description "Chrome extension for converting textarea to code editor"
            :url "https://github.com/nvbn/textarea-to-code-editor"
            :license {:name "Eclipse Public License"
                      :url "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.8.0"]
                           [org.clojure/clojurescript "1.7.228"]
                           [org.clojure/core.match "0.3.0-alpha4"]
                           [org.clojure/core.async "0.2.371"]
                           [com.cognitect/transit-cljs "0.8.237"]
                           [com.cemerick/clojurescript.test "0.3.3"]
                           [domina "1.0.3"]
                           [clj-di "0.5.0"]
                           [selmer "0.8.0"]
                           [alandipert/storage-atom "1.2.4"]
                           [org.clojure/tools.macro "0.1.2"]]
            :plugins [[lein-cljsbuild "1.1.3"]
                      [com.cemerick/clojurescript.test "0.3.3"]
                      [lein-bower "0.5.1"]]
            :bower-dependencies [[ace-builds "~1.2.3"]]
            :bower {:directory "resources/components/"}
            :jvm-opts ["-Xss16m"]
            :content-scripts ["content/main.js"
                              "components/ace-builds/src/"
                              "components/ace-builds/src/snippets/"]
            :background-scripts ["background/main.js"]
            :main textarea-to-code-editor.core
            :cljsbuild {:builds {:background {:source-paths ["src/textarea_to_code_editor/background/"]
                                              :compiler {:output-to "resources/background/main.js"
                                                         :output-dir "resources/background/"
                                                         :source-map "resources/background/main.js.map"
                                                         :optimizations :whitespace
                                                         :pretty-print true}}
                                 :content {:source-paths ["src/textarea_to_code_editor/content/"]
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
                                                "resources/components/ace-builds/src/mode-python.js"
                                                "resources/components/ace-builds/src/theme-monokai.js"
                                                "resources/components/ace-builds/src/ext-modelist.js"
                                                "target/cljs-test.js"]}})
