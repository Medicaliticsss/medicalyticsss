#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$ROOT_DIR/frontend"
DIST_DIR="$ROOT_DIR/dist/desktop"
MVN="$ROOT_DIR/backend/mvnw"

if [[ ! -x "$MVN" ]]; then
  chmod +x "$MVN"
fi

if ! command -v java >/dev/null 2>&1; then
  echo "JDK 21+ is required to build the desktop app."
  exit 1
fi

JAVA_MAJOR="$(java -version 2>&1 | awk -F '[\".]' '/version/ {print $2; exit}')"
if [[ "$JAVA_MAJOR" -lt 21 ]]; then
  echo "JDK 21+ is required. Detected Java $JAVA_MAJOR."
  exit 1
fi

echo "Building standalone Medicalytics desktop app..."
cd "$FRONTEND_DIR"
"$MVN" -f pom.xml -B -DskipTests clean package

APP_IMAGE_DIR="$FRONTEND_DIR/target/dist/Medicalytics"
if [[ ! -d "$APP_IMAGE_DIR" ]]; then
  echo "Expected app image was not created at: $APP_IMAGE_DIR"
  exit 1
fi

rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"
cp -R "$APP_IMAGE_DIR" "$DIST_DIR/"

OS_NAME="$(uname -s)"
ARCH_NAME="$(uname -m)"
case "$ARCH_NAME" in
  x86_64|amd64) ARCH_TAG="x64" ;;
  aarch64|arm64) ARCH_TAG="arm64" ;;
  *) ARCH_TAG="$ARCH_NAME" ;;
esac

case "$OS_NAME" in
  Linux)
    ARCHIVE_NAME="medicalytics-desktop-linux-${ARCH_TAG}.tar.gz"
    tar -czf "$ROOT_DIR/dist/$ARCHIVE_NAME" -C "$DIST_DIR" Medicalytics
  ;;
  Darwin)
    ARCHIVE_NAME="medicalytics-desktop-macos-${ARCH_TAG}.tar.gz"
    tar -czf "$ROOT_DIR/dist/$ARCHIVE_NAME" -C "$DIST_DIR" Medicalytics
  ;;
  MINGW*|MSYS*|CYGWIN*)
    ARCHIVE_NAME="medicalytics-desktop-windows-${ARCH_TAG}.zip"
    if command -v powershell.exe >/dev/null 2>&1; then
      powershell.exe -NoProfile -Command "Compress-Archive -Path '$DIST_DIR/Medicalytics' -DestinationPath '$ROOT_DIR/dist/$ARCHIVE_NAME' -Force"
    else
      echo "PowerShell is required to create the Windows zip archive."
      exit 1
    fi
  ;;
  *)
    ARCHIVE_NAME="medicalytics-desktop-${OS_NAME}-${ARCH_TAG}.tar.gz"
    tar -czf "$ROOT_DIR/dist/$ARCHIVE_NAME" -C "$DIST_DIR" Medicalytics
  ;;
esac

echo
echo "Standalone desktop app ready:"
if [[ -x "$DIST_DIR/Medicalytics/bin/Medicalytics" ]]; then
  echo "  Run directly: $DIST_DIR/Medicalytics/bin/Medicalytics"
elif [[ -f "$DIST_DIR/Medicalytics/Medicalytics.exe" ]]; then
  echo "  Run directly: $DIST_DIR/Medicalytics/Medicalytics.exe"
else
  echo "  App folder: $DIST_DIR/Medicalytics"
fi
echo "  Archive:      $ROOT_DIR/dist/$ARCHIVE_NAME"
echo
echo "The app bundles its own Java runtime. Start the API first with ./scripts/start.sh or Docker."
