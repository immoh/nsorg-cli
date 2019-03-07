(ns nsorg.cli.terminal
  (:require [clojure.string]))

(defn default-info-fn [& msg]
  (apply println msg))

(def ^:dynamic *info-fn* default-info-fn)

(defn info [& msg]
  (apply *info-fn* msg))

(defn default-error-fn [& msg]
  (binding [*out* *err*]
    (apply println msg)))

(def ^:dynamic *error-fn* default-error-fn)

(defn error [& msg]
  (apply *error-fn* msg))

(defn prompt! [msg]
  (locking *out*
    (loop []
      (print (str msg " [y/N] "))
      (flush)
      (case (clojure.string/lower-case (read-line))
        ("y" "yes") true
        ("" "n" "no") false
        (recur)))))
