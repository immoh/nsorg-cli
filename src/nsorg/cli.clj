(ns nsorg.cli
  (:require [clojure.java.io :as io]
            [clojure.stacktrace]
            [clojure.tools.cli :as cli]
            [nsorg.cli.diff :as diff]
            [nsorg.cli.terminal :as terminal]
            [nsorg.core :as nsorg])
  (:import (java.io File)
           (java.nio.file Paths)))

(defn clojure-file? [^File file]
  (and (.isFile file)
       (re-matches #".+\.clj.?" (.getName file))))

(defn excluded-file? [excluded-paths ^File file]
  (if-not ((set excluded-paths) file)
    file))

(defn paths->file-seq [paths]
  (->> paths
       (map io/file)
       (mapcat file-seq)))

(defn find-clojure-files [paths excluded-paths]
  (->> paths
       (paths->file-seq)
       (filter clojure-file?)
       (filter (partial excluded-file? (paths->file-seq excluded-paths)))
       (sort-by (memfn getAbsolutePath))))

(defn ->absolute-path [s]
  (.toAbsolutePath (Paths/get s (into-array String []))))

(defn relativize-path [path]
  (str (.relativize (->absolute-path "") (->absolute-path path))))

(defn summarize [{:keys [replace interactive]} {:keys [files errors problems replaces]}]
  (clojure.string/join ", " (keep identity
                                  [(format "Checked %s files" files)
                                   (when (and (zero? errors) (zero? problems))
                                     "all good!")
                                   (when (pos? errors)
                                     (format "failed to check %s files" errors))
                                   (when (and (pos? problems)
                                              (or (not replace) interactive))
                                     (format "found problems in %s files" problems))
                                   (when (pos? replaces)
                                     (format "fixed %s files" replaces))])))

(defn organize-ns-form! [file replace? interactive?]
  (let [path (relativize-path (.getAbsolutePath file))]
    (try
      (let [original-source (slurp file)
            modified-source (nsorg/rewrite-ns-form original-source)
            diff-chunks (diff/diff-chunks original-source modified-source)
            problem? (seq diff-chunks)]
        (when problem?
          (terminal/info (format "in %s:" path))
          (terminal/info (diff/format-diff diff-chunks))
          (terminal/info))
        (let [replaced? (when (and problem?
                                   replace?
                                   (or (not interactive?)
                                       (terminal/prompt! "Replace?")))
                          (spit file modified-source)
                          true)]
          {:files    1
           :problems (if problem? 1 0)
           :replaces (if replaced? 1 0)}))
      (catch Throwable t
        (terminal/error (format "Failed to check path %s:" path))
        (terminal/error (with-out-str (clojure.stacktrace/print-stack-trace t)))
        {:errors 1}))))

(defn organize-ns-forms! [paths excluded-paths options]
  (reduce
    (fn [result file]
      (merge-with + result (organize-ns-form! file (:replace options) (:interactive options))))
    {:errors 0 :files 0 :problems 0 :replaces 0}
    (find-clojure-files paths excluded-paths)))

(defn get-paths [arguments default-paths]
  (or (seq arguments)
      (seq default-paths)
      ["./"]))

(defn check
  "Leiningen plugin for organizing ns forms in source files.

Usage: lein nsorg [OPTIONS] [PATHS]

Clojure files are searched recursively from given paths. If no paths are given
and Leiningen is run inside project, project source and test paths are used.
Otherwise current workign directory is used.

Options:
  -e, --replace       Apply organizing suggestions to source files.
  -i, --interactive   Ask before applying suggestion (requires --replace).
  -x, --exclude PATH  Path to exclude from analysis."
  ([args]
    (check args nil))
  ([args {:keys [default-paths]}]
   (let [{:keys [options arguments]} (cli/parse-opts args [["-e" "--replace"]
                                                           ["-i" "--interactive"]
                                                           ["-x" "--exclude PATH"
                                                            :default []
                                                            :assoc-fn #(update %1 %2 conj %3)]])
         paths (map relativize-path (get-paths arguments default-paths))
         excluded-paths (map relativize-path (:exclude options))]
     (terminal/info "Checking following paths:")
     (doseq [path (sort paths)]
       (terminal/info path))
     (terminal/info)
     (when (seq excluded-paths)
       (terminal/info "Ignoring following paths:")
       (doseq [ignored-path (sort excluded-paths)]
         (terminal/info ignored-path))
       (terminal/info))
     (let [result (organize-ns-forms! paths excluded-paths options)]
       {:success? (or (pos? (:errors result))
                      (and (pos? (:problems result))
                           (not (:replace options))))
        :summary  (summarize options result)}))))

(defn -main [& args]
  (let [{:keys [success? summary]} (check args)]
    (shutdown-agents)
    (if success?
      (terminal/info summary)
      (do
        (terminal/error summary)
        (System/exit 1)))))
