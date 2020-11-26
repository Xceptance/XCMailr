#!/bin/sh
set -e

_dir=$(dirname "$0")

if [ $# -lt 1 ]; then
 echo "Must specify at least one argument"
 exit 1
fi

if [ -z "$1" ]; then
 echo "Empty filename?"
 exit 1
fi

EXPORT_FILENAME=$1
EXPORT_SQL="script nosettings drop to '"$EXPORT_FILENAME"';"

sh $_dir/shell.sh "$EXPORT_SQL"
