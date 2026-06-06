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
$MariaDbErrorLog = Join-Path $LogsDir "mariadb-error.log"

function Convert-ToMariaDbPath {
    param([string]$Path)
    return $Path.Replace('\', '/')
}

$MysqlBasedir = Convert-ToMariaDbPath $MysqlDir
$MysqlDataDirMaria = Convert-ToMariaDbPath $MysqlDataDir
$PluginDir = Convert-ToMariaDbPath (Join-Path $MysqlDir "lib\plugin")
$MessagesDir = Convert-ToMariaDbPath (Join-Path $MysqlDir "share")
$MariaDbErrorLogMaria = Convert-ToMariaDbPath $MariaDbErrorLog

function Write-MyIni {
    $iniContent = @"
[mysqld]
basedir=$MysqlBasedir
datadir=$MysqlDataDirMaria
plugin_dir=$PluginDir
lc-messages-dir=$MessagesDir
log-error=$MariaDbErrorLogMaria
port=3307
bind-address=127.0.0.1
max_allowed_packet=64M
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci

[client]
host=127.0.0.1
port=3307
"@
    Set-Content -Path $MyIni -Value $iniContent -Encoding ASCII
}

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

function Get-MariaDbErrorTail {
    param([int]$Lines = 20)
    if (Test-Path $MariaDbErrorLog) {
        $tail = (Get-Content $MariaDbErrorLog -Tail $Lines -ErrorAction SilentlyContinue) -join "`n"
        if ($tail) { return "MariaDB log:`n$tail" }
    }
    return "MariaDB log not found at $MariaDbErrorLog"
}

function Invoke-MysqlTcp {
    param(
        [string]$Sql,
        [string]$User = "root",
        [string]$Password = "",
        [string]$HostName = "127.0.0.1"
    )

    $args = @(
        "--defaults-file=$MyIni",
        "--protocol=tcp",
        "--skip-ssl",
        "-h", $HostName,
        "-P", "3307",
        "-u", $User,
        "-e", $Sql
    )
    if ($Password) {
        $args += "-p$Password"
    }

    $output = & $MysqlExe @args 2>&1
    if ($LASTEXITCODE -ne 0) {
        $details = ($output | Out-String).Trim()
        if ($details) {
            throw "MariaDB command failed: $details`n`n$(Get-MariaDbErrorTail)"
        }
        throw "MariaDB command failed.`n`n$(Get-MariaDbErrorTail)"
    }
}

function Invoke-MysqlAdmin {
    param([string]$Sql)
    $lastError = $null
    foreach ($hostName in @("127.0.0.1", "localhost")) {
        try {
            Invoke-MysqlTcp -Sql $Sql -User "root" -HostName $hostName
            return
        } catch {
            $lastError = $_
        }
    }
    throw $lastError
}

function Ensure-DatabaseUsers {
    Invoke-MysqlAdmin @"
CREATE DATABASE IF NOT EXISTS medicalytics;
CREATE USER IF NOT EXISTS 'medicalytics'@'localhost' IDENTIFIED BY 'medicalytics';
CREATE USER IF NOT EXISTS 'medicalytics'@'127.0.0.1' IDENTIFIED BY 'medicalytics';
GRANT ALL PRIVILEGES ON medicalytics.* TO 'medicalytics'@'localhost';
GRANT ALL PRIVILEGES ON medicalytics.* TO 'medicalytics'@'127.0.0.1';
FLUSH PRIVILEGES;
"@
}

function Start-MariaDbServer {
    return Start-Process -FilePath $MysqldExe `
        -ArgumentList @("--defaults-file=$MyIni", "--datadir=$MysqlDataDirMaria") `
        -WindowStyle Hidden `
        -PassThru `
        -WorkingDirectory $MysqlDir
}

function Test-MariaDbPortOpen {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $client.Connect("127.0.0.1", 3307)
        $open = $client.Connected
        $client.Close()
        return $open
    } catch {
        return $false
    }
}

function Wait-MariaDbReady {
    Write-Host "Waiting for database..."
    for ($i = 0; $i -lt 45; $i++) {
        if ($MysqldProcess -and $MysqldProcess.HasExited) {
            throw "MariaDB stopped during startup.`n`n$(Get-MariaDbErrorTail)"
        }

        if (Test-MariaDbPortOpen) {
            return
        }

        Start-Sleep -Seconds 2
    }
    throw "MariaDB did not become ready.`n`n$(Get-MariaDbErrorTail)"
}

function Test-MedicalyticsDbConnection {
    try {
        Invoke-MysqlTcp -Sql "SELECT 1" -User "medicalytics" -Password "medicalytics" | Out-Null
    } catch {
        throw "API database account cannot connect to 127.0.0.1:3307. Delete `"$DataDir`" and try again.`n`n$($_.Exception.Message)"
    }
}

function Test-ApiPortOpen {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $client.Connect("127.0.0.1", 8080)
        $open = $client.Connected
        $client.Close()
        return $open
    } catch {
        return $false
    }
}

function Test-ApiHealthy {
    try {
        $response = Invoke-WebRequest -Uri "http://127.0.0.1:8080/actuator/health" -UseBasicParsing -TimeoutSec 3
        return ($response.StatusCode -eq 200) -and ($response.Content -match '"status"\s*:\s*"UP"')
    } catch {
        $webResponse = $_.Exception.Response
        if ($webResponse) {
            try {
                $reader = New-Object System.IO.StreamReader($webResponse.GetResponseStream())
                $body = $reader.ReadToEnd()
                if ($body -match '"status"\s*:\s*"UP"') { return $true }
            } catch {
            }
        }
        return $false
    }
}

function Wait-ForApi {
    Write-Host "Waiting for API..."
    $portOpenSince = $null
    for ($i = 0; $i -lt 120; $i++) {
        if ($BackendProcess.HasExited) {
            throw "Backend stopped unexpectedly.`n`nLog tail:`n$(Get-BackendLogTail)"
        }

        if (Test-ApiHealthy) {
            return
        }

        if (Test-ApiPortOpen) {
            if (-not $portOpenSince) {
                $portOpenSince = Get-Date
            } elseif (((Get-Date) - $portOpenSince).TotalSeconds -ge 8) {
                Write-Host "API port is open and backend is running."
                return
            }
        } else {
            $portOpenSince = $null
        }

        Start-Sleep -Seconds 2
    }

    throw "API did not become ready in time.`n`nLog tail:`n$(Get-BackendLogTail)`n`nFull log: $BackendLog"
}

function Get-BackendLogTail {
    param([int]$Lines = 30)
    if (Test-Path $BackendLog) {
        $tail = (Get-Content $BackendLog -Tail $Lines -ErrorAction SilentlyContinue) -join "`n"
        if ($tail) { return $tail }
    }
    return "(no backend log written yet)"
}

function Clear-FileMotw {
    param([string]$FilePath)
    if (-not (Test-Path -LiteralPath $FilePath)) { return }
    Unblock-File -LiteralPath $FilePath -ErrorAction SilentlyContinue
    $zonePath = "${FilePath}:Zone.Identifier"
    if (Test-Path -LiteralPath $zonePath) {
        Remove-Item -LiteralPath $zonePath -Force -ErrorAction SilentlyContinue
    }
}

function Unblock-PortableAppFiles {
    foreach ($path in @($InstallRoot, $DataDir)) {
        if (-not (Test-Path $path)) { continue }
        Get-ChildItem -Path $path -Recurse -File -ErrorAction SilentlyContinue | ForEach-Object {
            Clear-FileMotw $_.FullName
        }
    }
}

function Unblock-RuntimeExecutables {
    foreach ($exe in @($JavaExe, $MysqldExe, $MysqlExe, $MysqlInstallDbExe, $DesktopExe)) {
        Clear-FileMotw $exe
    }
}

function Start-DesktopApp {
    $DesktopDir = Split-Path $DesktopExe -Parent
    Unblock-PortableAppFiles
    Unblock-RuntimeExecutables

    if (Test-Path $DesktopDir) {
        Get-ChildItem -Path $DesktopDir -Recurse -File -ErrorAction SilentlyContinue | ForEach-Object {
            Clear-FileMotw $_.FullName
        }
    }

    $env:MEDICALYTICS_API_URL = "http://127.0.0.1:8080"

    try {
        $process = Start-Process -FilePath $DesktopExe -WorkingDirectory $DesktopDir -PassThru -ErrorAction Stop
        Start-Sleep -Milliseconds 750
        if ($process.HasExited) {
            throw "Desktop app exited immediately (exit code $($process.ExitCode))."
        }
        return $process
    } catch {
        throw @"
Failed to start desktop app: $($_.Exception.Message)

If you downloaded the zip from the internet, Windows may be blocking it.
Right-click the extracted folder -> Properties -> check 'Unblock' -> Apply, then try again.

Desktop app: $DesktopExe
"@
    }
}

$MysqldProcess = $null
$BackendProcess = $null

function Stop-Services {
    if ($BackendProcess -and -not $BackendProcess.HasExited) {
        Stop-Process -Id $BackendProcess.Id -Force -ErrorAction SilentlyContinue
    }
    if ($MysqldProcess -and -not $MysqldProcess.HasExited) {
        Stop-Process -Id $MysqldProcess.Id -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
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

Write-MyIni
Unblock-PortableAppFiles
Unblock-RuntimeExecutables

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

    Write-MyIni

    $installProcess = Start-Process -FilePath $MysqlInstallDbExe -ArgumentList @(
        "--datadir=$MysqlDataDir",
        "-P", "3307",
        "-R"
    ) -Wait -PassThru -NoNewWindow -WorkingDirectory $MysqlDir
    if ($installProcess.ExitCode -ne 0) {
        throw "Database initialization failed (exit $($installProcess.ExitCode)). Delete `"$DataDir`" and try again."
    }

    $MysqldProcess = Start-MariaDbServer
    Wait-MariaDbReady
    Ensure-DatabaseUsers

    New-Item -ItemType File -Force -Path $InitMarker | Out-Null
    Stop-Services
    $MysqldProcess = $null
}

Write-Host "Starting database and API..."
Write-MyIni
$MysqldProcess = Start-MariaDbServer
Wait-MariaDbReady
Ensure-DatabaseUsers
Test-MedicalyticsDbConnection

$DataDirForJvm = $DataDir.Replace('\', '/')
$BackendLog = Join-Path $LogsDir "backend.log"
if (Test-Path $BackendLog) { Remove-Item $BackendLog -Force }

$backendArgs = @(
    "-Dmedicalytics.data.dir=$DataDirForJvm",
    "-Dlogging.file.name=$DataDirForJvm/logs/backend.log",
    "-jar", $BackendJar,
    "--spring.profiles.active=desktop",
    "--medicalytics.data.dir=$DataDirForJvm"
)
$BackendProcess = Start-Process -FilePath $JavaExe -ArgumentList $backendArgs -WindowStyle Hidden -PassThru `
    -WorkingDirectory (Split-Path $BackendJar -Parent)

Wait-ForApi

Write-Host "Launching Medicalytics..."
$DesktopProcess = Start-DesktopApp
Wait-Process -Id $DesktopProcess.Id

Write-Host "Shutting down..."
Stop-Services
