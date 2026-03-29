# Medicalytics - Instrukcja uruchomienia i API

Projekt składa się z serwera (Backend) oraz aplikacji okienkowej (Frontend). Aby system działał poprawnie, **BACKEND MUSI ZOSTAC URUCHOMIONY JAKO PIERWSZY**.

---

## 1. Instrukcja uruchomienia aplikacji

### Wymagania wstępne
* Zainstalowana Java (JDK 17 lub nowsza).
* Działająca baza danych MariaDB (zgodnie z `application.properties`).

### Krok 1: Uruchomienie Serwera (Backend)
1. W IntelliJ przejdź do folderu `backend/src/main/java/com/medicalyticsss/backend`.
2. Otwórz klasę `Application` (lub główną klasę Spring Boot).
3. Uruchom metodę `main` (zielony trójkąt "Run").
4. Poczekaj na komunikat w konsoli o nasłuchiwaniu na porcie `8080`. **NIE ZAMYKAJ TEJ KONSOLI.**

### Krok 2: Uruchomienie Aplikacji Okienkowej (Frontend)
1. Otwórz PRAWY panel **Maven** w IntelliJ.
2. Rozwiń: `Frontend` -> `Plugins` -> `javafx`.
3. Kliknij dwukrotnie na **`javafx:run`**.
4. System pobierze biblioteki i otworzy główne okno logowania.

---

## 2. Dokumentacja API (Endpointy)

Gdy serwer działa (Krok 1), udostępnia poniższe punkty końcowe pod adresem: `http://localhost:8080`

### Moduł Autoryzacji (`/api/auth`)
Komunikacja odbywa się NA TEN MOMENT poprzez przesyłanie parametrów formularza (`application/x-www-form-urlencoded`).

#### Rejestracja
* **URL:** `/api/auth/register`
* **Metoda:** `POST`
* **Parametry:** `username`, `password`
* **Odpowiedź:** `200 OK` (`"Zarejestrowano pomyślnie!"`)

#### Logowanie
* **URL:** `/api/auth/login`
* **Metoda:** `POST`
* **Parametry:** `username`, `password`
* **Odpowiedź:** `200 OK` (`"Zalogowano pomyślnie!"` lub `"Błędne login lub hasło!"`)