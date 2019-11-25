(ns nsorg.cli-tests
  (:require [nsorg.cli :as nsorg]
            [midje.sweet :refer :all]))

(tabular
  (fact
    "Summary contains counts of checked files, problematic files and fixed files"
    (nsorg/summarize {:replace ?replace :interactive ?interactive}
                     {:files ?files :errors ?errors :problems ?problems :replaces ?replaces})
    => ?summary)
  ?replace ?interactive ?files ?errors ?problems ?replaces ?summary
  false    false        6      0       0         0         "Checked 6 files, all good!"
  false    false        6      2       0         0         "Checked 6 files, failed to check 2 files"
  false    false        6      0       3         0         "Checked 6 files, found problems in 3 files"
  false    false        6      2       3         0         "Checked 6 files, failed to check 2 files, found problems in 3 files"
  true     false        6      0       3         3         "Checked 6 files, fixed 3 files"
  true     false        6      2       3         3         "Checked 6 files, failed to check 2 files, fixed 3 files"
  true     true         6      0       3         2         "Checked 6 files, found problems in 3 files, fixed 2 files"
  true     true         6      2       3         2         "Checked 6 files, failed to check 2 files, found problems in 3 files, fixed 2 files" )

(tabular
  (fact
    "Check is considered successful if there are no errors, and no problems if we're not fixing them"
    (nsorg/success? {:replace ?replace}
                     {:errors ?errors :problems ?problems})
    => ?success)
  ?replace ?errors ?problems ?success
  false    0       0         true
  false    2       0         false
  false    0       3         false
  false    2       3         false
  true     0       3         true
  true     2       3         false
  true     2       3         false)

(tabular
  (fact
    "Paths are based on arguments and project source and test paths. Fallback is current working directory"
    (nsorg/get-paths ?arguments ?default-paths) => ?paths)
  ?arguments    ?default-paths         ?paths
  ["dev" "src"] ["foo/src" "foo/test"] ["dev" "src"]
  ["dev" "src"] nil                    ["dev" "src"]
  []            ["foo/src" "foo/test"] ["foo/src" "foo/test"]
  []            nil                    ["./"])

(defn file-matcher [expected-file-path]
  (fn [^java.io.File file]
    (= expected-file-path (.getPath file))))

(fact
  "Finds Clojure files recursively. Ignore files in ignored-paths."
  (nsorg/find-clojure-files ["test_files"] ["test_files/foo/bar.cljc"]) => (just [(file-matcher "test_files/core.clj")
                                                                                  (file-matcher "test_files/core.cljc")
                                                                                  (file-matcher "test_files/core.cljs")]))
