@echo off
setlocal enabledelayedexpansion

:: =============================================================================
:: Library Management System - Production Build & Run Script
:: =============================================================================
:: This script handles compilation, resource copying, and execution with
:: proper error handling and SQLite native access configuration.
:: =============================================================================

echo.
echo ===============================================
echo   Library Management System - Starting...
echo ===============================================
echo.

:: Set working directory
set "PROJECT_DIR=d:\LMS_UPGRADE\Library-managment-system-advanced-version\Library"
cd /d "%PROJECT_DIR%"

if not exist "%PROJECT_DIR%" (
    echo ERROR: Project directory not found: %PROJECT_DIR%
    echo Please check the path and try again.
    pause
    exit /b 1
)

:: Check Java installation
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 17+ and add it to your PATH
    pause
    exit /b 1
)

:: Clean previous build
echo [1/4] Cleaning previous build...
if exist target (
    rmdir /s /q target >nul 2>&1
    if exist target (
        echo WARNING: Could not completely clean target directory
    )
)
mkdir target\classes >nul 2>&1

:: Compile Java source files
echo [2/4] Compiling Java source files...
echo   - Compiling main classes...
javac -cp "lib\*" -d target\classes ^
    src\main\java\com\library\*.java ^
    src\main\java\com\library\model\*.java ^
    src\main\java\com\library\repository\*.java ^
    src\main\java\com\library\service\*.java ^
    src\main\java\com\library\util\*.java ^
    src\main\java\com\library\controller\*.java ^
    src\main\java\com\library\ui\*.java 2>compile_errors.log

if %errorlevel% neq 0 (
    echo.
    echo ERROR: Compilation failed!
    echo Check compile_errors.log for details:
    echo.
    type compile_errors.log
    echo.
    pause
    exit /b 1
) else (
    echo   ✓ Compilation successful
    if exist compile_errors.log del compile_errors.log >nul 2>&1
)

:: Copy resources
echo [3/4] Copying resources...
if exist src\main\resources (
    xcopy /s /y /q src\main\resources\* target\classes\ >nul 2>&1
    if %errorlevel% equ 0 (
        echo   ✓ Resources copied successfully
    ) else (
        echo   WARNING: Some resources may not have been copied
    )
) else (
    echo   WARNING: Resources directory not found
)

:: Check if main class exists
if not exist "target\classes\com\library\ProfessionalLibraryApp.class" (
    echo ERROR: Main class not found after compilation
    echo Expected: target\classes\com\library\ProfessionalLibraryApp.class
    pause
    exit /b 1
)

:: Start application with SQLite native access fix
echo [4/4] Starting Library Management System...
echo.
echo ===============================================
echo   Application is starting...
echo   Login credentials: admin / admin
echo ===============================================
echo.

:: Run with native access enabled for SQLite (fixes the warning)
:: --enable-native-access=ALL-UNNAMED allows SQLite JDBC to load native libraries
java --enable-native-access=ALL-UNNAMED ^
     -cp "lib\*;target\classes" ^
     com.library.ProfessionalLibraryApp

set "APP_EXIT_CODE=%errorlevel%"

echo.
echo ===============================================
if %APP_EXIT_CODE% equ 0 (
    echo   Application closed normally
) else (
    echo   Application exited with error code: %APP_EXIT_CODE%
    echo   This might indicate a runtime error
)
echo ===============================================
echo.

:: Cleanup
if exist compile_errors.log del compile_errors.log >nul 2>&1

echo Press any key to exit...
pause >nul
exit /b %APP_EXIT_CODE%