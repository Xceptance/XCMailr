#!/bin/sh
set -e

_dir=`realpath -m $(dirname $0)/..`

conf_val() {
  local raw_val=`cat $_dir/conf/application.conf | egrep "^ebean.datasource.$1"`
  echo "${raw_val#*=}" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//'
}

[ -r $_dir/conf/application.conf ] || {
  echo "Application configuration file not found"
  exit 1
}

DB_URL=`conf_val databaseUrl`
DB_USER=`conf_val username`
DB_PASS=`conf_val password`

if [ -z "$DB_URL" ]; then
  echo "DB url not found or is empty. Please check your configuration."
  exit 1
fi

if [ -z "$DB_USER" ]; then
  echo "DB username not found or is empty. Please check your configuration."
  exit 1
fi

ARGS="-url $DB_URL -user $DB_USER"
if [ -n "$DB_PASS" ]; then
  ARGS="$ARGS -pass $DB_PASS"
fi

if [ $# -gt 0 ]; then
  java -cp $_dir/lib/h2*.jar org.h2.tools.Shell $ARGS 
else
  java -cp $_dir/lib/h2*.jar org.h2.tools.Shell $ARGS -sql "$1"
fi
