(ns clj-bin.main
  (:require [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [clj-bin.bin :as bin])
  (:gen-class))

(defn usage [options-summary]
  (->> ["clj-bin creates binary executables from jar files by adding a preamble script."
        ""
        "Usage: clj-bin [options] --jar JAR --out BIN"
        ""
        "Options:"
        options-summary
        ""
        "See https://github.com/markus-wa/clj-bin for more information."]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(def cli-options
  [["-i" "--jar JAR" "Jar source file"]
   ["-o" "--out BIN" "Binary output destination"]
   ["-m" "--main" "Main class"]
   ["-p" "--project NAME"
    :default "app"]
   ["-v" "--version VERSION"
    :default "v0.0.1"]
   [nil "--bootclasspath CLASSPATH"
    :default false]
   [nil "--skip-realign"
    :default false]
   [nil "--jvm-opts OPTS"]
   [nil "--win-jvm-opts OPTS"]
   [nil "--custom-preamble STRING"]
   [nil "--custom-preamble-script PATH"]
   ["-h" "--help"]])

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options)]
    (cond
      (or (:help options)
          (not (:jar options))
          (not (:out options))) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (= 0 (count arguments))
      options
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [jar out exit-message ok?] :as opts}
        (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (bin/bin jar out opts))))
