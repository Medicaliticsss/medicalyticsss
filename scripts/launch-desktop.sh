#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export MEDICALYTICS_API_URL="${MEDICALYTICS_API_URL:-http://localhost:8080}"

DESKTOP_BIN="$ROOT_DIR/dist/desktop/Medicalytics/bin/Medicalytics"
LEGACY_BIN="$ROOT_DIR/dist/frontend/bin/app"

if [[ -x "$DESKTOP_BIN" ]]; then
  exec "$DESKTOP_BIN"
fi

if [[ -x "$LEGACY_BIN" ]]; then
  exec "$LEGACY_BIN"
fi

echo "Standalone desktop app not found."
echo "Build it once with: ./scripts/build-frontend.sh"
exit 1
