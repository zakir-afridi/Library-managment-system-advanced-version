@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion
title LibraCore Pro v3.0.0
cd /d "%~dp0"

echo.
echo  ============================================================
echo   LibraCore Pro v3.0.0 - Library Management System
echo   Java 24  ^|  JavaFX 21.0.2  ^|  HikariCP  ^|  SQLite
echo  ============================================================
echo.

:: --- Auto-detect Java ---
set "JAVA_EXE="

if defined JAVA_HOME (
    if exist "!JAVA_HOME!\bin\java.exe" (
        set "JAVA_EXE=!JAVA_HOME!\bin\java.exe"
        goto :java_found
    )
)

where java >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=*" %%J in ('where java') do (
        if not defined JAVA_EXE set "JAVA_EXE=%%J"
    )
    if defined JAVA_EXE goto :java_found
)

for %%V in (24 23 22 21 17) do (
    for %%P in (
        "C:\Program Files\Java\jdk-%%V"
        "C:\Program Files\Eclipse Adoptium\jdk-%%V*"
        "C:\Program Files\Microsoft\jdk-%%V*"
        "C:\Program Files\Amazon Corretto\jdk%%V*"
    ) do (
        if not defined JAVA_EXE (
            for /d %%D in (%%P) do (
                if exist "%%D\bin\java.exe" set "JAVA_EXE=%%D\bin\java.exe"
            )
        )
    )
)

:java_found
if not defined JAVA_EXE (
    echo  [ERROR] Java 17+ not found. Install from: https://adoptium.net/
    pause & exit /b 1
)
echo  [OK] Java: !JAVA_EXE!

:: --- JAR ---
set "JAR=target\LibraCore-Pro-3.0.0.jar"

if exist "%JAR%" (
    if /i not "%~1"=="--rebuild" (
        echo  [OK] JAR found - skipping build.  (use --rebuild to force)
        goto :launch
    )
)

:: --- Auto-detect Maven ---
set "MVN="

where mvn >nul 2>&1
if %errorlevel% equ 0 ( set "MVN=mvn" & goto :build )

for %%V in (M2_HOME MAVEN_HOME) do (
    if not defined MVN (
        if defined %%V (
            if exist "!%%V!\bin\mvn.cmd" set "MVN=!%%V!\bin\mvn.cmd"
        )
    )
)
if defined MVN goto :build

for %%D in ("D:\maven" "C:\maven" "C:\tools\maven") do (
    if not defined MVN (
        for /d %%M in ("%%~D\apache-maven-*") do (
            if exist "%%M\bin\mvn.cmd" set "MVN=%%M\bin\mvn.cmd"
        )
    )
)
if defined MVN goto :build

echo  [ERROR] Maven not found. Download from: https://maven.apache.org/
pause & exit /b 1

:: --- Build ---
:build
echo  [BUILD] Running Maven...
set "JAVA_HOME=C:\Program Files\Java\jdk-24"
"!MVN!" clean package -DskipTests
if %errorlevel% neq 0 (
    echo  [ERROR] Build failed.
    pause & exit /b 1
)
echo  [OK] Build successful.

:: --- Launch ---
:launch
echo.
echo  [LAUNCH] Starting LibraCore Pro v3.0.0...
echo  Default login: admin / admin
echo.

if not exist "%JAR%" (
    echo  [ERROR] JAR not found: %JAR%
    pause & exit /b 1
)

set "FX_PATH="
for %%F in (
    "target\lib\javafx-controls-*-win.jar"
    "target\lib\javafx-fxml-*-win.jar"
    "target\lib\javafx-graphics-*-win.jar"
    "target\lib\javafx-base-*-win.jar"
    "target\lib\javafx-swing-*-win.jar"
) do (
    for %%G in (%%F) do (
        if "!FX_PATH!"=="" (
            set "FX_PATH=%%G"
        ) else (
            set "FX_PATH=!FX_PATH!;%%G"
        )
    )
)

if "!FX_PATH!"=="" (
    for %%F in ("target\lib\javafx-*.jar") do (
        if "!FX_PATH!"=="" (
            set "FX_PATH=%%F"
        ) else (
            set "FX_PATH=!FX_PATH!;%%F"
        )
    )
)

set "CP=%JAR%"
for %%F in (target\lib\*.jar) do set "CP=!CP!;%%F"

"!JAVA_EXE!" ^
  --module-path "!FX_PATH!" ^
  --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.swing ^
  --enable-preview ^
  --enable-native-access=javafx.graphics,ALL-UNNAMED ^
  -Dsun.misc.unsafe.memory.access=allow ^
  -Dfile.encoding=UTF-8 ^
  -Dstdout.encoding=UTF-8 ^
  -cp "!CP!" ^
  com.library.LibraCoreApp

if %errorlevel% neq 0 (
    echo.
    echo  [ERROR] Application exited with code: %errorlevel%
    pause
)

endlocal
