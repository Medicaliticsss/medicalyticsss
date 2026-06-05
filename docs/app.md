# Medicalytics — Przewodnik aplikacji i referencja API

Ten dokument opisuje sposoby uruchomienia Medicalytics w różnych środowiskach oraz REST API udostępniane przez backend.

Klient desktopowy wymaga działającego API. W **pakiecie przenośnym na Windows** API i baza danych startują automatycznie. W **trybie deweloperskim** najpierw uruchom backend, potem frontend.

---

## Uruchamianie aplikacji

### Opcja A: Pakiet przenośny na Windows (użytkownicy końcowi)

1. Pobierz z [GitHub Releases](https://github.com/Medicaliticsss/medicalyticsss/releases/latest)
2. Rozpakuj archiwum zip
3. Kliknij dwukrotnie **`Medicalytics.cmd`**

Nie trzeba instalować Javy, Dockera ani MariaDB.

### Opcja B: Docker (API + baza danych)

Z katalogu głównego repozytorium:

```bash
docker compose up -d --build
```

Uruchamia MariaDB i API Spring Boot na porcie `8080`. Następnie uruchom klienta desktopowego:

```bash
cd frontend
../backend/mvnw -Pdev javafx:run
```

Możesz też użyć samodzielnej wersji desktopowej, jeśli ją zbudowałeś:

```bash
scripts/launch-desktop.sh    # Linux/macOS
scripts\launch-desktop.bat   # Windows
```

### Opcja C: Lokalny development (IntelliJ / Maven)

**Wymagania:** JDK 21+ (frontend), JDK 17+ (backend), MariaDB

1. Utwórz bazę danych:

```sql
DROP DATABASE IF EXISTS medicalytics;
CREATE DATABASE medicalytics;
```

2. Skonfiguruj połączenie z MariaDB w `backend/src/main/resources/application.properties`.

3. Uruchom backend:

```bash
cd backend
./mvnw spring-boot:run
```

Przy starcie **Flyway** stosuje migracje, a **DictionarySeeder** ładuje słownik badań z pliku `backend/src/main/resources/dictionaries/test-types.json`.

4. Uruchom frontend:

```bash
cd frontend
../backend/mvnw -Pdev javafx:run
```

### Budowanie pakietu przenośnego na Windows

```bat
scripts\build-windows-package.bat
```

Wynik: `dist\medicalytics-windows-portable.zip`

---

## Ekrany aplikacji desktopowej

| Ekran | Opis |
|-------|------|
| **Logowanie / Rejestracja** | Uwierzytelnianie użytkownika |
| **Pliki** | Wgrywanie, podgląd, przetwarzanie i usuwanie plików CSV |
| **Raporty** | Tworzenie raportów BI, wykresów oraz eksport CSV/PNG |
| **Ustawienia** | Konto, zmiana hasła, słownik badań (MDM) |

---

## Moduł ustawień

Dostępny w aplikacji pod zakładką **Ustawienia**.

### Konto
- Podgląd nazwy użytkownika i statusu sesji
- Zmiana hasła

### Słownik badań (MDM)
- Przeglądanie i wyszukiwanie autorytatywnego katalogu badań (`dim_test_type`)
- Edycja norm i metadanych dla kodu badania
- Eksport słownika do JSON
- Import słownika z JSON (aktualizacja i dodawanie wpisów)

Słownik załadowany przy starcie serwera jest źródłem prawdy przy wykrywaniu anomalii. Wartości `norma_min`, `norma_max` i `jednostka` z pliku CSV **nie nadpisują** słownika.

Własny plik słownika przy starcie (konfiguracja serwera):

```bash
java -jar backend.jar --dictionary.tests.path=file:/sciezka/do/custom-test-types.json
```

Format JSON: tablica obiektów z polami `testCode`, `testName`, `categoryName`, `unit`, `normMin`, `normMax`.

---

## Referencja API

Adres bazowy: `http://localhost:8080`

Wszystkie endpointy poza `/api/auth/login` i `/api/auth/register` wymagają aktywnej sesji (ciasteczka).

### Autoryzacja (`/api/auth`)

Żądania formularzowe (`application/x-www-form-urlencoded`). Hasła są przechowywane jako hashe BCrypt.

| Metoda | Endpoint | Opis |
|--------|----------|------|
| `POST` | `/api/auth/register` | Rejestracja (`username`, `password`) |
| `POST` | `/api/auth/login` | Logowanie (`username`, `password`) |
| `GET` | `/api/auth/me` | Informacje o bieżącej sesji |
| `POST` | `/api/auth/logout` | Wylogowanie |

### Pliki i ETL (`/api/files`)

| Metoda | Endpoint | Opis |
|--------|----------|------|
| `GET` | `/api/files` | Lista wgranych plików (`id`, `fileName`, `status`, `uploadTime`) |
| `POST` | `/api/files/upload` | Wgranie CSV (`multipart/form-data`, pole `file`) |
| `GET` | `/api/files/{id}/preview` | Podgląd pierwszych wierszy (`?limit=50` opcjonalnie) |
| `POST` | `/api/files/{id}/process` | Uruchomienie ETL na pliku |
| `POST` | `/api/files/{id}/delete` | Miękkie usunięcie pliku i rollback powiązanych danych |
| `GET` | `/api/files/{id}/errors` | Błędy przetwarzania pliku |

**Zachowanie przy wgrywaniu:**
- Pliki są zapisywane w `uploads/` na serwerze
- Kolizje nazw są rozwiązywane automatycznie (np. `plik(1).csv`)
- Tworzony jest rekord w `files_history` ze statusem `UPLOADED`

**Zachowanie ETL:**
- Ścisła walidacja: wymagane wszystkie 15 kolumn CSV w każdym wierszu
- PESEL jest hashowany algorytmem SHA-256
- Nazwy placówek, miast i województw są normalizowane (Title Case)
- Flagi anomalii korzystają wyłącznie z norm ze słownika, nie z CSV
- Status pliku: `SUCCESS`, `PARTIAL_SUCCESS` lub `ERROR`

### Raporty (`/api/reports`)

| Metoda | Endpoint | Opis |
|--------|----------|------|
| `GET` | `/api/reports/summary` | Globalne KPI (`totalTests`, `normalResults`, `abnormalResults`) |
| `POST` | `/api/reports/custom` | Dane do wykresów (lista `ReportDataPoint`) |
| `POST` | `/api/reports/custom/table` | Dynamiczne wiersze tabeli (agregacja opcjonalna) |
| `POST` | `/api/reports/series` | Dane wieloseryjne (`x`, `series`, `value`) |
| `POST` | `/api/reports/raw` | Surowe wiersze drill-down (max 500, tylko filtry) |

Żądania raportowe używają JSON `CustomReportRequest` lub `SeriesReportRequest` z kolumnami z whitelisty, operacjami (`COUNT`, `SUM`, `AVG` itd.) oraz filtrami (`IN`, `BETWEEN` itd.).

### Ustawienia (`/api/settings`)

| Metoda | Endpoint | Opis |
|--------|----------|------|
| `PUT` | `/api/settings/password` | Zmiana hasła (JSON: `oldPassword`, `newPassword`) |
| `GET` | `/api/settings/dictionary` | Lista wpisów słownika badań |
| `GET` | `/api/settings/dictionary/export` | Eksport słownika do JSON |
| `PUT` | `/api/settings/dictionary/{testCode}` | Aktualizacja jednego wpisu |
| `POST` | `/api/settings/dictionary/import` | Import/synchronizacja słownika z tablicy JSON |

---

## Format pliku CSV

Wiersze muszą być rozdzielone przecinkami i zawierać **wszystkie 15 pól** w dokładnie tej kolejności:

```
imie, nazwisko, data_urodzenia, plec, pesel, placowka_nazwa, miasto, wojewodztwo, kod_badania, nazwa_badania, kategoria_badania, wynik, jednostka, norma_min, norma_max
```

| Pole | Opis |
|------|------|
| `imie` | Imię |
| `nazwisko` | Nazwisko |
| `data_urodzenia` | Data urodzenia |
| `plec` | Płeć |
| `pesel` | PESEL (hashowany przy imporcie) |
| `placowka_nazwa` | Nazwa placówki |
| `miasto` | Miasto |
| `wojewodztwo` | Województwo |
| `kod_badania` | Kod badania |
| `nazwa_badania` | Nazwa badania |
| `kategoria_badania` | Kategoria badania |
| `wynik` | Wynik |
| `jednostka` | Jednostka (nieużywana w MDM) |
| `norma_min` | Norma min z pliku (nieużywana w MDM) |
| `norma_max` | Norma max z pliku (nieużywana w MDM) |

Brakujące pola w wierszu powodują jego odrzucenie i zapis w `processing_errors`. Plik może otrzymać status `PARTIAL_SUCCESS`.

Przykładowe pliki: [`csv/`](../csv/)

---

## Sprawdzenie stanu API

```bash
curl http://localhost:8080/actuator/health
```

---

## Pliki testów API

Przykłady żądań HTTP do ręcznego testowania:

- [`backend/api-tests/test_logowania.http`](../backend/api-tests/test_logowania.http)
- [`backend/api-tests/test_sesji.http`](../backend/api-tests/test_sesji.http)
- [`backend/api-tests/test_dictionary.http`](../backend/api-tests/test_dictionary.http)
