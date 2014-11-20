(ns textarea-to-code-editor.background.modes)

(def used-modes-limit 5)

(def default-used-modes
  [{:caption "Python"
    :mode "ace/mode/python"}
   {:caption "SH"
    :mode "ace/mode/sh"}
   {:caption "JavaScript"
    :mode "ace/mode/javascript"}
   {:caption "HTML"
    :mode "ace/mode/html"}
   {:caption "Markdown"
    :mode "ace/mode/markdown"}])

(defn sanitize-used-modes
  "Returns list of valid used modes."
  [modes used-modes]
  (filter (set modes) used-modes))

(defn update-used-modes
  "Update list of used modes."
  [used-modes mode]
  (->> used-modes
       (remove #(= mode %))
       (into [mode])
       (take used-modes-limit)))

(defn get-unused-modes
  "Returns list with ordered unused modes."
  [modes used-modes]
  (->> modes
       (filter (complement (set used-modes)))
       (sort-by :caption)))
