# Medicalytics - Instrukcja uruchomienia i API

Projekt składa się z serwera (Backend) oraz aplikacji okienkowej (Frontend). Aby system działał poprawnie, **BACKEND MUSI ZOSTAC URUCHOMIONY JAKO PIERWSZY**.

---

## 1. Instrukcja uruchomienia aplikacji

### Wymagania wstępne
* Zainstalowana Java (JDK 17 lub nowsza).
* Działająca baza danych MariaDB (zgodnie z `application.properties`).

### Krok 1: Przygotowanie bazy danych (Ważne po aktualizacji kodu!)
Zarządzanie strukturą bazy danych w projekcie przejął **Flyway**. Zanim uruchomisz aplikację po pobraniu nowości z GitHuba, musisz zresetować swoją lokalną bazę:
1. Otwórz swój program do obsługi bazy (korzystamy z HeidiSQL).
2. Wykonaj poniższe polecenia, aby uzyskać czystą kartę:

DROP DATABASE medicalytics;
CREATE DATABASE medicalytics;

3. W IntelliJ otwórz prawy panel **Maven**.
4. Rozwiń `backend` -> `Lifecycle`, a następnie kliknij dwukrotnie **`clean`**, a zaraz po nim **`compile`**. (To gwarantuje, że skrypty migracyjne skopiują się do odpowiedniego folderu).

### Krok 2: Uruchomienie Serwera (Backend)
1. W IntelliJ przejdź do folderu `backend/src/main/java/com/medicalyticsss/backend`.
2. Otwórz klasę `Application` (główną klasę Spring Boot).
3. Uruchom metodę `main` (zielony trójkąt "Run").
4. W konsoli powinieneś zobaczyć komunikat o wymuszeniu uruchomienia Flywaya (skrypty same zbudują tabele w pustej bazie) oraz o nasłuchiwaniu Tomcata na porcie `8080`. **NIE ZAMYKAJ TEJ KONSOLI.**

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
* **Działanie:** * Zapisuje plik fizycznie w folderze `uploads/`.
  * **Obsługa kolizji:** Jeśli plik o danej nazwie istnieje, serwer automatycznie inkrementuje nazwę (np. `plik(1).csv`), zapobiegając nadpisaniu danych na dysku.
  * Tworzy rekord w `files_history` ze statusem `UPLOADED` oraz zapisuje dokładny czas operacji (`uploadTime`). Przypisuje plik do zalogowanego użytkownika.
* **Odpowiedź:** `200 OK` (informacja o pomyślnym wgraniu i wygenerowanej nazwie).

#### Pobieranie listy plików
* **URL:** `/api/files`
* **Metoda:** `GET`
* **Działanie:** * Odpytuje bazę danych o wszystkie rekordy zarejestrowane w tabeli `files_history`.
  * Mapuje dane na format JSON, przesyłając kluczowe pola: `id`, `fileName`, `status` oraz `uploadTime`.
  * Służy jako główne źródło danych dla interfejsu użytkownika (zasilanie komponentu ListView). Aplikacja frontendowa filtruje pliki ze statusem `DELETED`.
* **Odpowiedź:** `200 OK` (Zwraca listę JSON, np. `[{"id": 1, "fileName": "dane.csv", "status": "UPLOADED", "uploadTime": "2026-04-26T14:05:30.123"}]`).

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
  * Zabezpiecza wrażliwe dane: zamienia PESEL na skrót SHA-256.
  * Realizuje operacje "Upsert" na wymiarach: wyszukuje lub tworzy pacjentów (`dim_patient`), placówki (`dim_facility`) i słowniki badań (`dim_test_type`).
  * Waliduje typy liczbowe (zamiana `,` na `.`, precyzja `BigDecimal`).
  * Przelicza odchylenia od norm medycznych i ustawia flagę `is_abnormal`.
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
Moduł agregujący zanonimizowane dane medyczne w celu zasilania wykresów i kart KPI w aplikacji klienckiej.

#### Podsumowanie globalne (MVP)
* **URL:** `/api/reports/summary`
* **Metoda:** `GET`
* **Działanie:** Wykonuje szybkie zapytania agregujące (`COUNT`) na poziomie bazy danych w tabeli `fact_test_results`. Zlicza łączną liczbę wykonanych badań oraz proporcje wyników mieszczących się w normie w stosunku do wykrytych anomalii medycznych.
* **Odpowiedź:** `200 OK` (Zwraca obiekt JSON z podsumowaniem, np. `{"totalTests": 1500, "normalResults": 1350, "abnormalResults": 150}`).

---

#### Wymagana Struktura Pliku CSV
Aby walidator poprawnie przetworzył plik, musi on zawierać wartości oddzielone przecinkami (pierwszy wiersz z nagłówkami jest ignorowany) w dokładnie takiej kolejności:
`imie, nazwisko, data_urodzenia, plec, pesel, placowka_nazwa, miasto, kod_badania, nazwa_badania, wynik, jednostka, norma_min, norma_max`