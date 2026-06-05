#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required. Install Docker Desktop or Docker Engine, then run this script again."
  exit 1
fi

COMPOSE_CMD=(docker compose)
if ! docker compose version >/dev/null 2>&1; then
  if command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_CMD=(docker-compose)
  else
    echo "Docker Compose is required."
    exit 1
  fi
fi

echo "Starting Medicalytics backend and database..."
"${COMPOSE_CMD[@]}" up -d --build

echo "Waiting for API at http://localhost:8080 ..."
for _ in $(seq 1 60); do
  if curl -fsS "http://localhost:8080/actuator/health" >/dev/null 2>&1; then
    echo "Backend is ready."
    break
  fi
  sleep 2
done

if ! curl -fsS "http://localhost:8080/actuator/health" >/dev/null 2>&1; then
  echo "Backend did not become healthy in time. Check logs with: ${COMPOSE_CMD[*]} logs backend"
  exit 1
fi

export MEDICALYTICS_API_URL="${MEDICALYTICS_API_URL:-http://localhost:8080}"

DESKTOP_BIN="$ROOT_DIR/dist/desktop/Medicalytics/bin/Medicalytics"
LEGACY_BIN="$ROOT_DIR/dist/frontend/bin/app"

if [[ -x "$DESKTOP_BIN" ]]; then
  echo "Launching standalone desktop app..."
  exec "$DESKTOP_BIN"
fi

if [[ -x "$LEGACY_BIN" ]]; then
  echo "Launching packaged desktop app..."
  exec "$LEGACY_BIN"
fi

if [[ -x "$ROOT_DIR/backend/mvnw" ]]; then
  echo "Launching desktop app with Maven..."
  cd "$ROOT_DIR/frontend"
  exec "$ROOT_DIR/backend/mvnw" -q -Pdev javafx:run
fi

echo
echo "Server stack is running."
echo "API: $MEDICALYTICS_API_URL"
echo
echo "To open the desktop app, either:"
echo "  1. Run ./scripts/build-frontend.sh once, then ./scripts/start.sh again"
echo "  2. Or launch only the UI with ./scripts/launch-desktop.sh"
