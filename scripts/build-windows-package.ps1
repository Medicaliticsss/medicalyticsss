$ErrorActionPreference = "Stop"

$RootDir = Split-Path $PSScriptRoot -Parent
$FrontendDir = Join-Path $RootDir "frontend"
$BackendDir = Join-Path $RootDir "backend"
$PackagingDir = Join-Path $RootDir "packaging\windows"
$DistDir = Join-Path $RootDir "dist\Medicalytics-Windows"
$RuntimeDir = Join-Path $DistDir "runtime"
$CacheDir = Join-Path $RootDir "dist\cache"
$Mvnw = Join-Path $BackendDir "mvnw.cmd"

$MariaDbVersion = "11.4.5"
$MariaDbZip = "mariadb-$MariaDbVersion-winx64.zip"
$MariaDbUrl = "https://archive.mariadb.org/mariadb-$MariaDbVersion/winx64-packages/$MariaDbZip"
$JreZip = "temurin-jre-17-windows-x64.zip"
$JreUrl = "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jre/hotspot/normal/eclipse?project=jdk"

function Ensure-Download($Url, $Destination) {
    if ($env:CI -eq "true" -and (Test-Path $Destination)) {
        Remove-Item $Destination -Force
    }
    if (-not (Test-Path $Destination)) {
        New-Item -ItemType Directory -Force -Path (Split-Path $Destination -Parent) | Out-Null
        Write-Host "Downloading $Url"
        Invoke-WebRequest -Uri $Url -OutFile $Destination -UseBasicParsing
    }
}

function Expand-ArchiveClean($ArchivePath, $DestinationPath) {
    if (Test-Path $DestinationPath) {
        Remove-Item $DestinationPath -Recurse -Force
    }
    Expand-Archive -Path $ArchivePath -DestinationPath $DestinationPath -Force
}

Write-Host "Building backend..."
Push-Location $BackendDir
& $Mvnw -B -DskipTests clean package
if ($LASTEXITCODE -ne 0) { throw "Backend build failed." }
Pop-Location

Write-Host "Building desktop app..."
Push-Location $FrontendDir
& $Mvnw -f pom.xml -B -DskipTests clean package
if ($LASTEXITCODE -ne 0) { throw "Desktop build failed." }
Pop-Location

$BackendJar = Get-ChildItem (Join-Path $BackendDir "target\backend-*.jar") | Where-Object { $_.Name -notmatch "original" } | Select-Object -First 1
$DesktopImage = Join-Path $FrontendDir "target\dist\Medicalytics"
if (-not (Test-Path $DesktopImage)) { throw "Desktop image not found at $DesktopImage" }

if (Test-Path $DistDir) { Remove-Item $DistDir -Recurse -Force }
New-Item -ItemType Directory -Force -Path $RuntimeDir | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $RuntimeDir "backend") | Out-Null

Ensure-Download $MariaDbUrl (Join-Path $CacheDir $MariaDbZip)
Ensure-Download $JreUrl (Join-Path $CacheDir $JreZip)

if ($env:CI -eq "true" -and (Test-Path $CacheDir)) {
    Remove-Item $CacheDir -Recurse -Force
}

Write-Host "Unpacking runtime dependencies..."
$mariadbExtractDir = Join-Path $CacheDir "mariadb-extract"
$jreExtractDir = Join-Path $CacheDir "jre-extract"
Expand-ArchiveClean (Join-Path $CacheDir $MariaDbZip) $mariadbExtractDir
$MariaDbExtracted = Get-ChildItem $mariadbExtractDir | Where-Object { $_.PSIsContainer } | Select-Object -First 1
if (-not $MariaDbExtracted) { throw "MariaDB archive has unexpected structure." }
Copy-Item -Path $MariaDbExtracted.FullName -Destination (Join-Path $RuntimeDir "mariadb") -Recurse

Expand-ArchiveClean (Join-Path $CacheDir $JreZip) $jreExtractDir
$JreExtracted = Get-ChildItem $jreExtractDir | Where-Object { $_.PSIsContainer } | Select-Object -First 1
if (-not $JreExtracted) { throw "JRE archive has unexpected structure." }
Copy-Item -Path $JreExtracted.FullName -Destination (Join-Path $RuntimeDir "jre") -Recurse

Copy-Item $BackendJar.FullName (Join-Path $RuntimeDir "backend\backend.jar")
Copy-Item $DesktopImage (Join-Path $RuntimeDir "desktop") -Recurse

Copy-Item (Join-Path $PackagingDir "Medicalytics.cmd") $DistDir
Copy-Item (Join-Path $PackagingDir "README.txt") $DistDir
New-Item -ItemType Directory -Force -Path (Join-Path $DistDir "scripts") | Out-Null
Copy-Item (Join-Path $PackagingDir "scripts\launch.ps1") (Join-Path $DistDir "scripts\launch.ps1")

$Archive = Join-Path $RootDir "dist\medicalytics-windows-portable.zip"
if (Test-Path $Archive) { Remove-Item $Archive -Force }
Compress-Archive -Path $DistDir -DestinationPath $Archive

Write-Host ""
Write-Host "Windows portable package ready:"
Write-Host "  Folder:  $DistDir"
Write-Host "  Archive: $Archive"
Write-Host ""
Write-Host "For end users: extract the zip and double-click Medicalytics.cmd"
