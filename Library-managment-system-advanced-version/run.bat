@echo off
:: Root launcher — always resolves to its own directory then calls Library\run.bat
cd /d "%~dp0Library"
call run.bat
