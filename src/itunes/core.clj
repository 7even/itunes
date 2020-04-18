(ns itunes.core
  (:require [clojure.data.xml :refer [parse-str]]
            [clojure.java.shell :refer [sh]]))

(def path
  (let [relative-path "~/Music/iTunes/iTunes Music Library.xml"]
    (:out (sh "bash" "-c" (str "echo -n " relative-path)))))

(def data
  (-> path slurp parse-str))

(def tracks
  (->> data
       :content
       first
       :content
       (drop-while #(not= (:content %) '("Tracks")))
       second
       :content
       (remove #(= (:tag %) :key))))
