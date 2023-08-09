#!/bin/sh
set -e
_dir=`realpath -m $(dirname $0)`

if [ $# -lt 1 ]; then
 echo "Must specify at least one argument"
 exit 1
fi

if [ -z "$1" ]; then
 echo "Empty filename?"
 exit 1
fi

IMPORT_FILENAME=$1
IMPORT_SQL="runscript from '"$IMPORT_FILENAME"' from_1x;"

exec $_dir/shell.sh "$IMPORT_SQL"
