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
```sql
DROP DATABASE medicalytics;
CREATE DATABASE medicalytics;
```
3. W IntelliJ otwórz prawy panel **Maven**.
4. Rozwiń `backend` -> `Lifecycle`, a następnie kliknij dwukrotnie **`clean`**, a zaraz po nim **`compile`**. (To gwarantuje, że skrypty migracyjne skopiują się do odpowiedniego folderu).

### Krok 2: Uruchomienie Serwera (Backend)
1. W IntelliJ przejdź do folderu `backend/src/main/java/com/medicalyticsss/backend`.
2. Otwórz klasę `Application` (główną klasę Spring Boot).
3. Uruchom metodę `main` (zielony trójkąt "Run").
4. W konsoli powinieneś zobaczyć komunikat o wymuszeniu uruchomienia Flywaya (skrypty same zbudują tabele w pustej bazie!) oraz o nasłuchiwaniu Tomcata na porcie `8080`. **NIE ZAMYKAJ TEJ KONSOLI.**

### Krok 3: Uruchomienie Aplikacji Okienkowej (Frontend)
1. Otwórz PRAWY panel **Maven** w IntelliJ.
2. Rozwiń: `Frontend` -> `Plugins` -> `javafx`.
3. Kliknij dwukrotnie na **`javafx:run`**.
4. System pobierze biblioteki i otworzy główne okno logowania.

---

## 2. Dokumentacja API (Endpointy)

Gdy serwer działa (Krok 2), udostępnia poniższe punkty końcowe pod adresem: `http://localhost:8080`

### Moduł Autoryzacji (`/api/auth`)
Komunikacja odbywa się NA TEN MOMENT poprzez przesyłanie parametrów formularza (`application/x-www-form-urlencoded`). Hasła w bazie danych są automatycznie szyfrowane algorytmem BCrypt.

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