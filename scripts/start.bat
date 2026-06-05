@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0.."

where docker >nul 2>&1
if errorlevel 1 (
  echo Docker is required. Install Docker Desktop, then run this script again.
  exit /b 1
)

docker compose version >nul 2>&1
if errorlevel 1 (
  where docker-compose >nul 2>&1
  if errorlevel 1 (
    echo Docker Compose is required.
    exit /b 1
  )
  set COMPOSE_CMD=docker-compose
) else (
  set COMPOSE_CMD=docker compose
)

echo Starting Medicalytics backend and database...
%COMPOSE_CMD% up -d --build

echo Waiting for API at http://localhost:8080 ...
set /a ATTEMPTS=0
:wait_loop
curl -fsS http://localhost:8080/actuator/health >nul 2>&1
if not errorlevel 1 goto backend_ready
set /a ATTEMPTS+=1
if !ATTEMPTS! GEQ 60 (
  echo Backend did not become healthy in time. Check logs with: %COMPOSE_CMD% logs backend
  exit /b 1
)
timeout /t 2 /nobreak >nul
goto wait_loop

:backend_ready
echo Backend is ready.

if not defined MEDICALYTICS_API_URL set MEDICALYTICS_API_URL=http://localhost:8080

if exist "dist\frontend\bin\app.bat" (
  echo Launching packaged desktop app...
  start "" "dist\frontend\bin\app.bat"
  exit /b 0
)

where mvn >nul 2>&1
if not errorlevel 1 (
  echo Launching desktop app with Maven...
  pushd frontend
  mvn -q javafx:run
  popd
  exit /b 0
)

echo.
echo Server stack is running.
echo API: %MEDICALYTICS_API_URL%
echo.
echo To open the desktop app, either:
echo   1. Run scripts\build-frontend.sh once, then scripts\start.bat again
echo   2. Or from the frontend folder: mvn javafx:run
