(ns kit.server.http.log
  (:require
    [common.log :as log])
  (:require-macros
    [common.macros :refer (? !)]))

(defn request [req res next]
  (log/info (? req :log)
    "Serving HTTP request"
    {:req req})
  (next))

(defn response [req res route err]
  (if err
    (cond
      (? err :statusCode)
        (log/warn (? req :log)
          (? err :message)
          {:status (? err :statusCode)})
      :else
        (log/warn (? req :log)
          "Error while processing API request"
          {:err err}))
    ;; log successful request
    (log/info (? req :log)
      "HTTP request completed"
      {:res res})))

(defn uncaught-error [req res route err]
  (cond
   (instance? js/Error err)
     (log/error (? req :log)
       "Uncaught exception"
       {:err err :req req :res res})
   (instance? ExceptionInfo err)
     (log/error (? req :log)
       "Uncaught exception"
       {:err  err
        :code (:error (ex-data err))
        :req  req
        :res  res})
   :else
     (log/error (? req :log)
       "Unknown error while handing API request"
       {:junk err
        :req  req
        :res  res})))
