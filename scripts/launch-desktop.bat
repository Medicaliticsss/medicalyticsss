@echo off
setlocal

cd /d "%~dp0.."
if not defined MEDICALYTICS_API_URL set MEDICALYTICS_API_URL=http://localhost:8080

if exist "dist\desktop\Medicalytics\Medicalytics.exe" (
  start "" "dist\desktop\Medicalytics\Medicalytics.exe"
  exit /b 0
)

if exist "dist\frontend\bin\app.bat" (
  start "" "dist\frontend\bin\app.bat"
  exit /b 0
)

echo Standalone desktop app not found.
echo Build it once with: scripts\build-frontend.bat
exit /b 1
