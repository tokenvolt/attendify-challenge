(ns attendify-challenge.dribbble.core-test
  (:require [clojure.test :refer :all]
            [attendify-challenge.dribbble.core :as core]))

(deftest dribbble-urls-test
  (is (= (core/followers-url 123 {:page 3}) "https://api.dribbble.com/v1/users/123/followers?page=3&access_token=152c50b87ccaf7faa540834d5033bbf4678d1b80350f946c3e35eca11ad2b0ee"))
  (is (= (core/shots-url 123 {:page 3}) "https://api.dribbble.com/v1/users/123/shots?page=3&access_token=152c50b87ccaf7faa540834d5033bbf4678d1b80350f946c3e35eca11ad2b0ee"))
  (is (= (core/likes-url 123 {:page 3}) "https://api.dribbble.com/v1/shots/123/likes?page=3&access_token=152c50b87ccaf7faa540834d5033bbf4678d1b80350f946c3e35eca11ad2b0ee")))
