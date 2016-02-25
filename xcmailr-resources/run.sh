#!/bin/sh

#setup paths and options
CURR_DIR=`pwd`
JVM_OPTS="-Dsun.lang.ClassLoader.allowArraySyntax=true "
XCMAILR_OPTS="-Dxcmailr.xcmstarter.home=$CURR_DIR -Dninja.external.configuration=conf/application.conf -Dninja.mode=prod $@"
LOGBK_OPTS="-Dlogback.configurationFile=conf/logback.xml"
JETTY_OPTS="-Djetty.logging=$CURR_DIR/logs/jetty.log"
XCMAILR_JAR="./xcmailr-jetty-starter.jar"

# run with all options
java $JVM_OPTS $XCMAILR_OPTS $LOGBK_OPTS $JETTY_OPTS -jar $XCMAILR_JAR

