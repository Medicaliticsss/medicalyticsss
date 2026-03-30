# Architektura Systemu Medicalytics

Projekt opiera się na klasycznej architekturze **Klient-Serwer**, rozdzielając interfejs użytkownika od logiki biznesowej i bazy danych.

## Komponenty Systemu

### 1. Frontend (Aplikacja Desktopowa)
* **Technologia:** JavaFX (Java 17+)
* **Rola:** Interfejs graficzny użytkownika (GUI). Aplikacja nie łączy się bezpośrednio z bazą danych, lecz komunikuje się z Backendem za pomocą wbudowanego klienta `java.net.http.HttpClient`.
* **Budowa:** Projekt zarządzany przez narzędzie Maven (`javafx-maven-plugin`).

### 2. Backend (Serwer REST API)
* **Technologia:** Java, **Spring Boot 4.0.3**
* **Rola:** Obsługuje żądania HTTP od klienta, weryfikuje dane i zarządza bezpieczeństwem.
* **Bezpieczeństwo:** Wykorzystuje **Spring Security** oraz dedykowany `AuthController`. Hasła użytkowników są zabezpieczone przy użyciu algorytmu **BCrypt** (szyfrowanie jednokierunkowe).
* **Konfiguracja:** Klasa `SecurityConfig` zarządza dostępem do endpointów i definiuje Bean dla `PasswordEncoder`.

### 3. Baza Danych i Zarządzanie Schematem
* **Technologia:** MariaDB (Relacyjna Baza Danych)
* **Zarządzanie strukturą (Flyway):** System wykorzystuje narzędzie **Flyway** do wersjonowania bazy danych.
    * Skrypty SQL znajdują się w `src/main/resources/db/migration`.
    * Przy starcie aplikacji klasa `FlywayConfig` wymusza uruchomienie migracji, zapewniając identyczną strukturę tabel u wszystkich członków zespołu.
* **Komunikacja:** Mapowanie obiektowo-relacyjne (ORM) realizowane przez **Spring Data JPA / Hibernate**.

## Przepływ Danych

### Zarządzanie Bazą (Migracje)
1. Podczas startu serwera Flyway sprawdza tabelę `flyway_schema_history`.
2. Jeśli w folderze `db/migration` znajdują się nowe pliki (np. `V1`, `V2`), Flyway wykonuje je sekwencyjnie.
3. Gwarantuje to, że baza danych `medicalytics` zawsze posiada aktualne tabele (np. `users`, `dim_patient`, `fact_test_results`).

### Proces Autoryzacji (Logowanie)
1. Użytkownik wpisuje dane w oknie JavaFX.
2. Frontend wysyła żądanie `POST` na endpoint `/api/auth/login`.
3. `AuthController` wyszukuje użytkownika w bazie poprzez `UserRepository`.
4. `BCryptPasswordEncoder` porównuje wpisane hasło (tekst jawny) z bezpiecznym hashem przechowywanym w kolumnie `password_hash`.
5. Serwer zwraca informację o sukcesie lub błędzie, na podstawie której Frontend decyduje o zmianie widoku.