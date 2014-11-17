(ns textarea-to-code-editor.core
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [selmer.parser :refer [render-file]]))

(defn list-resources
  "Returns list with resources from paths."
  [paths]
  (->> (for [script paths
             :let [file (io/file (io/resource script))]]
         (if (.isDirectory file)
           (map #(string/replace (.getPath %) #"^.*resources/" "")
                (.listFiles file))
           [script]))
       flatten
       sort
       (filter #(-> % io/resource io/file .isDirectory not))))

(defn -main
  [& _]
  (let [project-data (read-string (slurp "project.clj"))
        [_ project-name project-version & _] project-data
        project-name (string/replace (str project-name) #"-" " ")
        project-map (apply hash-map (drop 3 project-data))
        content-scripts (list-resources (:content-scripts project-map))
        background-scripts (list-resources (:background-scripts project-map))]
    (with-open [wrtr (io/writer "resources/manifest.json")]
      (.write wrtr (render-file "manifest_template.json"
                                {:name project-name
                                 :version project-version
                                 :description (:description project-map)
                                 :content-scripts content-scripts
                                 :background-scripts background-scripts}))))
  (println "Manifest updated!"))
