(ns attendify-challenge.url-matcher.core
  (:require [clojure.string :as string]))

(def url-matcher #"\A(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)?(\?([^#]*))?(#(.*))?\z")
(def pattern-matcher #"(host|path|queryparam)\(([^)]+)\)")
(def params-matcher #"\&(\w+)")
(def var-matcher #"\?(\w+)")

(defn parse-path [path] (filter not-empty (string/split path #"/")))
(defn parse-queryparam [q] (string/split q #"="))
(defn parse-queryparams [qs] (map parse-queryparam (string/split qs #"&")))

(defn extract-match
  [[_ k v]]
  (hash-map (keyword k) v))

(defn new-pattern
  [pattern-str]
  (let [matches (re-seq pattern-matcher pattern-str)]
    (apply merge-with (comp sort vector) (map extract-match matches))))

(defn extract-segment [a b]
  (if-let [[_ k] (re-matches var-matcher a)]
    (vector (keyword k) b)
    (when (= a b) (vector :path/segment a))))

(defn extract-queryparam [[k1 a] [k2 b]]
  (when
    (= k1 k2)
    (extract-segment a b)))

(defn recognize [pattern url]
  (let [[_ _ _ _ url-host url-path _ url-queryparam] (re-matches url-matcher url)
        {:keys [host path queryparam]} pattern]
    (if
      (= url-host host)
      (let [parsed-url-path (parse-path url-path)
            parsed-pattern-path (parse-path path)
            parsed-url-queryparams (parse-queryparams url-queryparam)
            parsed-pattern-queryparams (map parse-queryparam queryparam)]
        (if
          (and
            (= (count parsed-url-path) (count parsed-pattern-path))
            (= (count parsed-url-queryparams) (count parsed-pattern-queryparams)))
          (let [path-segments (map extract-segment parsed-pattern-path parsed-url-path)
                queryparams-segments (map extract-queryparam parsed-pattern-queryparams parsed-url-queryparams)
                segments (concat path-segments queryparams-segments)]
            (when-not
              (some nil? segments)
              (into [] (filter (fn [[k _]] (not= :path/segment k)) segments)))))))))
