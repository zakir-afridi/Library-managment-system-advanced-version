@echo off
setlocal

set "ROOT_DIR=%~dp0.."
set "LAUNCHER=%ROOT_DIR%\LaunchLibraCore.bat"

if not exist "%LAUNCHER%" (
    echo [ERROR] Launcher not found: %LAUNCHER%
    pause
    exit /b 1
)

cmd /c "%LAUNCHER%"
exit /b %errorlevel%
