#!/bin/sh
set -e

_dir=`realpath -m $(dirname "$0")`

exec $_dir/import.sh "$1"
