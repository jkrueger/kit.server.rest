(ns kit.server.http.response
  (:require [kit.algo.gen :as gen]))

(def restify (js/require "restify"))

(defn send [res status msg]
  (.send res status msg))

(defn ok
  ([res msg]
     (.send res msg))
  ([res msg next]
     (ok res msg)
     (next)))

(defn created  ([res type id]
     (let [location (str "/" (name type) "/" id)]
       (.set res "location" location)
       (.send res 303 (js-obj "id" id "location" location))))
  ([res type id next]
     (created res type id)
     (next)))

(defn deleted
  ([res id]
     (.send res 200 (js-obj "id" id)))
  ([res id next]
     (deleted res id)
     (next)))

(defn error [clazz msg]
  (clazz. msg))

(defn conflict [msg]
  (error (aget restify "ConflictError") msg))

(defn bad-request [msg]
  (error (aget restify "BadRequestError") msg))

(defn media-type-not-supported [media-type]
  (let [msg (str "media type should be: '" media-type "'")]
    (error (aget restify "MediaTypeNotSupportedError") msg)))

(defn validation-error []
  (bad-request "Validation error"))

(defn no-body-error []
  (bad-request "No body"))

(defn not-found []
  (error (aget restify "NotFoundError") "Entity not found"))

(defn not-authorized [msg]
  (error (aget restify "UnauthorizedError") msg))

(defn forbidden [msg]
  (error (aget restify "ForbiddenError") msg))

(defn missing-credentials [res methods]
  (let [challenges (gen/commas methods)]
    (.header res "WWW-Authenticate" challenges))
  (not-authorized "Authentication required"))

(defn internal-error [msg]
  (error (aget restify "InternalError") msg))

(defn not-available [msg]
  (error (aget restify "ServiceUnavailableError") msg))

(defn content-type [res media-type]
  (.set "content-type" media-type)
  res)
