@echo off
setlocal EnableExtensions EnableDelayedExpansion

title LibraCore Pro v3.0.0
echo [LAUNCH] Starting LibraCore Pro v3.0.0...

set "ROOT_DIR=%~dp0"
set "PROJECT_DIR=%ROOT_DIR%Library"

if not exist "%PROJECT_DIR%\pom.xml" (
    if exist "%ROOT_DIR%pom.xml" (
        set "PROJECT_DIR=%ROOT_DIR%"
    ) else (
        for /d %%D in ("%ROOT_DIR%*") do (
            if not defined FOUND_PROJECT (
                if exist "%%~fD\pom.xml" (
                    set "PROJECT_DIR=%%~fD"
                    set "FOUND_PROJECT=1"
                )
            )
        )
    )
)

if not exist "%PROJECT_DIR%\pom.xml" (
    echo [ERROR] Could not find the Maven project folder.
    echo [INFO] Expected a pom.xml under: %ROOT_DIR%
    pause
    exit /b 1
)

set "TARGET_DIR=%PROJECT_DIR%\target"
set "LIB_DIR=%TARGET_DIR%\lib"
set "JAR_PATH=%TARGET_DIR%\LibraCore-Pro-3.0.0.jar"

if not exist "%JAR_PATH%" (
    for %%J in ("%TARGET_DIR%\*.jar") do (
        if exist "%%~fJ" if not defined JAR_PATH_FOUND (
            set "JAR_PATH=%%~fJ"
            set "JAR_PATH_FOUND=1"
        )
    )
)

call :buildModulePath

if not exist "%JAR_PATH%" (
    echo [BUILD] Packaged JAR not found. Building project...
    call :runMavenBuild || exit /b 1
)

if not defined MODULE_PATH (
    echo [BUILD] JavaFX libraries not found. Building/downloading dependencies...
    call :runMavenBuild || exit /b 1
)

call :buildModulePath

if not exist "%JAR_PATH%" (
    echo [ERROR] Build finished, but no runnable JAR was found in:
    echo [INFO] %TARGET_DIR%
    pause
    exit /b 1
)

if not defined MODULE_PATH (
    echo [ERROR] JavaFX libraries were not found in target\lib or javafx-sdk\lib.
    pause
    exit /b 1
)

cd /d "%PROJECT_DIR%"
echo [OK] Project: %PROJECT_DIR%
echo [OK] JAR: %JAR_PATH%

java --enable-native-access=ALL-UNNAMED,javafx.graphics ^
     --module-path "%MODULE_PATH%" ^
     --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.swing ^
     -cp "%JAR_PATH%;%LIB_DIR%\*" ^
     com.library.LibraCoreApp

echo.
echo [EXIT] LibraCore Pro closed.
pause
exit /b 0

:buildModulePath
set "MODULE_PATH="
if exist "%LIB_DIR%" (
    for %%F in ("%LIB_DIR%\javafx-*.jar") do (
        if exist "%%~fF" (
            if defined MODULE_PATH (
                set "MODULE_PATH=!MODULE_PATH!;%%~fF"
            ) else (
                set "MODULE_PATH=%%~fF"
            )
        )
    )
)

if not defined MODULE_PATH (
    if exist "%PROJECT_DIR%\javafx-sdk\lib" (
        for %%F in ("%PROJECT_DIR%\javafx-sdk\lib\javafx*.jar") do (
            if exist "%%~fF" (
                if defined MODULE_PATH (
                    set "MODULE_PATH=!MODULE_PATH!;%%~fF"
                ) else (
                    set "MODULE_PATH=%%~fF"
                )
            )
        )
    )
)
exit /b 0

:runMavenBuild
cd /d "%PROJECT_DIR%"
set "MVN_CMD="

if exist "%PROJECT_DIR%\mvnw.cmd" set "MVN_CMD=%PROJECT_DIR%\mvnw.cmd"

if not defined MVN_CMD (
    where mvn >nul 2>&1
    if not errorlevel 1 set "MVN_CMD=mvn"
)

if not defined MVN_CMD (
    for %%V in (M2_HOME MAVEN_HOME) do (
        if not defined MVN_CMD (
            if defined %%V (
                if exist "!%%V!\bin\mvn.cmd" set "MVN_CMD=!%%V!\bin\mvn.cmd"
            )
        )
    )
)

if not defined MVN_CMD (
    echo [ERROR] Maven is not installed or not on PATH.
    echo [INFO] The launcher is portable, but a fresh clone needs Maven once to build.
    echo [INFO] Install Maven, or commit the target folder/JAR with the project.
    pause
    exit /b 1
)

echo [BUILD] Running Maven from: %PROJECT_DIR%
call "%MVN_CMD%" clean package -DskipTests
if errorlevel 1 (
    echo [ERROR] Build failed!
    pause
    exit /b 1
)

set "JAR_PATH=%TARGET_DIR%\LibraCore-Pro-3.0.0.jar"
if not exist "%JAR_PATH%" (
    for %%J in ("%TARGET_DIR%\*.jar") do (
        if exist "%%~fJ" if not defined JAR_PATH_FOUND_AFTER_BUILD (
            set "JAR_PATH=%%~fJ"
            set "JAR_PATH_FOUND_AFTER_BUILD=1"
        )
    )
)
exit /b 0
