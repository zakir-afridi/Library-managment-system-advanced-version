@echo off
setlocal EnableDelayedExpansion
title LibraCore Pro v2.0.0

cd /d "%~dp0"

echo.
echo  ============================================================
echo   LibraCore Pro v2.0.0 - Library Management System
echo  ============================================================
echo.

:: ── Java check ────────────────────────────────────────────────────────────────
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo  [ERROR] Java not found on PATH.
    echo  Install Java 17+ from: https://adoptium.net/
    pause & exit /b 1
)
echo  [OK] Java detected.

:: ── Skip build if JAR already exists ─────────────────────────────────────────
set "JAR=target\library-management-system-2.0.0.jar"
if exist "%JAR%" (
    if /i not "%~1"=="--rebuild" (
        echo  [OK] JAR found. Skipping build.
        goto :launch
    )
)

:: ── Locate Maven ──────────────────────────────────────────────────────────────
set "MVN="

:: 1. mvn on PATH
where mvn >nul 2>&1
if %errorlevel% equ 0 ( set "MVN=mvn" & goto :build )

:: 2. Known exact location (most reliable)
set "_KNOWN=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.8.5-bin\5i5jha092a3i37g0paqnfr15e0\apache-maven-3.8.5\bin\mvn.cmd"
if exist "%_KNOWN%" ( set "MVN=%_KNOWN%" & goto :build )

:: 3. Search all apache-maven-* installs under .m2\wrapper\dists
for /d %%A in ("%USERPROFILE%\.m2\wrapper\dists\apache-maven-*") do (
    for /d %%B in ("%%A\*") do (
        if exist "%%B\apache-maven-*\bin\mvn.cmd" (
            for /d %%C in ("%%B\apache-maven-*") do (
                if not defined MVN (
                    if exist "%%C\bin\mvn.cmd" set "MVN=%%C\bin\mvn.cmd"
                )
            )
        )
    )
)
if defined MVN goto :build

echo  [ERROR] Maven not found.
echo  Install from: https://maven.apache.org/download.cgi
pause & exit /b 1

:: ── Build ─────────────────────────────────────────────────────────────────────
:build
echo  [OK] Maven: !MVN!
echo.
echo  [BUILD] Running Maven clean package...
echo.
"!MVN!" clean package -DskipTests
if %errorlevel% neq 0 (
    echo.
    echo  [ERROR] Build failed.
    pause & exit /b 1
)
echo.
echo  [OK] Build successful.

:: ── Launch ────────────────────────────────────────────────────────────────────
:launch
echo.
echo  [LAUNCH] Starting LibraCore Pro...
echo  Login: admin / admin
echo.

if not exist "%JAR%" (
    echo  [ERROR] JAR not found: %JAR%
    pause & exit /b 1
)

set "FX=target\lib"
set "FX_MODS=!FX!\javafx-controls-21.0.1.jar;!FX!\javafx-controls-21.0.1-win.jar;!FX!\javafx-fxml-21.0.1.jar;!FX!\javafx-fxml-21.0.1-win.jar;!FX!\javafx-graphics-21.0.1.jar;!FX!\javafx-graphics-21.0.1-win.jar;!FX!\javafx-base-21.0.1.jar;!FX!\javafx-base-21.0.1-win.jar;!FX!\javafx-swing-21.0.1.jar;!FX!\javafx-swing-21.0.1-win.jar"

set "CP=%JAR%"
for %%F in (target\lib\*.jar) do set "CP=!CP!;%%F"

java ^
  --module-path "!FX_MODS!" ^
  --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.swing ^
  --enable-native-access=ALL-UNNAMED,javafx.graphics ^
  -Dsun.misc.unsafe.memory.access=allow ^
  -cp "!CP!" ^
  com.library.LibraCoreApp

if %errorlevel% neq 0 (
    echo.
    echo  [ERROR] Application crashed. Exit code: %errorlevel%
    pause
)

endlocal
