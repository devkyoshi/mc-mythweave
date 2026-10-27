@echo off
rem Runs dev.ps1 without needing to change PowerShell's execution policy.
rem Double-click for a menu, or run e.g. "dev client" / "dev multiplayer".
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0dev.ps1" %*
if "%~1"=="" pause
