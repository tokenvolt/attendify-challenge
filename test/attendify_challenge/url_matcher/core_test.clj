(ns attendify-challenge.url-matcher.core-test
  (:require [clojure.test :refer :all]
            [attendify-challenge.url-matcher.core :refer :all]))

(deftest new-pattern-success
  (let [pattern "host(dribbble.com); path(?user/shots/?id); queryparam(offset=?offset); queryparam(list=?type);"
        result {:host "dribbble.com" :path "?user/shots/?id" :queryparam ["list=?type" "offset=?offset"]}]
    (is (= (new-pattern pattern) result))))

(deftest recognize-success
  (let [pattern (new-pattern "host(dribbble.com); path(?user/shots/?id); queryparam(offset=?offset); queryparam(list=?type);")
        url "https://dribbble.com/bob/shots/1905065-Travel-Icons-pack?list=users&offset=1"]
    (is
      (= (recognize pattern url) [[:user "bob"] [:id "1905065-Travel-Icons-pack"] [:type "users"] [:offset "1"]]))))

(deftest host-mismatch
  (let [pattern (new-pattern "host(twitter.com); path(?user/shots/?id); queryparam(offset=?offset); queryparam(list=?type);")
        url "https://dribbble.com/bob/shots/1905065-Travel-Icons-pack?list=users&offset=1"]
    (is
      (= (recognize pattern url) nil))))

(deftest path-mismatch
  (let [pattern (new-pattern "host(dribbble.com); path(?user/likes/?id); queryparam(offset=?offset); queryparam(list=?type);")
        url "https://dribbble.com/bob/shots/1905065-Travel-Icons-pack?list=users&offset=1"]
    (is
      (= (recognize pattern url) nil))))

(deftest path-segment-missing
  (let [pattern (new-pattern "host(dribbble.com); path(?user/likes/?id); queryparam(offset=?offset); queryparam(list=?type);")
        url "https://dribbble.com/shots/1905065-Travel-Icons-pack?list=users&offset=1"]
    (is
      (= (recognize pattern url) nil))))

(deftest queryparam-missing
  (let [pattern (new-pattern "host(dribbble.com); path(?user/shots/?id); queryparam(offset=?offset); queryparam(list=?type);")
        url "https://dribbble.com/bob/shots/1905065-Travel-Icons-pack?list=users"]
    (is
      (= (recognize pattern url) nil))))
