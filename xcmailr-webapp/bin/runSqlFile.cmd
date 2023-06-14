@echo off
setlocal enabledelayedexpansion

:: runs the SQL script
call %0\..\import.cmd %1

