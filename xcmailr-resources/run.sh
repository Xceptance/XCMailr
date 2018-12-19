#!/bin/bash
set -e

KEYSTORE_FILE=${KEYSTORE_FILE:-"conf/keystore.jks"}
KEYSTORE_PW=${KEYSTORE_PW:-"topsecret"}
XCM_INSTALL_DIR=${XCM_INSTALL_DIR:-`realpath -m $(dirname $0)`}

#setup paths and options
JVM_OPTS="-Dsun.lang.ClassLoader.allowArraySyntax=true"
JVM_OPTS="$JVM_OPTS -Djavax.net.ssl.keyStore=$KEYSTORE_FILE"
JVM_OPTS="$JVM_OPTS -Djavax.net.ssl.keyStorePassword=$KEYSTORE_PW"
XCMAILR_OPTS="-Dh2.maxCompactTime=15000 -Dxcmailr.xcmstarter.home=$XCM_INSTALL_DIR -Dninja.external.configuration=conf/application.conf -Dninja.mode=prod $@"
LOGBK_OPTS="-Dlogback.configurationFile=conf/logback.xml"
JETTY_OPTS="-Djetty.logging=$XCM_INSTALL_DIR/logs/jetty.log"
XCMAILR_JAR="xcmailr-jetty-starter.jar"

# run with all options
cd $XCM_INSTALL_DIR
exec java $JVM_OPTS $XCMAILR_OPTS $LOGBK_OPTS $JETTY_OPTS -jar $XCMAILR_JAR
