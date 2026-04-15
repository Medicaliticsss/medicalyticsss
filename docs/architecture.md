# Architektura Systemu Medicalytics

Projekt opiera się na klasycznej architekturze **Klient-Serwer**, rozdzielając interfejs użytkownika od logiki biznesowej i bazy danych. System integruje moduł **Autoryzacji** z zaawansowanym modułem **ETL (Extract, Transform, Load)**.

## Komponenty Systemu

### 1. Frontend (Aplikacja Desktopowa)
* **Technologia:** JavaFX (Java 17+)
* **Rola:** GUI umożliwiające logowanie, rejestrację oraz zarządzanie procesem wgrywania i analizy plików CSV.
* **Komunikacja:** Wykorzystuje `HttpClient` do łączenia się z REST API.

### 2. Backend (Serwer REST API)
* **Technologia:** Java, Spring Boot 3.x.
* **Bezpieczeństwo (Spring Security):** * Zarządza dostępem do endpointów.
  * Hasła są szyfrowane jednostronnie algorytmem **BCrypt**.
* **Moduł ETL:** Klasa `CsvProcessingService` odpowiada za walidację medyczną, anonimizację PESEL (SHA-256) oraz logikę "Upsert" wymiarów.

### 3. Baza Danych (Model Gwiazdy + Moduł User)
* **Technologia:** MariaDB zarządzana przez **Flyway**.
* **Struktura:**
  * **Użytkownicy:** Tabela `users` (id, username, password_hash).
  * **Tabele Faktów:** `fact_test_results` (wyniki badań).
  * **Tabele Wymiarów:** `dim_patient`, `dim_facility`, `dim_test_type`.
  * **Historia:** `files_history` – **kluczowa relacja z `users`** (kolumna `user_id`), wskazująca, kto wgrał dany plik.
  * **Błędy:** `processing_errors` – szczegółowe logi anomalii w plikach.

## Przepływ Danych i Bezpieczeństwo

### Proces Autoryzacji
1. Użytkownik loguje się przez Frontend.
2. Backend sprawdza hash hasła w tabeli `users`.
3. Po poprawnym zalogowaniu, ID użytkownika jest przypisywane do każdej sesji wgrywania pliku.

### Proces CSV (ETL) z kontekstem użytkownika
1. **Upload:** Plik trafia na serwer. W tabeli `files_history` powstaje wpis z przypisanym `user_id`.
2. **Transformacja:** Dane są anonimizowane (PESEL -> Hash) i konwertowane na typy numeryczne.
3. **Analiza:** System porównuje wyniki z normami i ustawia flagę `is_abnormal`.
4. **Finalizacja:** Dane trafiają do tabel faktów, a użytkownik otrzymuje raport o sukcesach i błędach przetwarzania.