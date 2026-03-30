## Szybki Test Integracji (Dane Testowe)

Aby upewnić się, że komunikacja między Frontend i Backend działa poprawnie, wykonaj poniższy scenariusz testowy:

1. **Uruchom Backend**, upewniając się, że Flyway stworzył tabele.
2. **Uruchom Frontend** i przejdź do widoku rejestracji.
3. **Przykładowe dane do testu:**
- **Login:** `tester_jan`
- **Hasło:** `Projekt2026!`
4. **Weryfikacja:**
- Po kliknięciu "Zarejestruj", system powinien zwrócić komunikat o sukcesie.
- W bazie danych (MariaDB), w tabeli `users`, powinien pojawić się rekord `tester_jan` z zaszyfrowanym hasłem.
- Spróbuj zalogować się tymi samymi danymi – system powinien przenieść Cię do Dashboardu.