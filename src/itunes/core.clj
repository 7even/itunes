(ns itunes.core
  (:require [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clojure.data.xml :refer [parse-str]]
            [clojure.java.shell :refer [sh]]
            [table.core :refer [table]])
  (:import [java.text SimpleDateFormat]))

(def relative-path
  (or (System/getenv "XML_PATH")
      "~/Music/iTunes/iTunes Music Library.xml"))

(def path
  (:out (sh "bash" "-c" (str "echo -n " relative-path))))

(def data
  (-> path slurp parse-str))

(defn- get-content [node]
  (->> (:content node)
       (filter map?)))

(defn- transform-value [{tag :tag
                         [content] :content}]
  (let [date-format (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ssX")]
    (case tag
      :integer (Long/parseLong content)
      :string content
      :date (.parse date-format content)
      :true true
      :false false)))

(defn- transform-track [track]
  (->> track
       get-content
       (partition 2)
       (reduce (fn [m [k v]]
                 (assoc m
                        (-> k :content first ->kebab-case-keyword)
                        (transform-value v)))
               {})))

(def tracks
  (->> data
       get-content
       first
       get-content
       (drop-while #(not= (:content %) '("Tracks")))
       second
       get-content
       (remove #(= (:tag %) :key))
       (map transform-track)))

(defn -main []
  (println "Top 50 tracks")
  (table (->> tracks
              (sort-by :play-count)
              reverse
              (map #(select-keys % [:name :album :play-count]))
              (take 50))
         :style :unicode-3d)
  (System/exit 0))

(comment
  ;; top 50 tracks
  (->> tracks
       (sort-by :play-count)
       reverse
       (map (juxt :name :play-count))
       (take 50))
  ;; kinds of tracks
  (->> tracks
       (map :kind)
       frequencies))
