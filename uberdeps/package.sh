#!/bin/bash -e
cd "$( dirname "${BASH_SOURCE[0]}" )/.."
mkdir -p classes
clj -M -e "(compile 'clj-bin.main)"

cd "$( dirname "${BASH_SOURCE[0]}" )"
clojure -M -m uberdeps.uberjar --deps-file ../deps.edn --target ../target/clj-bin.jar --main-class clj_bin.main

java -jar ../target/clj-bin.jar --project clj-bin -i ../target/clj-bin.jar -o ../target/clj-bin
