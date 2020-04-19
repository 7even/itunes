(ns itunes.core
  (:require [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clojure.data.xml :refer [parse-str]]
            [clojure.java.shell :refer [sh]])
  (:import [java.text SimpleDateFormat]))

(def path
  (let [relative-path "~/Music/iTunes/iTunes Music Library.xml"]
    (:out (sh "bash" "-c" (str "echo -n " relative-path)))))

(def data
  (-> path slurp parse-str))

(defn- transform-value [{tag :tag
                         [content] :content}]
  (let [date-format (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ssX")]
    (case tag
      :integer (Long/parseLong content)
      :string content
      :date (.parse date-format content)
      :true true)))

(defn- transform-track [track]
  (->> track
       :content
       (partition 2)
       (reduce (fn [m [k v]]
                 (assoc m
                        (-> k :content first ->kebab-case-keyword)
                        (transform-value v)))
               {})))

(def tracks
  (->> data
       :content
       first
       :content
       (drop-while #(not= (:content %) '("Tracks")))
       second
       :content
       (remove #(= (:tag %) :key))
       (map transform-track)))

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
