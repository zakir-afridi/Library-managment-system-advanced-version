@echo off
setlocal EnableDelayedExpansion
title LibraCore Pro v3.0.0

cd /d "%~dp0"

echo.
echo  ============================================================
echo   LibraCore Pro v3.0.0 - Library Management System
echo   Java 24  ^|  JavaFX 21.0.2  ^|  HikariCP  ^|  SQLite
echo  ============================================================
echo.

:: ── Auto-detect Java ──────────────────────────────────────────────────────────
set "JAVA_EXE="

:: 1. JAVA_HOME environment variable
if defined JAVA_HOME (
    if exist "!JAVA_HOME!\bin\java.exe" (
        set "JAVA_EXE=!JAVA_HOME!\bin\java.exe"
        goto :java_found
    )
)

:: 2. java already on PATH
where java >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=*" %%J in ('where java') do (
        if not defined JAVA_EXE set "JAVA_EXE=%%J"
    )
    if defined JAVA_EXE goto :java_found
)

:: 3. Scan common install locations (newest first)
for %%V in (24 23 22 21 20 19 18 17) do (
    for %%P in (
        "C:\Program Files\Java\jdk-%%V"
        "C:\Program Files\Eclipse Adoptium\jdk-%%V*"
        "C:\Program Files\Microsoft\jdk-%%V*"
        "C:\Program Files\Amazon Corretto\jdk%%V*"
        "C:\Program Files\BellSoft\LibericaJDK-%%V*"
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
    echo  [ERROR] Java 17+ not found.
    echo  Install Java 21+ LTS from: https://adoptium.net/
    echo  Or set JAVA_HOME to your JDK installation.
    pause & exit /b 1
)
echo  [OK] Java: !JAVA_EXE!

:: ── JAR name ──────────────────────────────────────────────────────────────────
set "JAR=target\LibraCore-Pro-3.0.0.jar"

:: ── Skip build if JAR exists and --rebuild not passed ─────────────────────────
if exist "%JAR%" (
    if /i not "%~1"=="--rebuild" (
        echo  [OK] JAR found - skipping build. (pass --rebuild to force)
        goto :launch
    )
)

:: ── Auto-detect Maven ─────────────────────────────────────────────────────────
set "MVN="

:: 1. mvn already on PATH
where mvn >nul 2>&1
if %errorlevel% equ 0 ( set "MVN=mvn" & goto :build )

:: 2. M2_HOME / MAVEN_HOME environment variable
for %%V in (M2_HOME MAVEN_HOME) do (
    if not defined MVN (
        if defined %%V (
            if exist "!%%V!\bin\mvn.cmd" set "MVN=!%%V!\bin\mvn.cmd"
        )
    )
)
if defined MVN goto :build

:: 3. Maven wrapper under ~/.m2/wrapper/dists
for /d %%A in ("%USERPROFILE%\.m2\wrapper\dists\apache-maven-*") do (
    for /d %%B in ("%%A\*") do (
        for /d %%C in ("%%B\apache-maven-*") do (
            if not defined MVN (
                if exist "%%C\bin\mvn.cmd" set "MVN=%%C\bin\mvn.cmd"
            )
        )
    )
)
if defined MVN goto :build

:: 4. Common manual install locations
for %%D in (
    "D:\maven"
    "C:\maven"
    "C:\tools\maven"
    "C:\Program Files\Apache\maven"
    "C:\Program Files\Maven"
) do (
    if not defined MVN (
        for /d %%M in ("%%~D\apache-maven-*") do (
            if exist "%%M\bin\mvn.cmd" set "MVN=%%M\bin\mvn.cmd"
        )
    )
)
if defined MVN goto :build

echo  [ERROR] Maven not found.
echo  Download from: https://maven.apache.org/download.cgi
echo  Or place it at D:\maven\apache-maven-X.X.X\
pause & exit /b 1

:: ── Build ─────────────────────────────────────────────────────────────────────
:build
echo  [OK] Maven: !MVN!
echo.
echo  [BUILD] Running: mvn clean package -DskipTests
echo.
set "JAVA_HOME=C:\Program Files\Java\jdk-24"
"!MVN!" clean package -DskipTests
if %errorlevel% neq 0 (
    echo.
    echo  [ERROR] Build failed. Check output above.
    pause & exit /b 1
)
echo.
echo  [OK] Build successful.

:: ── Launch ────────────────────────────────────────────────────────────────────
:launch
echo.
echo  [LAUNCH] Starting LibraCore Pro v3.0.0...
echo  Default login: admin / admin
echo.

if not exist "%JAR%" (
    echo  [ERROR] JAR not found: %JAR%
    echo  Run this script without arguments to trigger a build first.
    pause & exit /b 1
)

:: Build module path from platform-specific JavaFX jars (prefer -win.jar)
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

:: Fallback: include all javafx jars if no -win jars found
if "!FX_PATH!"=="" (
    for %%F in ("target\lib\javafx-*.jar") do (
        if "!FX_PATH!"=="" (
            set "FX_PATH=%%F"
        ) else (
            set "FX_PATH=!FX_PATH!;%%F"
        )
    )
)

:: Build full classpath (JAR + all deps)
set "CP=%JAR%"
for %%F in (target\lib\*.jar) do set "CP=!CP!;%%F"

"!JAVA_EXE!" ^
  --module-path "!FX_PATH!" ^
  --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.swing ^
  --enable-preview ^
  --enable-native-access=javafx.graphics,javafx.media,ALL-UNNAMED ^
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
