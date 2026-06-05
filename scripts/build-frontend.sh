#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$ROOT_DIR/frontend"
DIST_DIR="$ROOT_DIR/dist/frontend"

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven is required to build the desktop app."
  exit 1
fi

echo "Building standalone desktop runtime..."
cd "$FRONTEND_DIR"
mvn -q -DskipTests clean javafx:jlink

rm -rf "$DIST_DIR"
mkdir -p "$ROOT_DIR/dist"
cp -R "$FRONTEND_DIR/target/app" "$DIST_DIR"

echo "Desktop app built at: $DIST_DIR/bin/app"
