# Medicalytics - Instrukcja uruchomienia i API

Projekt składa się z serwera (Backend) oraz aplikacji okienkowej (Frontend). Aby system działał poprawnie, **BACKEND MUSI ZOSTAC URUCHOMIONY JAKO PIERWSZY**.

---

## 1. Instrukcja uruchomienia aplikacji

### Wymagania wstępne
* Zainstalowana Java (JDK 17 lub nowsza).
* Działająca baza danych MariaDB (zgodnie z `application.properties`).

### Krok 1: Przygotowanie bazy danych (Ważne po aktualizacji kodu!)
Zarządzanie strukturą bazy danych w projekcie przejął **Flyway**. Zanim uruchomisz aplikację po pobraniu nowości z GitHuba, musisz zresetować swoją lokalną bazę:
1. Otwórz swój program do obsługi bazy (np. HeidiSQL / DBeaver).
2. Wykonaj poniższe polecenia, aby uzyskać czystą kartę:

   DROP DATABASE medicalytics;
   CREATE DATABASE medicalytics;

3. W IntelliJ otwórz prawy panel **Maven**.
4. Rozwiń `backend` -> `Lifecycle`, a następnie kliknij dwukrotnie **`clean`**, a zaraz po nim **`compile`**. (To gwarantuje, że skrypty migracyjne skopiują się do odpowiedniego folderu).

### Krok 2: Uruchomienie Serwera (Backend)
1. W IntelliJ przejdź do folderu `backend/src/main/java/com/medicalyticsss/backend`.
2. Otwórz klasę `Application` (główną klasę Spring Boot).
3. Uruchom metodę `main` (zielony trójkąt "Run").
4. W konsoli powinieneś zobaczyć komunikat o wymuszeniu uruchomienia Flywaya (skrypty same zbudują tabele w pustej bazie). Dodatkowo uruchomi się **DictionarySeeder**, który automatycznie załaduje słownik badań z pliku JSON do bazy. Tomcat rozpocznie nasłuchiwanie na porcie `8080`. **NIE ZAMYKAJ TEJ KONSOLI.**

Domyślny słownik znajduje się w `backend/src/main/resources/dictionaries/test-types.json`. Możesz wskazać własny plik bez przebudowy aplikacji, ustawiając właściwość `dictionary.tests.path`, np.:

```bash
java -jar backend.jar --dictionary.tests.path=file:/sciezka/do/custom-test-types.json
```

Plik powinien być tablicą obiektów JSON z polami: `testCode`, `testName`, `categoryName`, `unit`, `normMin`, `normMax`.

### Krok 3: Uruchomienie Aplikacji Okienkowej (Frontend)
1. Otwórz PRAWY panel **Maven** w IntelliJ.
2. Rozwiń: `Frontend` -> `Plugins` -> `javafx`.
3. Kliknij dwukrotnie na **`javafx:run`**.
4. System pobierze biblioteki i otworzy główne okno logowania.

---

## 2. Dokumentacja API (Endpointy)

Gdy serwer działa (Krok 2), udostępnia poniższe punkty końcowe pod adresem: `http://localhost:8080`

### Moduł Autoryzacji (`/api/auth`)
Komunikacja odbywa się poprzez przesyłanie parametrów formularza (`application/x-www-form-urlencoded`). Hasła w bazie danych są automatycznie szyfrowane algorytmem BCrypt.

#### Rejestracja
* **URL:** `/api/auth/register`
* **Metoda:** `POST`
* **Parametry:** `username`, `password`
* **Odpowiedź:** `200 OK` (`"Zarejestrowano pomyślnie!"` lub `"Błąd: Użytkownik o takiej nazwie już istnieje!"`)

#### Logowanie
* **URL:** `/api/auth/login`
* **Metoda:** `POST`
* **Parametry:** `username`, `password`
* **Odpowiedź:** `200 OK` (`"Zalogowano pomyślnie!"`, `"Błędne login lub hasło!"` lub `"Nie znaleziono użytkownika!"`)

---

### Moduł Zarządzania Plikami i Analizy ETL (`/api/files`)
Moduł obsługujący procesy hurtowni danych (Extract, Transform, Load) oraz bezpieczeństwo danych (hashowanie).

#### Wgranie pliku na serwer
* **URL:** `/api/files/upload`
* **Metoda:** `POST`
* **Typ zawartości:** `multipart/form-data`
* **Parametry:** `file` (wymagany plik z rozszerzeniem .csv)
* **Działanie:** * Zapisuje plik fizycznie w folderze `uploads/` (folder jest ignorowany w Git). Oficjalne dane testowe znajdują się w repozytorium w folderze `csv/`.
  * **Obsługa kolizji:** Jeśli plik o danej nazwie istnieje, serwer automatycznie inkrementuje nazwę (np. `plik(1).csv`), zapobiegając nadpisaniu danych na dysku.
  * Tworzy rekord w `files_history` ze statusem `UPLOADED` oraz zapisuje dokładny czas operacji (`uploadTime`). Przypisuje plik do zalogowanego użytkownika.
* **Odpowiedź:** `200 OK` (informacja o pomyślnym wgraniu i wygenerowanej nazwie).

#### Pobieranie listy plików
* **URL:** `/api/files`
* **Metoda:** `GET`
* **Działanie:** * Odpytuje bazę danych o wszystkie rekordy zarejestrowane w tabeli `files_history`.
  * Mapuje dane na format JSON, przesyłając kluczowe pola: `id`, `fileName`, `status` oraz `uploadTime`.
  * Służy jako główne źródło danych dla interfejsu użytkownika (zasilanie komponentu ListView). Aplikacja frontendowa filtruje pliki ze statusem `DELETED`.
* **Odpowiedź:** `200 OK` (Zwraca listę JSON, np. `[{"id": 1, "fileName": "dane.csv", "status": "UPLOADED", "uploadTime": "2026-05-23T14:05:30.123"}]`).

#### Podgląd zawartości pliku (Preview)
* **URL:** `/api/files/{id}/preview`
* **Metoda:** `GET`
* **Parametr URL:** `{id}` - ID pliku. Opcjonalnie parametr `?limit=50` do określenia liczby wierszy.
* **Działanie:** Odczytuje strumieniowo fizyczny plik z dysku i zwraca zdefiniowaną liczbę wierszy. Działa niezależnie od statusu przetwarzania. Optymalizacja chroni pamięć RAM przed przeładowaniem w przypadku dużych plików.
* **Odpowiedź:** `200 OK` (Tablica stringów reprezentująca wiersze CSV).

#### Przetwarzanie i Analiza Danych (ETL)
* **URL:** `/api/files/{id}/process`
* **Metoda:** `POST`
* **Parametr URL:** `{id}` - ID pliku z tabeli `files_history`.
* **Działanie:** * Czyta wgrany plik wiersz po wierszu.
  * **Ścisła walidacja (Strict Validation):** System weryfikuje kompletność każdego wiersza. Brak chociażby jednego pola skutkuje odrzuceniem konkretnego rekordu i logowaniem go do tabeli błędów.
  * Zabezpiecza wrażliwe dane: zamienia PESEL na skrót SHA-256.
  * **Normalizacja MDM:** Nazwy placówek, miast i województw przed zapisem są formatowane (Title Case, usuwanie znaków specjalnych i podwójnych spacji), aby zapobiec duplikatom w wymiarze `dim_facility`.
  * **Słownik MDM:** Wymiar `dim_test_type` jest nienaruszalny. Pliki CSV dostarczają tylko wyniki pacjenta – do ewaluacji anomalii używane są wyłącznie normy załadowane do bazy przez Seeder z pliku JSON.
  * Sukcesy dopisuje do tabeli `fact_test_results`, a anomalie odrzuca do tabeli `processing_errors`.
  * Aktualizuje finalny status pliku (`SUCCESS`, `PARTIAL_SUCCESS` lub `ERROR`).
* **Odpowiedź:** `200 OK` (`"Proces przetwarzania zakończony. Sprawdź status pliku."`)

#### Miękkie usuwanie pliku i Rollback
* **URL:** `/api/files/{id}/delete`
* **Metoda:** `POST`
* **Parametr URL:** `{id}` - ID pliku z tabeli `files_history`.
* **Działanie:** Zmienia status rekordu w `files_history` na `DELETED` (Soft Delete). Wykonuje Rollback, czyli kasuje wszystkie powiązane rekordy z tabel faktycznych (`fact_test_results`) oraz błędów (`processing_errors`), zachowując przy tym dane słownikowe w tabelach wymiarów.
* **Odpowiedź:** `200 OK` (`"Plik pomyślnie usunięty, a dane zrolowane."`)

---

### Moduł Raportów i Statystyk (`/api/reports`)
Moduł agregujący zanonimizowane dane medyczne w celu zasilania dynamicznych wykresów, tabel BI i kart KPI w aplikacji klienckiej.

#### Podsumowanie globalne
* **URL:** `/api/reports/summary`
* **Metoda:** `GET`
* **Działanie:** Wykonuje szybkie zapytania agregujące (`COUNT`) na poziomie bazy danych w tabeli `fact_test_results`. Zlicza łączną liczbę wykonanych badań oraz proporcje wyników mieszczących się w normie w stosunku do wykrytych anomalii medycznych.
* **Odpowiedź:** `200 OK` (Zwraca obiekt JSON z podsumowaniem, np. `{"totalTests": 1500, "normalResults": 1350, "abnormalResults": 150}`).

#### Kreator Raportów Analitycznych BI (OLAP)
* **URL:** `/api/reports/custom`
* **Metoda:** `POST`
* **Typ zawartości:** `application/json`
* **Parametry (Body):** Obiekt `CustomReportRequest` definiujący m.in. kolumny wymiarów (`selectColumns`), oś wartości (`aggregateColumn`), funkcję matematyczną (`operation` np. COUNT, SUM, AVG), sortowanie oraz listę dynamicznych filtrów (`filters` określające klauzulę WHERE). Filtry obsługują także operatory `IN` i `BETWEEN` z wartościami rozdzielonymi przecinkami.
* **Działanie:** Wykorzystuje silnik JPA Criteria API do generowania w pełni dynamicznych zapytań bazodanowych. Automatycznie rozwiązuje relacje (LEFT JOIN) między tabelami i bezpiecznie mapuje filtry z użyciem wzorca Whitelist, zapobiegając ryzyku SQL Injection.
* **Odpowiedź:** `200 OK` (Zwraca uniwersalną listę punktów danych, np. `[{"label": "Kobieta", "value": 152}, {"label": "Mężczyzna", "value": 98}]`).

#### Elastyczna tabela raportowa
* **URL:** `/api/reports/custom/table`
* **Metoda:** `POST`
* **Typ zawartości:** `application/json`
* **Działanie:** Zwraca dynamiczne wiersze raportowe jako listę słowników. Agregacja jest opcjonalna: bez `operation` endpoint zwraca unikalne kombinacje wybranych `selectColumns`, a z agregacją dodaje kolumnę wartości, np. `AVG_RESULT_VALUE`.
* **Odpowiedź:** `200 OK` (np. `[{"PATIENT_BIRTH_YEAR": 1980, "TEST_CODE": "HDL", "AVG_RESULT_VALUE": 55.2}]`).

#### Raport wieloseryjny
* **URL:** `/api/reports/series`
* **Metoda:** `POST`
* **Typ zawartości:** `application/json`
* **Działanie:** Buduje dane w formacie `x`, `series`, `value`, np. dla wykresu lipidogramu mężczyzn według roku urodzenia (`xAxis=PATIENT_BIRTH_YEAR`, `seriesField=TEST_CODE`, `aggregateColumn=RESULT_VALUE`, `operation=AVG`).
* **Odpowiedź:** `200 OK` (np. `[{"x": "1980", "series": "HDL", "value": 55.2}]`).

#### Pobieranie Surowych Danych (SELECT *)
* **URL:** `/api/reports/raw`
* **Metoda:** `POST`
* **Typ zawartości:** `application/json`
* **Parametry (Body):** Obiekt `CustomReportRequest` (do generowania zapytania brana jest pod uwagę wyłącznie tablica `filters`).
* **Działanie:** Buduje zapytanie pobierające surowe wiersze z bazy danych z zastosowaniem zdefiniowanych przez użytkownika warunków (funkcja Drill-down). W celu optymalizacji pamięci RAM wynik jest limitowany do 500 rekordów. Każdy wiersz encji jest tłumaczony na dynamiczny słownik (Map) z zachowaniem kolejności oraz polskimi nazwami nagłówków.
* **Odpowiedź:** `200 OK` (Zwraca listę słowników, np. `[{"ID Wyniku": 1, "Płeć": "MALE", "Miasto": "Olsztyn", "Czy Anomalia?": false}]`).

---

#### Wymagana Struktura Pliku CSV
Aby parser poprawnie przetworzył plik (zgodnie z mechanizmem Strict Validation), wiersze muszą zawierać wartości oddzielone przecinkami w dokładnie takiej kolejności:

`imie, nazwisko, data_urodzenia, plec, pesel, placowka_nazwa, miasto, wojewodztwo, kod_badania, nazwa_badania, kategoria_badania, wynik, jednostka, norma_min, norma_max`

> **Ważne:** Wszystkie 15 pól jest absolutnie wymagane. Puste wartości w kluczowych polach zrzucą rekord do logów jako błąd, skutkując statusem przetwarzania `PARTIAL_SUCCESS`. Ze względów bezpieczeństwa (MDM) wartości `jednostka`, `norma_min` i `norma_max` z pliku CSV nie nadpisują twardego słownika bazy danych.