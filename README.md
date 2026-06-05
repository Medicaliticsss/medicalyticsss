# Medicalytics

Medical data analytics platform with a Spring Boot REST API, MariaDB warehouse, and JavaFX desktop client.

## Download for Windows

**[Download Medicalytics for Windows (latest release)](https://github.com/Medicaliticsss/medicalyticsss/releases/latest)**

Direct link: [medicalytics-windows-portable.zip](https://github.com/Medicaliticsss/medicalyticsss/releases/latest/download/medicalytics-windows-portable.zip)

### How to run

1. Download and extract the zip
2. Double-click **`Medicalytics.cmd`**
3. Wait 1–2 minutes on first launch (database setup)

No Java, Docker, or MariaDB installation required. Data is stored in `%LOCALAPPDATA%\Medicalytics`.

> The Windows package is built automatically on every push to `main` and published under [Releases](https://github.com/Medicaliticsss/medicalyticsss/releases).

## Development

See [docs/README.md](docs/README.md) for manual setup. Docker quick start:

```bash
docker compose up -d --build
cd frontend && ../backend/mvnw -Pdev javafx:run
```

### Build the Windows package locally

```bat
scripts\build-windows-package.bat
```

Output: `dist\medicalytics-windows-portable.zip`
