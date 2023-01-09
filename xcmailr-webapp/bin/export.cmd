@echo off
setlocal enabledelayedexpansion

set SQL_FILE=%1

if "%SQL_FILE%"=="" (
  echo "Must specify at least path to some file to store SQL to as argument"
  exit /b 1
)

call %0\..\shell.cmd "script nosettings drop to '%SQL_FILE%';"

