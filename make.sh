#!/bin/sh
set -x
rm -rf out && mkdir out
javac -d out -cp --source-path src/RePatch.java
