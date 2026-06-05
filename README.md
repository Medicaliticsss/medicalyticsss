# Medicalytics

Medicalytics is a desktop application for uploading medical CSV files, processing them into an analytical database, and building reports and charts from anonymized test results.

The system consists of a **JavaFX desktop client** and a **Spring Boot API** backed by **MariaDB**.

## Download (Windows)

**[Download the latest Windows package](https://github.com/Medicaliticsss/medicalyticsss/releases/latest)**

Direct link: [medicalytics-windows-portable.zip](https://github.com/Medicaliticsss/medicalyticsss/releases/latest/download/medicalytics-windows-portable.zip)

1. Extract the zip
2. Double-click **`Medicalytics.cmd`**
3. Wait 1–2 minutes on first launch (local database setup)

No Java, Docker, or MariaDB installation is required. User data is stored in `%LOCALAPPDATA%\Medicalytics`.

The package is built automatically on every push to `main` and published under [Releases](https://github.com/Medicaliticsss/medicalyticsss/releases).

## Basic workflow

```
Download → Launch → Register / Log in → Upload CSV → Process file → Reports → Settings
```

| Step | What you do | What happens |
|------|-------------|--------------|
| 1. **Start** | Run `Medicalytics.cmd` (or start API + desktop app in dev mode) | API and database start automatically in the portable package |
| 2. **Log in** | Register a new account or sign in | Session is created; actions are tied to your user |
| 3. **Files** | Upload a `.csv` file from the **Pliki** screen | File is stored on the server; status is `UPLOADED` |
| 4. **Preview** | Open a preview of the selected file | Server streams the first rows without loading the entire file |
| 5. **Process** | Click **Przetwórz plik** | ETL validates rows, anonymizes PESEL, loads results into the warehouse |
| 6. **Reports** | Open **Raporty** | Build charts and tables from processed data (BI / OLAP) |
| 7. **Settings** | Open **Ustawienia** | Change password, view account info, manage the test dictionary (MDM) |

Sample CSV files for testing are in the [`csv/`](csv/) folder.

## Documentation

- **[Application guide & API reference](docs/app.md)** — setup options, endpoints, CSV format, settings
- **[Architecture](docs/architecture.md)** — system design, components, data flows

## Development quick start

For contributors running from source:

```bash
docker compose up -d --build
cd frontend && ../backend/mvnw -Pdev javafx:run
```

See [docs/app.md](docs/app.md) for full development setup.
