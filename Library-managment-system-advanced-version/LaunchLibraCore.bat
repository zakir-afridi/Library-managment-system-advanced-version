@echo off
setlocal EnableDelayedExpansion
title LibraCore Pro v2.0.0

set "ROOT=%~dp0Library"
cd /d "%ROOT%"

echo.
echo  ============================================================
echo   LibraCore Pro v2.0.0 - Library Management System
echo  ============================================================
echo.

:: ── Auto-detect Java ──────────────────────────────────────────────────────────
set "JAVA_EXE="

:: 1. java already on PATH
java -version >nul 2>&1
if %errorlevel% equ 0 ( set "JAVA_EXE=java" & goto :java_found )

:: 2. JAVA_HOME environment variable
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
        goto :java_found
    )
)

:: 3. Scan common install locations
for %%D in (
    "C:\Program Files\Java"
    "C:\Program Files\Eclipse Adoptium"
    "C:\Program Files\Amazon Corretto"
    "C:\Program Files\Microsoft"
    "C:\Program Files\BellSoft"
    "C:\Program Files\Azul Systems\Zulu"
) do (
    if exist "%%~D" (
        for /d %%J in ("%%~D\jdk*") do (
            if not defined JAVA_EXE (
                if exist "%%J\bin\java.exe" set "JAVA_EXE=%%J\bin\java.exe"
            )
        )
    )
)
if defined JAVA_EXE goto :java_found

echo  [ERROR] Java not found. Install Java 17+ from: https://adoptium.net/
pause & exit /b 1

:java_found
echo  [OK] Java: !JAVA_EXE!

:: ── Auto-detect Maven ─────────────────────────────────────────────────────────
set "MVN="

:: 1. mvn already on PATH
mvn -version >nul 2>&1
if %errorlevel% equ 0 ( set "MVN=mvn" & goto :mvn_found )

:: 2. M2_HOME or MAVEN_HOME environment variable
for %%V in (M2_HOME MAVEN_HOME) do (
    if not defined MVN (
        if defined %%V (
            if exist "!%%V!\bin\mvn.cmd" set "MVN=!%%V!\bin\mvn.cmd"
        )
    )
)
if defined MVN goto :mvn_found

:: 3. Maven wrapper under .m2\wrapper\dists
for /d %%A in ("%USERPROFILE%\.m2\wrapper\dists\apache-maven-*") do (
    for /d %%B in ("%%A\*") do (
        for /d %%C in ("%%B\apache-maven-*") do (
            if not defined MVN (
                if exist "%%C\bin\mvn.cmd" set "MVN=%%C\bin\mvn.cmd"
            )
        )
    )
)
if defined MVN goto :mvn_found

:: 4. Common manual install locations
for %%D in (
    "C:\Program Files\Apache\maven"
    "C:\maven"
    "C:\tools\maven"
    "D:\maven"
) do (
    if not defined MVN (
        for /d %%M in ("%%~D\apache-maven-*") do (
            if exist "%%M\bin\mvn.cmd" set "MVN=%%M\bin\mvn.cmd"
        )
    )
)
if defined MVN goto :mvn_found

echo  [ERROR] Maven not found. Install from: https://maven.apache.org/download.cgi
pause & exit /b 1

:mvn_found
echo  [OK] Maven: !MVN!

:: ── Build or skip ─────────────────────────────────────────────────────────────
set "JAR=target\library-management-system-2.0.0.jar"
if exist "%JAR%" (
    echo  [OK] JAR found. Skipping build.
    goto :launch
)

echo.
echo  [BUILD] First run — building project (this takes ~1 minute)...
echo.
"!MVN!" clean package -DskipTests
if %errorlevel% neq 0 (
    echo  [ERROR] Build failed.
    pause & exit /b 1
)
echo  [OK] Build successful.

:: ── Launch ────────────────────────────────────────────────────────────────────
:launch
echo.
echo  [LAUNCH] Starting LibraCore Pro...
echo  Login: admin / admin
echo.

set "FX=target\lib"
set "FX_MODS=!FX!\javafx-controls-21.0.1.jar;!FX!\javafx-controls-21.0.1-win.jar;!FX!\javafx-fxml-21.0.1.jar;!FX!\javafx-fxml-21.0.1-win.jar;!FX!\javafx-graphics-21.0.1.jar;!FX!\javafx-graphics-21.0.1-win.jar;!FX!\javafx-base-21.0.1.jar;!FX!\javafx-base-21.0.1-win.jar;!FX!\javafx-swing-21.0.1.jar;!FX!\javafx-swing-21.0.1-win.jar"

set "CP=%JAR%"
for %%F in (target\lib\*.jar) do set "CP=!CP!;%%F"

"!JAVA_EXE!" ^
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
