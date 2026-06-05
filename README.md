# Medicalytics

Medical data analytics platform with a Spring Boot REST API, MariaDB warehouse, and JavaFX desktop client.

## Standalone desktop app

Build a self-contained desktop application with a bundled Java runtime. No JDK or Maven is required to run it after building.

**Build once (requires JDK 21+):**

```bash
chmod +x scripts/*.sh backend/mvnw
./scripts/build-frontend.sh
```

Windows:

```bat
scripts\build-frontend.bat
```

**Run the desktop app:**

```bash
./scripts/launch-desktop.sh
```

Windows: `dist\desktop\Medicalytics\Medicalytics.exe`  
Linux/macOS: `dist/desktop/Medicalytics/bin/Medicalytics`

The build also creates an archive in `dist/` (`.tar.gz` on Linux/macOS, folder ready to zip on Windows).

> The desktop app still needs the API running. Use the Docker quick start below, or point it at an existing server with `MEDICALYTICS_API_URL`.

Pre-built packages for Linux, Windows, and macOS are produced by the [Build Desktop App](.github/workflows/build-desktop.yml) GitHub Actions workflow.

## Quick start (API + desktop)

Requirements:

- [Docker](https://docs.docker.com/get-docker/) with Compose
- For building the desktop app: JDK 21+

Start the API and launch the UI:

```bash
chmod +x scripts/*.sh
./scripts/build-frontend.sh   # first time only
./scripts/start.sh
```

This command:

1. Builds and starts MariaDB plus the backend API in Docker
2. Waits until the API is healthy on port `8080`
3. Launches the standalone desktop app

Stop services:

```bash
./scripts/stop.sh
```

Windows:

```bat
scripts\build-frontend.bat
scripts\start.bat
scripts\stop.bat
```

## What runs in Docker

`docker-compose.yml` starts:

| Service | Port | Purpose |
|---------|------|---------|
| `db` | internal `3306` | MariaDB with Flyway migrations |
| `backend` | `8080` | Spring Boot API, file uploads, ETL, reports |

Uploaded CSV files and database data are stored in Docker volumes, so they survive container restarts.

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `MEDICALYTICS_API_URL` | `http://localhost:8080` | API base URL used by the desktop client |
| `SPRING_DATASOURCE_*` | see `.env.example` | Database connection for the backend container |

Copy `.env.example` to `.env` if you want to customize database credentials before `docker compose up`.

## Manual development setup

For local development without Docker:

```bash
cd frontend
../backend/mvnw -Pdev javafx:run
```

See [docs/README.md](docs/README.md) for the full manual setup.

## Architecture

See [docs/architecture.md](docs/architecture.md).
