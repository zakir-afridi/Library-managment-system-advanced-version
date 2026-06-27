$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$launcher = Join-Path $root "LaunchLibraCore.bat"
$shortcutPath = Join-Path $root "LibraCore Pro.lnk"

if (-not (Test-Path $launcher)) {
    throw "Launcher not found: $launcher"
}

$ws = New-Object -ComObject WScript.Shell
$sc = $ws.CreateShortcut($shortcutPath)
$sc.TargetPath = Join-Path $env:SystemRoot "System32\cmd.exe"
$sc.Arguments = "/c `"$launcher`""
$sc.WorkingDirectory = $root
$sc.WindowStyle = 1
$sc.Description = "LibraCore Pro v3.0.0"
$sc.Save()

Write-Host "Shortcut created: $shortcutPath"
