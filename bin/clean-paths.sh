#!/bin/sh
set -x
BASE="$(dirname $(readlink -f ${BASH_SOURCE[0]}))"
java -cp $BASE/../out CleanPaths $@
