#!/bin/sh
set -x
rm -rf out && mkdir out
javac -d out -cp --source-path src/RePatch.java
javac -d out -cp --source-path src/CleanPaths.java
javac -d out -cp --source-path src/CommonBaseline.java
javac -d out -cp --source-path src/Webrev.java
