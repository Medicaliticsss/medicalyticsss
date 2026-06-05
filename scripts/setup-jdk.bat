@echo off
REM Configures JAVA_HOME and PATH for Maven builds on Windows.
REM Call from other scripts: call "%~dp0setup-jdk.bat" || exit /b 1

if defined JAVA_HOME (
  if exist "%JAVA_HOME%\bin\javac.exe" (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    goto :ready
  )
)

where javac >nul 2>&1
if not errorlevel 1 goto :ready

if exist "%USERPROFILE%\.jdks" (
  for /f "delims=" %%J in ('dir /b /ad /o-n "%USERPROFILE%\.jdks" 2^>nul') do (
    if exist "%USERPROFILE%\.jdks\%%J\bin\javac.exe" (
      set "JAVA_HOME=%USERPROFILE%\.jdks\%%J"
      set "PATH=%JAVA_HOME%\bin;%PATH%"
      goto :ready
    )
  )
)

for /d %%J in ("C:\Program Files\Java\jdk*") do (
  if exist "%%J\bin\javac.exe" (
    set "JAVA_HOME=%%J"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    goto :ready
  )
)

for /d %%J in ("C:\Program Files\Eclipse Adoptium\jdk*") do (
  if exist "%%J\bin\javac.exe" (
    set "JAVA_HOME=%%J"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    goto :ready
  )
)

echo.
echo ERROR: JDK 21+ is required. This shell is using a JRE or JAVA_HOME is not set.
echo.
echo Fix options:
echo   1. In PowerShell for this session:
echo        $env:JAVA_HOME = "C:\Users\YOUR_USER\.jdks\corretto-21.0.x"
echo        $env:Path = "$env:JAVA_HOME\bin;" + $env:Path
echo        scripts\build-frontend.bat
echo.
echo   2. Permanently: Windows Environment Variables - set JAVA_HOME to your JDK folder.
echo.
echo   3. From IntelliJ Maven tool window: Frontend ^> Lifecycle ^> package
echo      (uses the JDK configured in IntelliJ, not the system PATH)
echo.
exit /b 1

:ready
for /f "tokens=3" %%V in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VERSION=%%V
set JAVA_VERSION=%JAVA_VERSION:"=%
echo Using JDK: %JAVA_HOME%
exit /b 0
