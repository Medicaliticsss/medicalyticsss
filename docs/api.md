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
* **Przez frontend:** Uruchom `backend` oraz po nim `frontend`, po czym zaloguj się i wybierz plik ze swojego komputera.
* **Metoda:** `POST`
* **Typ zawartości:** `multipart/form-data`
* **Parametry:** `file` (wymagany plik z rozszerzeniem .csv)
* **Działanie:** Zapisuje plik fizycznie w folderze `uploads/` i tworzy rekord bazowy w `files_history` ze statusem `UPLOADED`. Przypisuje plik do zalogowanego użytkownika.
* **Odpowiedź:** `200 OK` (informacja o wgraniu pliku na serwer).

#### Przetwarzanie i Analiza Danych
* **URL:** `/api/files/{id}/process`
* **Metoda:** `POST`
* **Parametr URL:** `{id}` - ID pliku z tabeli `files_history`.
* **Działanie:** - Czyta wgrany plik wiersz po wierszu.
    - Zabezpiecza wrażliwe dane: zamienia PESEL na skrót SHA-256.
    - Realizuje operacje "Upsert" na wymiarach: wyszukuje lub tworzy pacjentów (`dim_patient`), placówki (`dim_facility`) i słowniki badań (`dim_test_type`).
    - Waliduje typy liczbowe (zamiana `,` na `.`, precyzja `BigDecimal`).
    - Przelicza odchylenia od norm medycznych i ustawia flagę `is_abnormal`.
    - Sukcesy dopisuje do tabeli `fact_test_results`, a anomalie wierszy odrzuca do tabeli `processing_errors`.
    - Aktualizuje finalny status pliku (`SUCCESS`, `PARTIAL_SUCCESS` lub `ERROR`).
* **Odpowiedź:** `200 OK` (`"Proces przetwarzania zakończony. Sprawdź status pliku."`)

#### Wymagana Struktura Pliku CSV
Aby walidator poprawnie przetworzył plik, musi on zawierać wartości oddzielone przecinkami (pierwszy wiersz z nagłówkami jest ignorowany) w dokładnie takiej kolejności:
`imie, nazwisko, data_urodzenia, plec, pesel, placowka_nazwa, miasto, kod_badania, nazwa_badania, wynik, jednostka, norma_min, norma_max`