# Medicalytics

Medical data analytics platform with a Spring Boot REST API, MariaDB warehouse, and JavaFX desktop client.

## Windows portable package (one-click)

No Java, Docker, or MariaDB installation required.

1. Extract the zip
2. Double-click **`Medicalytics.cmd`**
3. Wait 1–2 minutes on first launch

Data is stored in `%LOCALAPPDATA%\Medicalytics`.

### Download from GitHub Actions

1. Open **Actions** → **Build Windows Portable Package**
2. Click **Run workflow** → branch `main` → **Run workflow**
3. When the run finishes, download artifact **medicalytics-windows-portable**
4. Extract and run **`Medicalytics.cmd`**

Artifacts are kept for 90 days. Pushes to `main` and tags `v*` also trigger builds automatically.

### Build locally

```bat
scripts\build-windows-package.bat
```

Output: `dist\medicalytics-windows-portable.zip`

## Development

See [docs/README.md](docs/README.md) for manual setup. Docker quick start:

```bash
docker compose up -d --build
cd frontend && ../backend/mvnw -Pdev javafx:run
```
