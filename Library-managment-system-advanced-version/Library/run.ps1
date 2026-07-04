$ErrorActionPreference = "Stop"

$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent $projectDir
$launcher = Join-Path $rootDir "LaunchLibraCore.bat"

if (-not (Test-Path $launcher)) {
    throw "Launcher not found: $launcher"
}

Push-Location $rootDir
try {
    & cmd.exe /c "`"$launcher`""
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
