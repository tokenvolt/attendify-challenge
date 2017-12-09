(ns attendify-challenge.dribbble.utils)

(defn sort-map-by-values [target]
  (into (sorted-map-by (fn [key1 key2]
                         (compare [(target key2) key1]
                                  [(target key1) key2]))) target))

(defn current-time []
  (quot (System/currentTimeMillis) 1000))
