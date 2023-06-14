@echo off
setlocal enabledelayedexpansion

:: setup paths and options
if not defined INSTALL_DIR for %%i in ("%~dp0..") do set INSTALL_DIR=%%~fi
if not defined CONFIG_DIR set CONFIG_DIR=%INSTALL_DIR%\conf

set CONF_FILE=%CONFIG_DIR%\application.conf
set SQL=%1

if not exist "%CONF_FILE%" (
  echo "Application configuration file not found"
  exit /b 1
)

:: parse values of 'ebean.datasource.[databaseUrl|username|password]' properties
for /f "tokens=1* delims==" %%G in ('findstr /rc:"^ebean.datasource.databaseUrl" "%CONF_FILE%"') do set DB_URL=%%H
for /f "tokens=1* delims==" %%G in ('findstr /rc:"^ebean.datasource.username" "%CONF_FILE%"') do set DB_USER=%%H
for /f "tokens=1* delims==" %%G in ('findstr /rc:"^ebean.datasource.password" "%CONF_FILE%"') do set DB_PASS=%%H

:: determine fully-qualified path of H2 library
for %%G in ("%INSTALL_DIR%"\lib\h2*.jar) do set H2_LIB=%%~fG

:: assemble arguments
set ARGS=-url "%DB_URL%" -user "%DB_USER%"
if not "%DB_PASS%"=="" set ARGS=%ARGS% -password "%DB_PASS%"

if defined SQL (
  java -cp "%H2_LIB%" org.h2.tools.Shell %ARGS% -sql %SQL%
) else (
  java -cp "%H2_LIB%" org.h2.tools.Shell %ARGS%
)

