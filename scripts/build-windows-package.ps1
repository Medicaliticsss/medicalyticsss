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

function Ensure-Download($Url, $Destination, [int]$MinSizeBytes = 1MB) {
    $parent = Split-Path $Destination -Parent
    New-Item -ItemType Directory -Force -Path $parent | Out-Null

    if (Test-Path $Destination) {
        Remove-Item $Destination -Force
    }

    Write-Host "Downloading $Url"
    if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
        curl.exe -fL --retry 3 --retry-delay 5 -o $Destination $Url
        if ($LASTEXITCODE -ne 0) {
            throw "Download failed for $Url"
        }
    } else {
        Invoke-WebRequest -Uri $Url -OutFile $Destination -UseBasicParsing
    }

    if (-not (Test-Path $Destination)) {
        throw "Download did not create file: $Destination"
    }

    $size = (Get-Item $Destination).Length
    if ($size -lt $MinSizeBytes) {
        throw "Downloaded file is too small ($size bytes): $Destination"
    }

    Write-Host "Saved $Destination ($([math]::Round($size / 1MB, 1)) MB)"
}

function Expand-ArchiveClean($ArchivePath, $DestinationPath) {
    if (-not (Test-Path $ArchivePath)) {
        throw "Archive not found: $ArchivePath"
    }

    if (Test-Path $DestinationPath) {
        Remove-Item $DestinationPath -Recurse -Force
    }

    Expand-Archive -Path $ArchivePath -DestinationPath $DestinationPath -Force
}

if ($env:CI -eq "true" -and (Test-Path $CacheDir)) {
    Remove-Item $CacheDir -Recurse -Force
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

$mariadbArchive = Join-Path $CacheDir $MariaDbZip
$jreArchive = Join-Path $CacheDir $JreZip

Ensure-Download $MariaDbUrl $mariadbArchive 50MB
Ensure-Download $JreUrl $jreArchive 30MB

Write-Host "Unpacking runtime dependencies..."
$mariadbExtractDir = Join-Path $CacheDir "mariadb-extract"
$jreExtractDir = Join-Path $CacheDir "jre-extract"
Expand-ArchiveClean $mariadbArchive $mariadbExtractDir
$MariaDbExtracted = Get-ChildItem $mariadbExtractDir | Where-Object { $_.PSIsContainer } | Select-Object -First 1
if (-not $MariaDbExtracted) { throw "MariaDB archive has unexpected structure." }
Copy-Item -Path $MariaDbExtracted.FullName -Destination (Join-Path $RuntimeDir "mariadb") -Recurse

Expand-ArchiveClean $jreArchive $jreExtractDir
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
