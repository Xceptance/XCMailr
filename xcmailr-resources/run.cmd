@echo off
setlocal enabledelayedexpansion

if not defined KEYSTORE_FILE set KEYSTORE_FILE="conf/keystore.jks"
if not defined KEYSTORE_PW set KEYSTORE_PW=topsecret

:: setup paths and options
if not defined XCM_INSTALL_DIR for %%i in ("%~dp0..") do set XCM_INSTALL_DIR=%%~fi

set JVM_OPTS=%JVM_OPTS% -Dsun.lang.ClassLoader.allowArraySyntax=true
set JVM_OPTS=%JVM_OPTS% -Djavax.net.ssl.keyStore=%KEYSTORE_FILE%
set JVM_OPTS=%JVM_OPTS% -Djavax.net.ssl.keyStorePassword=%KEYSTORE_PW%
set XCMAILR_OPTS=-Dh2.maxCompactTime=150000 -Dxcmailr.xcmstarter.home=%XCM_INSTALL_DIR% -Dninja.external.configuration=conf/application.conf -Dninja.mode=prod %*
set LOGBK_OPTS="-Dlogback.configurationFile=conf/logback.xml"
set JETTY_OPTS="-Djetty.logging=%XCM_INSTALL_DIR:\=/%/logs/jetty.log"
set XCMAILR_JAR="./xcmailr-jetty-starter.jar"

:: run with all options
cd %XCM_INSTALL_DIR%
java %JVM_OPTS% %XCMAILR_OPTS% %LOGBK_OPTS% %JETTY_OPTS% -jar %XCMAILR_JAR%

