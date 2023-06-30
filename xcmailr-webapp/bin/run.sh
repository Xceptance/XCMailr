#!/bin/bash
set -e

INSTALL_DIR=${INSTALL_DIR:-`realpath -m $(dirname $0)/..`}

# setup Java class path
CLASSPATH=$INSTALL_DIR/lib/\*

# setup paths and options
JAVA_OPTS="-Dh2.maxCompactTime=15000  -Dninja.external.configuration=conf/application.conf  -Dninja.mode=prod"
JAVA_OPTS="$JAVA_OPTS -Dlogback.configurationFile=file://$INSTALL_DIR/conf/logback.xml"

# append options to suppress illegal access warnings for Java 9+
PACKAGES="java.base/java.lang java.base/java.lang.reflect java.base/java.text java.base/java.util"
for p in $PACKAGES; do JAVA_OPTS="$JAVA_OPTS --add-opens=$p=ALL-UNNAMED"; done
#JAVA_OPTS="$JAVA_OPTS --illegal-access=debug"

# run with all options
exec java $JAVA_OPTS -cp "$CLASSPATH" "$@" main.Main 
