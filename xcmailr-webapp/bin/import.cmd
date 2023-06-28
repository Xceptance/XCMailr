@echo off
setlocal enabledelayedexpansion

set SQL_FILE=%~f1

if not exist "%SQL_FILE%" (
  echo "Must specify at least path to some SQL file as argument"
  exit /b 1
)

call %0\..\shell.cmd "runscript from '%SQL_FILE%' from_1x;"

