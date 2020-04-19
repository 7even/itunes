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

(defn- transform-value [{:keys [tag content]}]
  (let [date-format (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ssX")]
    (case tag
      :integer (Long/parseLong (first content))
      :string (first content)
      :date (.parse date-format (first content))
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
