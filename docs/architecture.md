# Architektura Systemu Medicalytics

Projekt opiera się na klasycznej architekturze **Klient-Serwer**, rozdzielając interfejs użytkownika od logiki biznesowej i bazy danych. System integruje moduł **Autoryzacji** z zaawansowanym modułem **ETL (Extract, Transform, Load)** oraz bezpiecznym systemem zarządzania plikami fizycznymi.

## Komponenty Systemu

### 1. Frontend (Aplikacja Desktopowa)
* **Technologia:** JavaFX (Java 17+)
* **Rola:** GUI umożliwiające logowanie, rejestrację oraz zarządzanie procesem wgrywania, podglądu i analizy plików CSV.
* **Komunikacja:** Wykorzystuje wbudowanego klienta `HttpClient` do asynchronicznej komunikacji z REST API.

### 2. Backend (Serwer REST API)
* **Technologia:** Java, Spring Boot 3.x.
* **Bezpieczeństwo (Spring Security):** * Zarządza dostępem do endpointów i sesjami.
  * Hasła są szyfrowane jednostronnie algorytmem **BCrypt**.
* **Zarządzanie Plikami:** * **Collision Resolver:** Automatycznie zapobiega nadpisywaniu plików na dysku serwera (np. poprzez inkrementację nazw: `plik(1).csv`), zachowując fizyczną integralność danych.
  * **Data Profiling:** Wykorzystuje odczyt strumieniowy (`Files.lines`) w Javie do bezpiecznego podglądu plików, chroniąc pamięć RAM serwera przed przeciążeniem gigabajtowymi plikami.
* **Moduł ETL:** Klasa `CsvProcessingService` odpowiada za walidację medyczną, anonimizację PESEL (SHA-256) oraz transakcyjną logikę bazy danych.

### 3. Baza Danych (Model Gwiazdy + Moduł User)
* **Technologia:** MariaDB zarządzana przez **Flyway**.
* **Struktura:**
  * **Użytkownicy:** Tabela `users` (id, username, password_hash).
  * **Tabele Faktów:** `fact_test_results` (wyniki badań powiązane z wymiarami i plikiem źródłowym).
  * **Tabele Wymiarów:** `dim_patient`, `dim_facility`, `dim_test_type` (współdzielone słowniki uaktualniane metodą *Upsert*).
  * **Historia:** `files_history` – kluczowa relacja z `users` (kolumna `user_id`). Przechowuje finalną nazwę pliku, datę wgrania (`uploadTime`) oraz status cyklu życia (np. `UPLOADED`, `SUCCESS`, `DELETED`).
  * **Błędy:** `processing_errors` – szczegółowe logi anomalii w plikach chroniące przed przerwaniem procesu ETL.

## Przepływ Danych i Cykl Życia Pliku

### Proces Autoryzacji
1. Użytkownik loguje się przez Frontend.
2. Backend sprawdza hash hasła w tabeli `users`.
3. Po poprawnym zalogowaniu, ID użytkownika jest trwale przypisywane do jego akcji w systemie (np. wgrywania plików).

### Proces CSV (Od Uploadu do Rollbacku)
1. **Upload i Kolizje:** Plik trafia na serwer. System weryfikuje unikalność nazwy na dysku, zapisuje fizyczny plik i tworzy rekord w `files_history` z początkowym statusem `UPLOADED`.
2. **Podgląd (Preview):** Przed przetworzeniem użytkownik może zażądać podglądu. Serwer strumieniuje określoną liczbę wierszy bez ładowania całego pliku do pamięci.
3. **Transformacja (ETL):** Dane są anonimizowane (PESEL -> Hash), konwertowane na typy numeryczne z zachowaniem odpowiedniej precyzji (`BigDecimal`).
4. **Analiza:** System wykonuje logikę biznesową, weryfikuje istnienie wymiarów (tworząc je w razie braku), porównuje wyniki z normami i ustawia flagę anomali (`is_abnormal`).
5. **Finalizacja:** Prawidłowe wiersze zasilają model gwiazdy, a błędne lądują w tabeli anomalii. Status pliku ulega zmianie.
6. **Wycofanie Zmian (Rollback / Soft Delete):** Po usunięciu pliku przez użytkownika, system transakcyjnie kasuje wyniki i błędy (`@Modifying`) chroniąc spójność raportów. Wymiary (Słowniki) celowo nie są usuwane, co stanowi dobrą praktykę architektury hurtowni danych (zapobieganie naruszaniu kluczy obcych u innych pacjentów). Rekord otrzymuje status `DELETED`.