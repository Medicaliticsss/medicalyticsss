# Medicalytics — Application Guide & API Reference

This document covers how to run Medicalytics in different environments and describes the REST API exposed by the backend.

The desktop client requires a running API. In the **Windows portable package**, the API and database start automatically. In **development mode**, start the backend before the frontend.

---

## Running the application

### Option A: Windows portable package (end users)

1. Download from [GitHub Releases](https://github.com/Medicaliticsss/medicalyticsss/releases/latest)
2. Extract the zip
3. Double-click **`Medicalytics.cmd`**

No Java, Docker, or MariaDB installation is required.

### Option B: Docker (API + database)

From the repository root:

```bash
docker compose up -d --build
```

This starts MariaDB and the Spring Boot API on port `8080`. Then launch the desktop client:

```bash
cd frontend
../backend/mvnw -Pdev javafx:run
```

Or use the standalone desktop build if you have one:

```bash
scripts/launch-desktop.sh    # Linux/macOS
scripts\launch-desktop.bat   # Windows
```

### Option C: Local development (IntelliJ / Maven)

**Requirements:** JDK 21+ (frontend), JDK 17+ (backend), MariaDB

1. Create the database:

```sql
DROP DATABASE IF EXISTS medicalytics;
CREATE DATABASE medicalytics;
```

2. Configure `backend/src/main/resources/application.properties` for your MariaDB connection.

3. Start the backend:

```bash
cd backend
./mvnw spring-boot:run
```

On startup, **Flyway** applies migrations and **DictionarySeeder** loads the test dictionary from `backend/src/main/resources/dictionaries/test-types.json`.

4. Start the frontend:

```bash
cd frontend
../backend/mvnw -Pdev javafx:run
```

### Building the Windows portable package

```bat
scripts\build-windows-package.bat
```

Output: `dist\medicalytics-windows-portable.zip`

---

## Desktop application screens

| Screen | Purpose |
|--------|---------|
| **Login / Register** | User authentication |
| **Pliki** | Upload, preview, process, and delete CSV files |
| **Raporty** | Build BI reports, charts, and export CSV/PNG |
| **Ustawienia** | Account info, password change, test dictionary (MDM) |

---

## Settings module

Available under **Ustawienia** in the desktop app.

### Account
- View logged-in username and session status
- Change password

### Test dictionary (MDM)
- Browse and search the authoritative test catalog (`dim_test_type`)
- Edit norms and metadata for a test code
- Export dictionary as JSON
- Import dictionary from JSON (upserts all entries)

The dictionary loaded at server startup is the source of truth for anomaly detection. CSV `norma_min`, `norma_max`, and `jednostka` values do **not** override the dictionary.

Custom dictionary file at startup (server config):

```bash
java -jar backend.jar --dictionary.tests.path=file:/path/to/custom-test-types.json
```

JSON format: array of objects with fields `testCode`, `testName`, `categoryName`, `unit`, `normMin`, `normMax`.

---

## API reference

Base URL: `http://localhost:8080`

All endpoints except `/api/auth/login` and `/api/auth/register` require an active session (cookie-based).

### Authentication (`/api/auth`)

Form-encoded requests (`application/x-www-form-urlencoded`). Passwords are stored as BCrypt hashes.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Register (`username`, `password`) |
| `POST` | `/api/auth/login` | Log in (`username`, `password`) |
| `GET` | `/api/auth/me` | Current session info |
| `POST` | `/api/auth/logout` | End session |

### Files & ETL (`/api/files`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/files` | List uploaded files (`id`, `fileName`, `status`, `uploadTime`) |
| `POST` | `/api/files/upload` | Upload CSV (`multipart/form-data`, field `file`) |
| `GET` | `/api/files/{id}/preview` | Stream first rows (`?limit=50` optional) |
| `POST` | `/api/files/{id}/process` | Run ETL on file |
| `POST` | `/api/files/{id}/delete` | Soft-delete file and roll back related data |
| `GET` | `/api/files/{id}/errors` | Processing errors for a file |

**Upload behaviour:**
- Files are stored in `uploads/` on the server
- Name collisions are resolved automatically (e.g. `file(1).csv`)
- A `files_history` record is created with status `UPLOADED`

**ETL behaviour:**
- Strict validation: all 15 CSV columns required per row
- PESEL is hashed with SHA-256
- Facility names, cities, and provinces are normalized (Title Case)
- Anomaly flags use dictionary norms only, not CSV norms
- File status becomes `SUCCESS`, `PARTIAL_SUCCESS`, or `ERROR`

### Reports (`/api/reports`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/reports/summary` | Global KPIs (`totalTests`, `normalResults`, `abnormalResults`) |
| `POST` | `/api/reports/custom` | Chart data (aggregated `ReportDataPoint` list) |
| `POST` | `/api/reports/custom/table` | Dynamic table rows (optional aggregation) |
| `POST` | `/api/reports/series` | Multi-series chart data (`x`, `series`, `value`) |
| `POST` | `/api/reports/raw` | Raw drill-down rows (max 500, filters only) |

Report requests use `CustomReportRequest` or `SeriesReportRequest` JSON bodies with whitelisted columns, operations (`COUNT`, `SUM`, `AVG`, etc.), and filters (`IN`, `BETWEEN`, etc.).

### Settings (`/api/settings`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `PUT` | `/api/settings/password` | Change password (`oldPassword`, `newPassword` JSON body) |
| `GET` | `/api/settings/dictionary` | List test dictionary entries |
| `GET` | `/api/settings/dictionary/export` | Export dictionary as JSON |
| `PUT` | `/api/settings/dictionary/{testCode}` | Update one dictionary entry |
| `POST` | `/api/settings/dictionary/import` | Import/sync dictionary from JSON array |

---

## CSV file format

Rows must be comma-separated with **all 15 fields** in this exact order:

```
imie, nazwisko, data_urodzenia, plec, pesel, placowka_nazwa, miasto, wojewodztwo, kod_badania, nazwa_badania, kategoria_badania, wynik, jednostka, norma_min, norma_max
```

| Field | Description |
|-------|-------------|
| `imie` | First name |
| `nazwisko` | Last name |
| `data_urodzenia` | Date of birth |
| `plec` | Gender |
| `pesel` | National ID (hashed on import) |
| `placowka_nazwa` | Facility name |
| `miasto` | City |
| `wojewodztwo` | Province |
| `kod_badania` | Test code |
| `nazwa_badania` | Test name |
| `kategoria_badania` | Test category |
| `wynik` | Result value |
| `jednostka` | Unit (not used for MDM) |
| `norma_min` | Min norm from file (not used for MDM) |
| `norma_max` | Max norm from file (not used for MDM) |

Missing fields in a row cause that row to be rejected and logged in `processing_errors`. The file may end up with status `PARTIAL_SUCCESS`.

Sample files: [`csv/`](../csv/)

---

## Health check

```bash
curl http://localhost:8080/actuator/health
```

---

## API test files

HTTP examples for manual testing:

- [`backend/api-tests/test_logowania.http`](../backend/api-tests/test_logowania.http)
- [`backend/api-tests/test_sesji.http`](../backend/api-tests/test_sesji.http)
- [`backend/api-tests/test_dictionary.http`](../backend/api-tests/test_dictionary.http)
