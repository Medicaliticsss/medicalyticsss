# Medicalytics

Medical data analytics platform with a Spring Boot REST API, MariaDB warehouse, and JavaFX desktop client.

## Quick start (recommended)

Requirements:

- [Docker](https://docs.docker.com/get-docker/) with Compose
- For the desktop UI: JDK 23 + Maven, or run `./scripts/build-frontend.sh` once to create a standalone app

Start everything:

```bash
chmod +x scripts/*.sh
./scripts/start.sh
```

This command:

1. Builds and starts MariaDB plus the backend API in Docker
2. Waits until the API is healthy on port `8080`
3. Launches the desktop app if a packaged build exists, otherwise runs it with Maven

Stop services:

```bash
./scripts/stop.sh
```

Windows:

```bat
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

## Desktop app packaging

Build a self-contained JavaFX runtime (no Maven needed on later launches):

```bash
./scripts/build-frontend.sh
./scripts/start.sh
```

The packaged app is created at `dist/frontend/bin/app`.

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `MEDICALYTICS_API_URL` | `http://localhost:8080` | API base URL used by the desktop client |
| `SPRING_DATASOURCE_*` | see `.env.example` | Database connection for the backend container |

Copy `.env.example` to `.env` if you want to customize database credentials before `docker compose up`.

## Manual development setup

For local development without Docker, see [docs/README.md](docs/README.md).

## Architecture

See [docs/architecture.md](docs/architecture.md).
