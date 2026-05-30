# Architektura Systemu Medicalytics

Projekt opiera się na klasycznej architekturze **Klient-Serwer**, rozdzielając interfejs użytkownika od logiki biznesowej i bazy danych. System integruje moduł **Autoryzacji** z zaawansowanym modułem **ETL (Extract, Transform, Load)**, mechanizmami **Master Data Management (MDM)**, bezpiecznym systemem zarządzania plikami fizycznymi oraz **Dynamicznym Silnikiem Raportowym (Business Intelligence / OLAP)**.

## Komponenty Systemu

### 1. Frontend (Aplikacja Desktopowa kliencka)
* **Technologia:** JavaFX (Java 17+) z wykorzystaniem biblioteki stylów **AtlantaFX**.
* **Architektura:** Wzorzec **MVC (Model-View-Controller)**. Logika została zdywersyfikowana zgodnie z zasadą Single Responsibility:
  * **Views:** Osobne klasy odpowiedzialne za renderowanie poszczególnych ekranów. Obejmuje zaawansowane komponenty Data Grid oraz dynamiczne generatory wykresów (Bar, Pie, Line, Scatter) potrafiące budować swoje kolumny automatycznie na podstawie odbieranych słowników JSON.
  * **Services:** Separacja komunikacji sieciowej (wbudowany `HttpClient` asynchronicznie odpytujący REST API) oraz deserializacji JSON (Gson).
  * **Models:** Obiekty transferu danych (DTO, Records) oraz dynamiczne mapy służące do komunikacji z serwerem.
  * **ViewManager / Router:** Centralny zarządca scen i płynnej nawigacji (Single Page Application UI).

### 2. Backend (Serwer REST API)
* **Technologia:** Java, Spring Boot 3.x.
* **Bezpieczeństwo (Spring Security):** * Zarządza dostępem do endpointów i sesjami.
  * Hasła są szyfrowane jednostronnie algorytmem **BCrypt**.
* **Zarządzanie Plikami:** * **Collision Resolver:** Automatycznie zapobiega nadpisywaniu plików na dysku serwera (np. poprzez inkrementację nazw: `plik(1).csv`), zachowując fizyczną integralność danych.
  * **Data Profiling:** Wykorzystuje odczyt strumieniowy (`Files.lines`) w Javie do bezpiecznego podglądu plików, chroniąc pamięć RAM serwera przed przeciążeniem gigabajtowymi plikami.
* **Moduł ETL & MDM:** * **Strict Validation:** Klasa `CsvProcessingService` rygorystycznie waliduje kompletność danych wiersz po wierszu.
  * **String Sanitization:** Czyści i standaryzuje (Title Case) dane tekstowe "w locie", aby zapobiegać duplikatom.
  * Zapewnia anonimizację danych wrażliwych pacjentów (PESEL -> SHA-256).
* **Dictionary Seeder:** Komponent ładujący referencyjny słownik medycznych norm i badań z konfigurowalnego pliku JSON podczas startu serwera, uodparniając bazę na błędne dane z zewnątrz.
* **Silnik Raportowy BI (OLAP):** Zastąpił sztywne zapytania w pełni dynamicznym silnikiem opartym na **JPA Criteria API**. Wykorzystuje wzorzec **Whitelist** (Enumy), gwarantując całkowitą ochronę przed atakami SQL Injection. Samodzielnie rozwiązuje złączenia tabel (LEFT JOIN) oraz przetwarza rzutowanie typów dla zaawansowanych filtrów (klauzula WHERE).

### 3. Baza Danych (Model Gwiazdy + Moduł User)
* **Technologia:** MariaDB zarządzana przez zautomatyzowane skrypty migracyjne **Flyway**.
* **Struktura:**
  * **Użytkownicy:** Tabela `users` (id, username, password_hash).
  * **Tabele Faktów:** `fact_test_results` (centralny punkt modelu gwiazdy; wyniki badań powiązane z wymiarami i plikiem źródłowym).
  * **Tabele Wymiarów (Współdzielone):** * `dim_patient`, `dim_facility` – aktualizowane dynamicznie i transakcyjnie metodą *Upsert* (po uprzedniej normalizacji tekstów).
    * `dim_test_type` – **Słownik MDM**, zasilany przez system z pliku wskazanego we właściwości `dictionary.tests.path`, służący jako ostateczne źródło prawdy dla norm badawczych.
  * **Historia:** `files_history` – kluczowa relacja z `users` (kolumna `user_id`). Przechowuje finalną nazwę pliku, datę wgrania (`uploadTime`) oraz status cyklu życia.
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
4. **Analiza i Weryfikacja (MDM):** System weryfikuje wymiary. Przy wyliczaniu flagi anomalii (`is_abnormal`), parser całkowicie ignoruje normy zawarte w pliku CSV, opierając analizę wyłącznie na słowniku MDM załadowanym do bazy przez system z pliku JSON.
5. **Finalizacja:** Prawidłowe wiersze zasilają model gwiazdy, a błędne lądują w tabeli anomalii. Status pliku ulega zmianie na `SUCCESS` lub `PARTIAL_SUCCESS`.
6. **Wycofanie Zmian (Rollback / Soft Delete):** Po usunięciu pliku przez użytkownika, system transakcyjnie kasuje wyniki i błędy (`@Modifying`) chroniąc spójność raportów. Wymiary i słowniki celowo nie są usuwane. Rekord pliku otrzymuje status `DELETED`.

### Proces Raportowania i Analityki Business Intelligence (OLAP)
1. **Konfiguracja Żądania:** W interfejsie użytkownika zdefiniowane zostają parametry analizy: wymiar (oś X), funkcja agregująca (np. AVG, SUM na osi Y) oraz dynamiczne filtry i typ wizualizacji.
2. **Translacja (Criteria API):** Backend obiera żądanie i w sposób bezpieczny (Whitelist) tłumaczy je na zapytanie bazodanowe. Silnik wykrywa, których tabel dotyczy zapytanie i automatycznie generuje odpowiednie klauzule złączeń (`JOIN`).
3. **Obliczenia Bazodanowe:** Główne ciężary agregacji, sortowania i filtrowania wykonywane są bezpośrednio przez silnik MariaDB. Aplikacja Java nie ładuje setek tysięcy rekordów do pamięci RAM, chyba że użytkownik wykonuje żądanie podglądu surowych danych (funkcja Drill-down) – wówczas zapytanie jest optymalizowane limitem rekodrów.
4. **Uniwersalny Transfer:** Zliczone wartości są transformowane na zunifikowany generyczny format (`ReportDataPoint` dla wykresów/tabeli, lub płaskie słowniki typu `Map<String, Object>` dla surowych wierszy).
5. **Wizualizacja:** Frontend asynchronicznie odbiera dane. Tabela wyników generuje swoje kolumny automatycznie na podstawie kluczy ze słownika JSON, a obszar wykresów responsywnie renderuje wskazany przez użytkownika typ wizualizacji graficznej.