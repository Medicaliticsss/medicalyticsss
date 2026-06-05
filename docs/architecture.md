# Architektura Medicalytics

Medicalytics opiera się na klasycznej architekturze **klient–serwer**: desktopowy klient JavaFX komunikuje się z REST API Spring Boot, które korzysta z hurtowni danych MariaDB.

System łączy **autoryzację**, **ETL**, **Master Data Management (MDM)**, **zarządzanie plikami** oraz **dynamiczny silnik raportowy BI / OLAP**.

---

## Komponenty systemu

### 1. Frontend (klient desktopowy)

| Aspekt | Szczegóły |
|--------|-----------|
| Technologia | JavaFX 21, motyw AtlantaFX |
| Wzorzec | MVC z oddzielnymi widokami, serwisami i modelami |
| Sieć | Asynchroniczny `HttpClient` z sesjami opartymi na ciasteczkach |
| Serializacja | Gson |
| Nawigacja | `ViewManager` przełącza ekrany |

**Widoki:** Logowanie, Rejestracja, Menu główne, Pliki (Dashboard), Raporty, Ustawienia

**Serwisy:** `AuthService`, `FileService`, `ReportService`, `SettingsService`

**Kluczowe możliwości UI:**
- Dynamiczne tabele i typy wykresów (słupkowy, kołowy, liniowy, punktowy)
- Eksport CSV i PNG
- Ustawienia: zarządzanie kontem i edycja słownika MDM

### 2. Backend (REST API)

| Aspekt | Szczegóły |
|--------|-----------|
| Technologia | Java 17, Spring Boot 4.x |
| Bezpieczeństwo | Spring Security, hasła BCrypt, sesje HTTP |
| Migracje | Flyway |
| Persystencja | Spring Data JPA |

**Moduły:**

- **Autoryzacja** — rejestracja, logowanie, zarządzanie sesją
- **Zarządzanie plikami** — upload z obsługą kolizji nazw, strumieniowy podgląd
- **ETL** — `CsvProcessingService` ze ścisłą walidacją wierszy
- **MDM** — `DictionaryService` + `DictionarySeeder` dla norm badań
- **Silnik BI** — `CustomReportService` oparty na JPA Criteria API z whitelistą enumów (ochrona przed SQL injection)
- **Ustawienia** — zmiana hasła, CRUD słownika, import/eksport

### 3. Baza danych (model gwiazdy)

| Aspekt | Szczegóły |
|--------|-----------|
| Silnik | MariaDB |
| Migracje | Flyway (`backend/src/main/resources/db/migration/`) |

**Tabele:**

| Tabela | Rola |
|--------|------|
| `users` | Konta użytkowników |
| `files_history` | Cykl życia pliku (`UPLOADED` → `SUCCESS` / `PARTIAL_SUCCESS` / `ERROR` → `DELETED`) |
| `fact_test_results` | Centralna tabela faktów (wyniki badań powiązane z wymiarami) |
| `dim_patient` | Wymiar pacjenta (zhashowany PESEL, rok urodzenia, płeć) |
| `dim_facility` | Wymiar placówki (znormalizowana nazwa, miasto, województwo) |
| `dim_test_type` | Słownik MDM — autorytatywne kody badań i normy |
| `processing_errors` | Logi odrzuconych wierszy ETL |

---

## Modele wdrożenia

### Pakiet przenośny na Windows

Zawiera MariaDB, JRE, JAR backendu i aplikację desktopową. Skrypt uruchomieniowy (`Medicalytics.cmd`) startuje wszystkie komponenty i otwiera interfejs.

Dane użytkownika: `%LOCALAPPDATA%\Medicalytics`

### Docker Compose

Uruchamia MariaDB i API backendu w kontenerach. Klient desktopowy działa na hoście i łączy się z `http://localhost:8080`.

### Lokalny development

Backend i MariaDB działają natywnie; frontend uruchamiany przez Maven `javafx:run`.

---

## Przepływy danych

### Autoryzacja

```
Użytkownik → LoginView → AuthService → POST /api/auth/login
         → Backend weryfikuje hash BCrypt → sesja HTTP (ciasteczko JSESSIONID)
         → UserSession przechowuje nazwę użytkownika lokalnie
```

### Cykl życia pliku CSV

```
Upload → files_history (UPLOADED) + plik na dysku
Podgląd → strumieniowanie pierwszych N wierszy z dysku
Process → walidacja → hash PESEL → normalizacja tekstu → sprawdzenie norm (MDM)
        → zapis do fact_test_results / processing_errors
        → aktualizacja statusu pliku (SUCCESS | PARTIAL_SUCCESS | ERROR)
Delete  → soft-delete pliku, usunięcie faktów i błędów, zachowanie wymiarów
```

### Reguły ETL

1. **Ścisła walidacja** — wymagane wszystkie 15 pól CSV w każdym wierszu
2. **Anonimizacja** — PESEL → hash SHA-256 w `dim_patient`
3. **Normalizacja tekstu** — Title Case dla placówki/miasta/województwa
4. **MDM** — wykrywanie anomalii korzysta wyłącznie z norm w `dim_test_type`; kolumny norm z CSV są ignorowane
5. **Upsert wymiarów** — pacjenci i placówki tworzeni lub aktualizowani transakcyjnie

### Raportowanie (OLAP)

```
ReportView → ReportService → POST /api/reports/*
           → CustomReportService (Criteria API)
           → agregacja w MariaDB
           → odpowiedź JSON → dynamiczne kolumny tabeli / wykresy
```

Silnik BI:
- Automatycznie rozwiązuje wymagane JOIN-y
- Używa enumów z whitelisty dla kolumn, operacji i operatorów filtrów
- Wykonuje agregację w bazie danych (nie w pamięci aplikacji)
- Ogranicza surowy drill-down do 500 wierszy

### Ustawienia / słownik MDM

```
Start → DictionarySeeder ładuje test-types.json → dim_test_type
UI Ustawienia → SettingsService → /api/settings/dictionary
              → DictionaryService waliduje i synchronizuje wpisy
```

Import aktualizuje istniejące kody i dodaje nowe. Słownik jest jedynym źródłem prawdy dla norm medycznych.

---

## Model bezpieczeństwa

| Obszar | Podejście |
|--------|----------|
| Hasła | Hashowanie BCrypt |
| Sesje | Ciasteczka sesji HTTP |
| Dostęp do API | Wymaga autoryzacji poza loginem/rejestracją |
| SQL injection | Criteria API z whitelistą |
| Dane wrażliwe | PESEL hashowany; nie przechowywany w postaci jawnej |
| Endpoint health | `/actuator/health` publiczny do monitorowania |

---

## Pipeline budowania i dystrybucji

| Wynik | Sposób produkcji |
|-------|------------------|
| JAR backendu | Maven `package` w `backend/` |
| Obraz aplikacji desktopowej | Maven `jpackage` w `frontend/` |
| Zip przenośny na Windows | `scripts/build-windows-package.ps1` |
| GitHub Release | Workflow CI przy pushu do `main` |
