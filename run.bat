@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"
if not exist out mkdir out
set SOURCES=
for %%F in (src\*.java) do set SOURCES=!SOURCES! "%%F"
javac -encoding UTF-8 -d out %SOURCES%
if errorlevel 1 (
    echo.
    echo Compilation failed. Make sure a Java JDK is installed and javac is on PATH.
    pause
    exit /b 1
)
java -cp out SupermarketBillingApp
if errorlevel 1 pause
endlocal
