$ws = New-Object -ComObject WScript.Shell
$sc = $ws.CreateShortcut("d:\Projects\Library-managment-system-advanced-version\Library-managment-system-advanced-version\LibraCore Pro.lnk")
$sc.TargetPath = "C:\Windows\System32\cmd.exe"
$sc.Arguments = '/c "d:\Projects\Library-managment-system-advanced-version\Library-managment-system-advanced-version\Library\run.bat"'
$sc.WorkingDirectory = "d:\Projects\Library-managment-system-advanced-version\Library-managment-system-advanced-version\Library"
$sc.WindowStyle = 1
$sc.Description = "LibraCore Pro v2.0.0"
$sc.Save()
Write-Host "Shortcut created!"
