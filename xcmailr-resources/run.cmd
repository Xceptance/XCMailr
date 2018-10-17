@echo off
setlocal enabledelayedexpansion

:: setup paths and options
set CURR_DIR=%CD%
set JVM_OPTS=%JVM_OPTS% "-Dsun.lang.ClassLoader.allowArraySyntax=true "
set XCMAILR_OPTS=-Dxcmailr.xcmstarter.home=%CURR_DIR% -Dninja.external.configuration=conf/application.conf -Dninja.mode=prod %*
set LOGBK_OPTS="-Dlogback.configurationFile=conf/logback.xml"
set JETTY_OPTS="-Djetty.logging=$CURR_DIR/logs/jetty.log"
set XCMAILR_JAR="./xcmailr-jetty-starter.jar"

:: run with all options
java %JVM_OPT% %XCMAILR_OPTS% %LOGBK_OPTS% %JETTY_OPTS% -jar %XCMAILR_JAR%

