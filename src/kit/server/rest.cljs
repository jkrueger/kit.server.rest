(ns kit.server.rest
  (:refer-clojure :exclude (get))
  (:require
    [kit.app.component :as comp]
    [kit.app.log :as log]
    [kit.server.rest.log :as log])
  (:require-macros
    [kit.core :refer (? !)]))

(def restify (js/require "restify"))

(defn panic [res]
  (-> "Internal error while handling a request"
      (http/internal-error)
      (.send)))

(defn on-error [app f]
  (.on app "uncaughtException"
    f))

(defn log-and-panic [req res route err]
  (log/uncaught-error req res route err)
  (panic res))

(defn register-middleware [app opts]
  (doseq [mw (seq (? opts :mw :pre))]
    (.pre mw))
  (doseq [mw (seq (? opts :mw :post))]
    (.on "after" mw))
  (doseq [mw (seq (? opts :mw :route))]
    (.use mw)))

(defn- log-requests [app]
  (.use app (.requestLogger restify)))

(defrecord RestifyServer [app opts]
  (up [_ next]
    (try
      (on-error app log-and-panic)
      (register-middleware app opts)
      (.listen app (? opts :port) (? opts :address) next)
      (catch js/Error e
        (next e))))
  (down [_ next]
    (try
      (.close app)
      (next)
      (catch js/Error e
        (next e)))))

(defn make [opts]
  (let [opts (clj->js opts)]
    (RestifyServer.
      (.createServer restify opts)
      opts)))

(defn body-parser [& {:keys [max-size]}]
  (.bodyParser restify (js-obj "maxBodySize" max-size)))

(def query-parser (.queryParser restify))

(def log-request  log/request)
(def log-response log/response)

(defn server-static [server route directory]
  (.get (:app server) route
    (.serveStatic restify
      (js-obj "directory" directory))))

(defn error? [x]
  (instance? (? restify :RestError) x)
  (instance? (? restify :HttpError) x))

(defn- register-route [f route mws]
  (let [args (into-array (cons route mws))]
    (.apply f (:app server) args)))

(defn get [server route & mws]
  (register-route (? (:app server) :get) route mws))

(defn put [server route & mws]
  (register-route (? (:app server) :put) route mws))

(defn post [server route & mws]
  (register-route (? (:app server) :post) route mws))

(defn del [server route & mws]
  (register-route (? (:app server) :del) route mws))
