$ErrorActionPreference = "Stop"

$InstallRoot = Split-Path $PSScriptRoot -Parent

$RuntimeDir = Join-Path $InstallRoot "runtime"
$DataDir = Join-Path $env:LOCALAPPDATA "Medicalytics"
$MysqlDataDir = Join-Path $DataDir "mysql"
$UploadsDir = Join-Path $DataDir "uploads"
$LogsDir = Join-Path $DataDir "logs"
$MysqlDir = Join-Path $RuntimeDir "mariadb"
$BackendJar = Join-Path $RuntimeDir "backend\backend.jar"
$DesktopExe = Join-Path $RuntimeDir "desktop\Medicalytics.exe"
$JavaExe = Join-Path $RuntimeDir "jre\bin\java.exe"
$MysqldExe = Join-Path $MysqlDir "bin\mysqld.exe"
$MysqlExe = Join-Path $MysqlDir "bin\mysql.exe"
$MysqlInstallDbExe = Join-Path $MysqlDir "bin\mysql_install_db.exe"
if (-not (Test-Path $MysqlInstallDbExe)) {
    $MysqlInstallDbExe = Join-Path $MysqlDir "bin\mariadb-install-db.exe"
}
$MyIni = Join-Path $MysqlDir "my.ini"
$InitMarker = Join-Path $DataDir "mysql-initialized"

function Test-MysqlSystemTables {
    param([string]$DataDir)
    return Test-Path (Join-Path $DataDir "mysql")
}

function Reset-MysqlDataDir {
    param([string]$DataDir)
    if (Test-Path $DataDir) {
        Remove-Item -Recurse -Force $DataDir
    }
    New-Item -ItemType Directory -Force -Path $DataDir | Out-Null
}

$MysqldProcess = $null
$BackendProcess = $null

function Stop-Services {
    if ($BackendProcess -and -not $BackendProcess.HasExited) {
        Stop-Process -Id $BackendProcess.Id -Force -ErrorAction SilentlyContinue
    }
    if ($MysqldProcess -and -not $MysqldProcess.HasExited) {
        & $MysqlExe --defaults-file="$MyIni" -u root -e "SHUTDOWN;" 2>$null | Out-Null
        Start-Sleep -Seconds 2
        if (-not $MysqldProcess.HasExited) {
            Stop-Process -Id $MysqldProcess.Id -Force -ErrorAction SilentlyContinue
        }
    }
}

trap {
    Stop-Services
    [System.Windows.Forms.MessageBox]::Show(
        "Medicalytics failed to start:`n`n$($_.Exception.Message)",
        "Medicalytics",
        [System.Windows.Forms.MessageBoxButtons]::OK,
        [System.Windows.Forms.MessageBoxIcon]::Error
    ) | Out-Null
    exit 1
}

Add-Type -AssemblyName System.Windows.Forms

foreach ($path in @($DataDir, $MysqlDataDir, $UploadsDir, $LogsDir)) {
    New-Item -ItemType Directory -Force -Path $path | Out-Null
}

if (-not (Test-Path $DesktopExe)) { throw "Desktop app not found: $DesktopExe" }
if (-not (Test-Path $BackendJar)) { throw "Backend not found: $BackendJar" }
if (-not (Test-Path $MysqldExe)) { throw "MariaDB not found: $MysqldExe" }
if (-not (Test-Path $MysqlInstallDbExe)) { throw "MariaDB installer not found: $MysqlInstallDbExe" }

$iniContent = @"
[mysqld]
port=3307
bind-address=127.0.0.1
datadir=$($MysqlDataDir.Replace('\', '/'))
max_allowed_packet=64M
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
console

[client]
port=3307
"@
Set-Content -Path $MyIni -Value $iniContent -Encoding ASCII

if (-not (Test-Path $InitMarker)) {
    Write-Host "First launch: preparing local database (this may take a minute)..."

    $hasDataFiles = (Test-Path $MysqlDataDir) -and
        ((Get-ChildItem $MysqlDataDir -Force -ErrorAction SilentlyContinue | Measure-Object).Count -gt 0)
    if ($hasDataFiles -and -not (Test-MysqlSystemTables $MysqlDataDir)) {
        Write-Host "Removing incomplete database files from a previous attempt..."
        Reset-MysqlDataDir $MysqlDataDir
    } elseif (-not $hasDataFiles) {
        New-Item -ItemType Directory -Force -Path $MysqlDataDir | Out-Null
    }

    # MariaDB 11.x does not support mysqld --initialize-insecure (MySQL-only option).
    $installProcess = Start-Process -FilePath $MysqlInstallDbExe -ArgumentList @(
        "--datadir=$MysqlDataDir",
        "-P", "3307"
    ) -Wait -PassThru -NoNewWindow
    if ($installProcess.ExitCode -ne 0) {
        throw "Database initialization failed (exit $($installProcess.ExitCode)). Delete `"$DataDir`" and try again."
    }

    $MysqldProcess = Start-Process -FilePath $MysqldExe -ArgumentList @("--defaults-file=$MyIni") -WindowStyle Hidden -PassThru
    Start-Sleep -Seconds 10

    & $MysqlExe --defaults-file="$MyIni" -u root --protocol=tcp -e "CREATE DATABASE IF NOT EXISTS medicalytics;"
    if ($LASTEXITCODE -ne 0) { throw "Could not create medicalytics database." }

    & $MysqlExe --defaults-file="$MyIni" -u root --protocol=tcp -e "CREATE USER IF NOT EXISTS 'medicalytics'@'localhost' IDENTIFIED BY 'medicalytics';"
    if ($LASTEXITCODE -ne 0) { throw "Could not create medicalytics database user." }

    & $MysqlExe --defaults-file="$MyIni" -u root --protocol=tcp -e "GRANT ALL PRIVILEGES ON medicalytics.* TO 'medicalytics'@'localhost'; FLUSH PRIVILEGES;"
    if ($LASTEXITCODE -ne 0) { throw "Could not grant database privileges." }

    New-Item -ItemType File -Force -Path $InitMarker | Out-Null
    Stop-Services
    $MysqldProcess = $null
}

Write-Host "Starting database and API..."
$MysqldProcess = Start-Process -FilePath $MysqldExe -ArgumentList @("--defaults-file=$MyIni") -WindowStyle Hidden -PassThru
Start-Sleep -Seconds 4

$backendArgs = @(
    "-jar", $BackendJar,
    "--spring.profiles.active=desktop",
    "-Dmedicalytics.data.dir=$DataDir"
)
$BackendProcess = Start-Process -FilePath $JavaExe -ArgumentList $backendArgs -WindowStyle Hidden -PassThru -WorkingDirectory (Split-Path $BackendJar -Parent)

Write-Host "Waiting for API..."
$healthy = $false
for ($i = 0; $i -lt 90; $i++) {
    try {
        $response = Invoke-WebRequest -Uri "http://127.0.0.1:8080/actuator/health" -UseBasicParsing -TimeoutSec 2
        if ($response.StatusCode -eq 200) {
            $healthy = $true
            break
        }
    } catch {
        if ($BackendProcess.HasExited) {
            throw "Backend stopped unexpectedly. Check logs in $LogsDir"
        }
        Start-Sleep -Seconds 2
    }
}

if (-not $healthy) {
    throw "API did not become ready in time."
}

Write-Host "Launching Medicalytics..."
$env:MEDICALYTICS_API_URL = "http://127.0.0.1:8080"
$DesktopProcess = Start-Process -FilePath $DesktopExe -PassThru
Wait-Process -Id $DesktopProcess.Id

Write-Host "Shutting down..."
Stop-Services
