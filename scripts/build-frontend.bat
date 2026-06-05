@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0.."
set ROOT_DIR=%CD%
set FRONTEND_DIR=%ROOT_DIR%\frontend
set DIST_DIR=%ROOT_DIR%\dist\desktop
set MVN=%ROOT_DIR%\backend\mvnw.cmd

where java >nul 2>&1
if errorlevel 1 (
  echo JDK 21+ is required to build the desktop app.
  exit /b 1
)

echo Building standalone Medicalytics desktop app...
pushd "%FRONTEND_DIR%"
call "%MVN%" -f pom.xml -B -DskipTests clean package
if errorlevel 1 exit /b 1
popd

set APP_IMAGE_DIR=%FRONTEND_DIR%\target\dist\Medicalytics
if not exist "%APP_IMAGE_DIR%" (
  echo Expected app image was not created at: %APP_IMAGE_DIR%
  exit /b 1
)

if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
mkdir "%DIST_DIR%"
xcopy /E /I /Y "%APP_IMAGE_DIR%" "%DIST_DIR%\Medicalytics" >nul

set ARCHIVE_NAME=medicalytics-desktop-windows-x64.zip
powershell -NoProfile -Command "Compress-Archive -Path '%DIST_DIR%\Medicalytics' -DestinationPath '%ROOT_DIR%\dist\%ARCHIVE_NAME%' -Force"

echo.
echo Standalone desktop app ready:
echo   Run directly: %DIST_DIR%\Medicalytics\Medicalytics.exe
echo   Archive:      %ROOT_DIR%\dist\%ARCHIVE_NAME%
echo.
echo The app bundles its own Java runtime. Start the API first with scripts\start.bat or Docker.
