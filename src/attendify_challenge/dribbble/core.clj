(ns attendify-challenge.dribbble.core
  (:require [clj-http.client :as client]
            [clojure.string :as string]
            [cheshire.core :as json]
            [attendify-challenge.dribbble.utils :as utils]
            [manifold.stream :as s]))

(def access-token "152c50b87ccaf7faa540834d5033bbf4678d1b80350f946c3e35eca11ad2b0ee")
(def host-v1 "https://api.dribbble.com/v1")

(def followers-stream (s/stream))
(def shots-stream (s/stream))
(def likes-stream (s/stream))
(def log-stream (s/stream))

(def api-calls-performed (atom 0))
(def remaining-calls (atom 60))
(def reset-at (atom 0))
(def done (atom false))
(def result (atom []))
(def likers (atom []))

(defn build-queryparams [queryparams]
  (string/join "&" (map (fn [[k v]] (str (name k) "=" v)) (merge queryparams {"access_token" access-token}))))

(defn followers-url [user-id queryparams]
  (str host-v1 "/" "users/" user-id "/" "followers" "?" (build-queryparams queryparams)))

(defn shots-url [user-id queryparams]
  (str host-v1 "/" "users/" user-id "/" "shots" "?" (build-queryparams queryparams)))

(defn likes-url [shot-id queryparams]
  (str host-v1 "/" "shots/" shot-id "/" "likes" "?" (build-queryparams queryparams)))


(defn pause-api-request! []
  (s/put! log-stream "Dribbble API limit exceeded.")
  (let [timeout-ms (* (- @reset-at (utils/current-time)) 1000)]
    (Thread/sleep timeout-ms)
    (reset! remaining-calls 60)
    (reset! reset-at 0)
    (s/put! log-stream "Resuming Dribbble API requests")))

(defn parse-response [response]
  (let [data (json/parse-string (:body response) true)
        next-uri (get-in response [:links :next :href])
        ratelimit-remaining (get-in response [:headers "X-RateLimit-Remaining"])
        ratelimit-reset (get-in response [:headers "X-RateLimit-Reset"])
        status (:status response)]
    {:data data
     :next-uri next-uri
     :status status
     :ratelimit-remaining ratelimit-remaining
     :ratelimit-reset ratelimit-reset}))

(defn update-ratelimit-info [ratelimit-remaining ratelimit-reset]
  (when (and ratelimit-remaining ratelimit-reset)
    (reset! remaining-calls (Integer. ratelimit-remaining))
    (reset! reset-at (Integer. ratelimit-reset))))

(defn update-api-stats! []
  (swap! api-calls-performed inc))

(defn perform-api-request! [uri]
  (when (<= @remaining-calls 0) (pause-api-request!))
  (let [response (parse-response (client/get uri))
        {:keys [data next-uri ratelimit-remaining ratelimit-reset status]} response]
    (update-api-stats!)
    (update-ratelimit-info ratelimit-remaining ratelimit-reset)
    (when (= status 200)
      {:data data :next-uri next-uri})))

(defn get-followers-async [user-id]
  (loop [page 1]
    (s/put! log-stream (str "Started fetching page #" page " of followers for user #" user-id))
    (let [response (perform-api-request! (followers-url user-id {:page page}))
          {:keys [data next-uri]} response]
      (doseq [follower data] (s/put! followers-stream (get-in follower [:follower :id])))
      (if next-uri
        (recur (inc page))
        (do
          (s/put! log-stream (str "Finished fetching followers for user #" user-id))
          (s/close! followers-stream))))))

(defn get-shots-async [user-id]
  (loop [page 1]
    (s/put! log-stream (str "Started fetching page #" page " of shots for user #" user-id))
    (let [response (perform-api-request! (shots-url user-id {:page page}))
          {:keys [data next-uri]} response]
      (doseq [shot data] (s/put! shots-stream (:id shot)))
      (if next-uri
        (recur (inc page))
        (s/put! log-stream (str "Finished fetching shots for user #" user-id))))))

(defn get-likes-async [shot-id]
  (loop [page 1]
    (s/put! log-stream (str "Started fetching page #" page " of likes for shot #" shot-id))
    (let [response (perform-api-request! (likes-url shot-id {:page page}))
          {:keys [data next-uri]} response]
      (doseq [like data] (s/put! likes-stream (get-in like [:user :username])))
      (if next-uri
        (recur (inc page))
        (do
          (s/put! log-stream (str "Finished fetching likes for shot #" shot-id)))))))

(defn calculate-top-likers [likers qnt]
  (take qnt (utils/sort-map-by-values (frequencies likers))))

(defn accumulate-liker [liker]
  (swap! likers (fn [v] (conj v liker)))
  (reset! result (calculate-top-likers @likers 10)))

(defn consume-followers []
  (s/consume get-shots-async followers-stream))

(defn consume-shots []
  (s/consume get-likes-async shots-stream))

(defn consume-likes []
  (s/consume accumulate-liker likes-stream))

(defn logger []
  (s/consume println log-stream))

(defn print-result []
  (println @result))

(defn -main [username]
  (logger)
  (consume-followers)
  (consume-shots)
  (consume-likes)
  (get-followers-async username)
  (s/on-closed followers-stream print-result))
