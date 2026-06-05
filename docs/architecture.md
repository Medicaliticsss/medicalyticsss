# Medicalytics Architecture

Medicalytics uses a classic **client–server** architecture: a JavaFX desktop client communicates with a Spring Boot REST API backed by a MariaDB data warehouse.

The system combines **authentication**, **ETL**, **Master Data Management (MDM)**, **file management**, and a **dynamic BI / OLAP reporting engine**.

---

## System components

### 1. Frontend (desktop client)

| Aspect | Detail |
|--------|--------|
| Technology | JavaFX 21, AtlantaFX theme |
| Pattern | MVC with separate views, services, and models |
| Networking | Async `HttpClient` with cookie-based sessions |
| Serialization | Gson |
| Navigation | `ViewManager` routes between screens |

**Views:** Login, Register, Main Menu, Files (Dashboard), Reports, Settings

**Services:** `AuthService`, `FileService`, `ReportService`, `SettingsService`

**Key UI capabilities:**
- Dynamic data grids and chart types (bar, pie, line, scatter)
- CSV and PNG export
- Settings: account management and MDM dictionary editor

### 2. Backend (REST API)

| Aspect | Detail |
|--------|--------|
| Technology | Java 17, Spring Boot 4.x |
| Security | Spring Security, BCrypt passwords, HTTP sessions |
| Migrations | Flyway |
| Persistence | Spring Data JPA |

**Modules:**

- **Authentication** — register, login, session management
- **File management** — upload with collision handling, streaming preview
- **ETL** — `CsvProcessingService` with strict row validation
- **MDM** — `DictionaryService` + `DictionarySeeder` for test norms
- **BI engine** — `CustomReportService` using JPA Criteria API with whitelist enums (SQL injection safe)
- **Settings** — password change, dictionary CRUD/import/export

### 3. Database (star schema)

| Aspect | Detail |
|--------|--------|
| Engine | MariaDB |
| Migrations | Flyway (`backend/src/main/resources/db/migration/`) |

**Tables:**

| Table | Role |
|-------|------|
| `users` | Application accounts |
| `files_history` | Uploaded file lifecycle (`UPLOADED` → `SUCCESS` / `PARTIAL_SUCCESS` / `ERROR` → `DELETED`) |
| `fact_test_results` | Central fact table (test results linked to dimensions) |
| `dim_patient` | Patient dimension (hashed PESEL, birth year, gender) |
| `dim_facility` | Facility dimension (normalized name, city, province) |
| `dim_test_type` | MDM dictionary — authoritative test codes and norms |
| `processing_errors` | Per-row ETL rejection logs |

---

## Deployment models

### Windows portable package

Bundles MariaDB, JRE, backend JAR, and desktop app. A launcher script (`Medicalytics.cmd`) starts all components and opens the UI.

User data: `%LOCALAPPDATA%\Medicalytics`

### Docker Compose

Runs MariaDB + backend API in containers. The desktop client runs on the host and connects to `http://localhost:8080`.

### Local development

Backend and MariaDB run natively; frontend launched via Maven `javafx:run`.

---

## Data flows

### Authentication

```
User → LoginView → AuthService → POST /api/auth/login
     → Backend validates BCrypt hash → HTTP session (JSESSIONID cookie)
     → UserSession stores username locally
```

### CSV lifecycle

```
Upload → files_history (UPLOADED) + file on disk
Preview → stream first N rows from disk
Process → validate → hash PESEL → normalize text → check norms (MDM)
        → insert fact_test_results / processing_errors
        → update file status (SUCCESS | PARTIAL_SUCCESS | ERROR)
Delete  → soft-delete file, remove facts and errors, keep dimensions
```

### ETL rules

1. **Strict validation** — all 15 CSV fields required per row
2. **Anonymization** — PESEL → SHA-256 hash in `dim_patient`
3. **Text normalization** — Title Case for facility/city/province
4. **MDM** — anomaly detection uses `dim_test_type` norms only; CSV norm columns are ignored
5. **Upsert dimensions** — patients and facilities created or updated transactionally

### Reporting (OLAP)

```
ReportView → ReportService → POST /api/reports/*
          → CustomReportService (Criteria API)
          → MariaDB aggregation
          → JSON response → dynamic table columns / charts
```

The BI engine:
- Resolves required JOINs automatically
- Uses whitelist enums for columns, operations, and filter operators
- Performs aggregation in the database (not in application memory)
- Limits raw drill-down to 500 rows

### Settings / MDM dictionary

```
Startup → DictionarySeeder loads test-types.json → dim_test_type
Settings UI → SettingsService → /api/settings/dictionary
           → DictionaryService validates and syncs entries
```

Import updates existing codes and adds new ones. The dictionary is the single source of truth for medical norms.

---

## Security model

| Concern | Approach |
|---------|----------|
| Passwords | BCrypt hashing |
| Sessions | HTTP session cookies |
| API access | Authenticated except login/register |
| SQL injection | Whitelist-based Criteria API |
| Sensitive data | PESEL hashed; not stored in plain text |
| Health endpoint | `/actuator/health` public for monitoring |

---

## Build & distribution pipeline

| Output | How it is produced |
|--------|-------------------|
| Backend JAR | Maven `package` in `backend/` |
| Desktop app image | Maven `jpackage` in `frontend/` |
| Windows portable zip | `scripts/build-windows-package.ps1` |
| GitHub Release | CI workflow on push to `main` |
