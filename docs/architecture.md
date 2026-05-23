# Architektura Systemu Medicalytics

Projekt opiera się na klasycznej architekturze **Klient-Serwer**, rozdzielając interfejs użytkownika od logiki biznesowej i bazy danych. System integruje moduł **Autoryzacji** z zaawansowanym modułem **ETL (Extract, Transform, Load)**, mechanizmami **Master Data Management (MDM)**, bezpiecznym systemem zarządzania plikami fizycznymi oraz **Modułem Raportowym**.

## Komponenty Systemu

### 1. Frontend (Aplikacja Desktopowa kliencka)
* **Technologia:** JavaFX (Java 17+).
* **Architektura:** Wzorzec **MVC (Model-View-Controller)**. Logika została zdywersyfikowana zgodnie z zasadą Single Responsibility:
  * **Views:** Osobne klasy odpowiedzialne za renderowanie poszczególnych ekranów (GUI).
  * **Services:** Separacja komunikacji sieciowej (wbudowany `HttpClient` asynchronicznie odpytujący REST API) oraz deserializacji JSON (Gson).
  * **Models:** Obiekty DTO (np. `FileItem`, `ReportSummary`) służące do transferu danych.
  * **ViewManager / Router:** Centralny zarządca scen i płynnej nawigacji (Single Page Application UI).

### 2. Backend (Serwer REST API)
* **Technologia:** Java, Spring Boot 3.x.
* **Bezpieczeństwo (Spring Security):** * Zarządza dostępem do endpointów i sesjami.
  * Hasła są szyfrowane jednostronnie algorytmem **BCrypt**.
* **Zarządzanie Plikami:** * **Collision Resolver:** Automatycznie zapobiega nadpisywaniu plików na dysku serwera (np. poprzez inkrementację nazw: `plik(1).csv`), zachowując fizyczną integralność danych.
  * **Data Profiling:** Wykorzystuje odczyt strumieniowy (`Files.lines`) w Javie do bezpiecznego podglądu plików, chroniąc pamięć RAM serwera przed przeciążeniem gigabajtowymi plikami.
* **Moduł ETL & MDM:** * **Strict Validation:** Klasa `CsvProcessingService` rygorystycznie waliduje kompletność danych wiersz po wierszu.
  * **String Sanitization:** Czyści i standaryzuje (Title Case) dane tekstowe (np. nazwy placówek) "w locie", aby zapobiegać duplikatom.
  * Zapewnia anonimizację danych wrażliwych pacjentów (PESEL -> SHA-256).
* **Dictionary Seeder:** Komponent ładujący twardy słownik referencyjny medycznych norm i badań podczas startu serwera, uodparniając bazę na błędne dane z zewnątrz.
* **Moduł Raportowy (Analytics):** Agreguje przetworzone dane. Zwraca obiekty transferowe (Record DTO) i deleguje ciężkie obliczenia do silnika bazy danych (funkcje SQL agregujące), eliminując przeciążenie pamięci Javy.

### 3. Baza Danych (Model Gwiazdy + Moduł User)
* **Technologia:** MariaDB zarządzana przez zautomatyzowane skrypty migracyjne **Flyway**.
* **Struktura:**
  * **Użytkownicy:** Tabela `users` (id, username, password_hash).
  * **Tabele Faktów:** `fact_test_results` (wyniki badań powiązane z wymiarami i plikiem źródłowym).
  * **Tabele Wymiarów (Współdzielone):** * `dim_patient`, `dim_facility` – aktualizowane dynamicznie i transakcyjnie metodą *Upsert* (po uprzedniej normalizacji tekstów).
    * `dim_test_type` – **Twardy Słownik (MDM)**, zasilany wyłącznie przez system, służący jako ostateczne źródło prawdy dla norm badawczych.
  * **Historia:** `files_history` – kluczowa relacja z `users` (kolumna `user_id`). Przechowuje finalną nazwę pliku, datę wgrania (`uploadTime`) oraz status cyklu życia (np. `UPLOADED`, `SUCCESS`, `DELETED`).
  * **Błędy:** `processing_errors` – szczegółowe logi anomalii w plikach chroniące przed przerwaniem globalnego procesu ETL.

## Przepływ Danych i Cykl Życia Systemu

### Proces Autoryzacji
1. Użytkownik loguje się przez zrefaktoryzowany Frontend (AuthService).
2. Backend sprawdza hash hasła w tabeli `users`.
3. Po poprawnym zalogowaniu, ID użytkownika jest trwale przypisywane do jego akcji w systemie (np. wgrywania plików).

### Proces CSV (Od Uploadu do Rollbacku)
1. **Upload i Kolizje:** Plik trafia na serwer. System weryfikuje unikalność nazwy na dysku, zapisuje fizyczny plik i tworzy rekord w `files_history` z początkowym statusem `UPLOADED`.
2. **Podgląd (Preview):** Przed przetworzeniem użytkownik może zażądać podglądu. Serwer strumieniuje określoną liczbę wierszy bez ładowania całego pliku do pamięci.
3. **Transformacja (ETL & Walidacja):** * Wiersze poddawane są "Żelaznej Bramce" (odrzucanie rekordów z brakującymi kolumnami).
* Wrażliwe dane ulegają anonimizacji.
* Teksty podlegają obróbce i czyszczeniu (String Sanitization).
4. **Analiza i Weryfikacja (MDM):** System weryfikuje wymiary. Przy wyliczaniu flagi anomalii (`is_abnormal`), parser całkowicie ignoruje normy zawarte w pliku CSV, opierając analizę wyłącznie na *Twardym Słowniku* załadowanym do bazy przez system.
5. **Finalizacja:** Prawidłowe wiersze zasilają model gwiazdy, a błędne lądują w tabeli anomalii. Status pliku ulega zmianie na `SUCCESS` lub `PARTIAL_SUCCESS`.
6. **Wycofanie Zmian (Rollback / Soft Delete):** Po usunięciu pliku przez użytkownika, system transakcyjnie kasuje wyniki i błędy (`@Modifying`) chroniąc spójność raportów. Wymiary i słowniki celowo nie są usuwane. Rekord pliku otrzymuje status `DELETED`.

### Proces Raportowania i Analityki
1. **Żądanie:** Użytkownik otwiera zakładkę raportów w aplikacji klienckiej (Frontend wywołuje `ReportService`).
2. **Optymalizacja Agregacji:** Backend odbiera żądanie i zamiast pobierać setki tysięcy rekordów do pamięci RAM, wysyła zoptymalizowane zapytania do bazy danych (np. `COUNT(*) WHERE is_abnormal = true`).
3. **Transfer:** Zliczone wartości są mapowane na zoptymalizowany obiekt DTO i przesyłane do klienta.
4. **Wizualizacja:** Frontend (w oparciu o silnik graficzny JavaFX i mechanizm Auto-Grow) responsywnie renderuje odebrane wskaźniki na kartach KPI oraz dynamicznych wykresach (np. `PieChart`).