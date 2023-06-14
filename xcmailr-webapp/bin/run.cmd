@echo off
setlocal enabledelayedexpansion

:: setup paths and options
if not defined INSTALL_DIR for %%G in ("%~dp0..") do set INSTALL_DIR=%%~fG

if not defined CONFIG_DIR set CONFIG_DIR=%INSTALL_DIR%\conf
if not defined CONFIG_DIR_URI set CONFIG_DIR_URI=file:///%CONFIG_DIR:\=/%

if not defined KEYSTORE_FILE set KEYSTORE_FILE=%CONFIG_DIR%\keystore.jks
if not defined KEYSTORE_PW set KEYSTORE_PW=topsecret


if exist "%KEYSTORE_FILE%" (
  set JAVA_OPTS=%JAVA_OPTS% "-Djavax.net.ssl.keyStore=%KEYSTORE_FILE:\=/%" "-Djavax.net.ssl.keyStorePassword=%KEYSTORE_PW%"
)

set CLASSPATH=%INSTALL_DIR%\lib\*

set JAVA_OPTS=-Dh2.maxCompactTime=150000 "-Dninja.external.configuration=%CONFIG_DIR_URI%/application.conf" -Dninja.mode=prod
set JAVA_OPTS=%JAVA_OPTS% "-Dlogback.configurationFile=%CONFIG_DIR_URI%/logback.xml"

:: append options to suppress illegal access warnings for Java 9+
set PACKAGES=java.base/java.lang java.base/java.lang.reflect java.base/java.text java.base/java.util
for %%P in (%PACKAGES%) do set JAVA_OPTS=!JAVA_OPTS! --add-opens=%%P=ALL-UNNAMED
rem set JAVA_OPTS=%JAVA_OPTS% --illegal-access=debug

set JAVA_OPTS=%JAVA_OPTS% -cp "%CLASSPATH%"

:: run with all options
java %JAVA_OPTS% %* main.Main


