@echo off
setlocal
cd /d "%~dp0.."

call "%~dp0setup-jdk.bat"
if errorlevel 1 exit /b 1

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-windows-package.ps1"
exit /b %ERRORLEVEL%
