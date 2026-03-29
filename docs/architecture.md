# Architektura Systemu Medicalytics

Projekt opiera się na klasycznej architekturze **Klient-Serwer**, rozdzielając interfejs użytkownika od logiki biznesowej i bazy danych.

## Komponenty Systemu

### 1. Frontend (Aplikacja Desktopowa)
* **Technologia:** JavaFX (Java 17+)
* **Rola:** Interfejs graficzny użytkownika (GUI). Aplikacja nie łączy się bezpośrednio z bazą danych, lecz komunikuje się z Backendem za pomocą wbudowanego klienta `java.net.http.HttpClient`.
* **Budowa:** Projekt zarządzany przez narzędzie Maven (`javafx-maven-plugin`).

### 2. Backend (Serwer REST API)
* **Technologia:** Java, Spring Boot 3
* **Rola:** Obsługuje żądania HTTP od klienta, weryfikuje dane i zarządza bezpieczeństwem.
* **Bezpieczeństwo:** Wykorzystuje **Spring Security**. Hasła użytkowników nie są przechowywane otwartym tekstem – są hashowane przy użyciu algorytmu **BCrypt**.

### 3. Baza Danych
* **Technologia:** MariaDB (Relacyjna Baza Danych)
* **Rola:** Trwałe przechowywanie danych systemu, w tym poświadczeń użytkowników (tabela `users` z kolumnami `username` i `password_hash`). Połączenie z Backendem realizowane jest przez Spring Data JPA/Hibernate.

## Przepływ Danych (Logowanie)
1. Użytkownik wpisuje dane w oknie JavaFX.
2. Frontend wysyła żądanie `POST` z danymi do Backendu.
3. Backend pobiera z bazy danych hash hasła dla podanego loginu.
4. BCrypt porównuje wpisane hasło z hashem z bazy.
5. Backend zwraca odpowiedź (sukces/błąd) do Frontendu, który odpowiednio zmienia widok (na Dashboard).