@echo off
setlocal
cd /d "%~dp0"

where mvn >nul 2>nul
if errorlevel 1 (
    echo Maven was not found. Install Apache Maven 3.9 or later and reopen Command Prompt.
    pause
    exit /b 1
)

if not exist config\db.properties (
    copy config\db.properties.example config\db.properties >nul
    echo Created config\db.properties from the example.
    echo Edit that file with your MySQL username and password, then run run.bat again.
    pause
    exit /b 1
)

mvn -q compile exec:java
if errorlevel 1 (
    echo.
    echo The application could not start. Check the MySQL server, database setup, and credentials.
    pause
    exit /b 1
)

endlocal
