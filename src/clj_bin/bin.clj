(ns clj-bin.bin
  "Create a standalone executable for your project."
  (:require [clojure.java.io :as io]
            [clostache.parser :refer [render]]
            [me.raynes.fs :as fs]
            [clojure.string :as str]
            [clj-zip-meta.core :refer [repair-zip-with-preamble-bytes]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                      ---==| T E M P L A T E S |==----                      ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def BOOTCLASSPATH-TEMPLATE
  ":;exec java {{{jvm-opts}}} -D{{{project}}}.version={{{version}}} -Xbootclasspath/a:$0 {{{main}}} \"$@\"\n@echo off\r\njava {{{win-jvm-opts}}} -D{{{project}}}.version={{{version}}} -Xbootclasspath/a:\"%~f0\" {{{main}}} %*\r\ngoto :eof\r\n")

(def NORMAL-TEMPLATE
  ":;exec java {{{jvm-opts}}} -D{{{project}}}.version={{{version}}} -jar $0 \"$@\"\n@echo off\r\njava {{{win-jvm-opts}}} -D{{{project}}}.version={{{version}}} -jar \"%~f0\" %*\r\ngoto :eof\r\n")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                       ---==| P R E A M B L E |==----                       ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- preamble-template
  [{:keys [bootclasspath custom-preamble custom-preamble-script]}]
  (cond
    custom-preamble-script (slurp custom-preamble-script)
    custom-preamble (str custom-preamble "\r\n")
    bootclasspath BOOTCLASSPATH-TEMPLATE
    :else NORMAL-TEMPLATE))

(defn- render-preamble
  [template opts]
  (-> (render template opts)
      (str/replace #"\\\$" "\\$")))

(defn- sanitize-jvm-opts-for-win
  "turns linux style vars \"$FOO\" into win style \"%FOO\"."
  [opts]
  (str/replace opts #"\$([a-zA-Z0-9_]+)" "%$1%"))

(defn- preamble
  [opts]
  (-> (preamble-template opts)
      (render-preamble opts)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                         ---==| B I N A R Y |==----                         ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- write-preamble [out ^String preamble]
  (.write out (.getBytes preamble)))

(defn- writing-bin [binfile uberjar preamble]
  (println "Creating standalone executable:" (str binfile))
  (io/make-parents binfile)
  (with-open [bin (io/output-stream binfile)]
    (write-preamble bin preamble)
    (io/copy (fs/file uberjar) bin))
  (fs/chmod "+x" binfile))

(defn bin
  "Create a standalone console executable from a jar file."
  [jarpath binpath opts]
  (let [binfile (fs/file binpath)
        opts (-> opts
                 (update :win-jvm-opts #(when % (sanitize-jvm-opts-for-win %))))]
    (writing-bin binfile jarpath (preamble opts))
    (when-not (:skip-realign opts)
      (println "Re-aligning zip offsets")
      (repair-zip-with-preamble-bytes binfile))))
