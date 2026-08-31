@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion
title LibraCore Pro v3.0.0
cd /d "%~dp0"
color 0A

cls
echo.
echo  ██╗     ██╗██████╗ ██████╗  █████╗  ██████╗ ██████╗ ██████╗ ███████╗
echo  ██║     ██║██╔══██╗██╔══██╗██╔══██╗██╔════╝██╔═══██╗██╔══██╗██╔════╝
echo  ██║     ██║██████╔╝██████╔╝███████║██║     ██║   ██║██████╔╝█████╗
echo  ██║     ██║██╔══██╗██╔══██╗██╔══██║██║     ██║   ██║██╔══██╗██╔══╝
echo  ███████╗██║██████╔╝██║  ██║██║  ██║╚██████╗╚██████╔╝██║  ██║███████╗
echo  ╚══════╝╚═╝╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝ ╚═════╝ ╚═╝  ╚═╝╚══════╝
echo.
echo                    ██████╗ ██████╗  ██████╗
echo                    ██╔══██╗██╔══██╗██╔═══██╗
echo                    ██████╔╝██████╔╝██║   ██║
echo                    ██╔═══╝ ██╔══██╗██║   ██║
echo                    ██║     ██║  ██║╚██████╔╝
echo                    ╚═╝     ╚═╝  ╚═╝ ╚═════╝
echo.
echo  ════════════════════════════════════════════════════════════════════════
echo   v3.0.0  ^|  Java 24  ^|  JavaFX 21  ^|  SQLite  ^|  HikariCP
echo  ════════════════════════════════════════════════════════════════════════
echo.

:: ── Locate Java ──────────────────────────────────────────────────────────────
set "JAVA_EXE=C:\Program Files\Java\jdk-24\bin\java.exe"
if not exist "!JAVA_EXE!" (
    where java >nul 2>&1 && (for /f "tokens=*" %%J in ('where java') do set "JAVA_EXE=%%J") || (
        echo  [!] Java not found. Install JDK 17+ from https://adoptium.net
        pause & exit /b 1
    )
)

:: ── Locate Maven ─────────────────────────────────────────────────────────────
set "MVN=D:\maven\apache-maven-3.9.6\bin\mvn.cmd"
if not exist "!MVN!" (
    where mvn >nul 2>&1 && set "MVN=mvn" || (
        echo  [!] Maven not found at D:\maven. Checking system PATH...
        pause & exit /b 1
    )
)

:: ── Build if JAR missing ──────────────────────────────────────────────────────
set "JAR=target\LibraCore-Pro-3.0.0.jar"
if not exist "%JAR%" (
    echo  [~] First run detected — building project...
    echo.
    set "JAVA_HOME=C:\Program Files\Java\jdk-24"
    "!MVN!" clean package -DskipTests --no-transfer-progress
    if !errorlevel! neq 0 (
        echo.
        echo  [✗] Build FAILED. See errors above.
        pause & exit /b 1
    )
    echo.
    echo  [✓] Build successful!
    echo.
)

:: ── Launch ────────────────────────────────────────────────────────────────────
echo  [✓] Java  : !JAVA_EXE!
echo  [✓] Maven : !MVN!
echo  [✓] JAR   : %JAR%
echo.
echo  ──────────────────────────────────────────────────────────────────────
echo   Starting LibraCore Pro...     Default login:  admin / admin
echo  ──────────────────────────────────────────────────────────────────────
echo.

set "CP=%JAR%"
for %%F in (target\lib\*.jar) do set "CP=!CP!;%%F"

set "FX_PATH="
for %%F in (target\lib\javafx-controls-*-win.jar target\lib\javafx-fxml-*-win.jar target\lib\javafx-graphics-*-win.jar target\lib\javafx-base-*-win.jar) do (
    if exist "%%F" (
        if "!FX_PATH!"=="" ( set "FX_PATH=%%F" ) else ( set "FX_PATH=!FX_PATH!;%%F" )
    )
)

"!JAVA_EXE!" ^
  --module-path "!FX_PATH!" ^
  --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.swing ^
  --enable-preview ^
  --enable-native-access=javafx.graphics,ALL-UNNAMED ^
  -Dsun.misc.unsafe.memory.access=allow ^
  -Dfile.encoding=UTF-8 ^
  -cp "!CP!" ^
  com.library.LibraCoreApp

if !errorlevel! neq 0 (
    echo.
    echo  [✗] Application exited with an error ^(code: !errorlevel!^)
    pause
)
endlocal
