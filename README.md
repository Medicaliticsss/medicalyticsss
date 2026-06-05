# Medicalytics

Medicalytics to aplikacja desktopowa do wgrywania medycznych plików CSV, przetwarzania ich do bazy analitycznej oraz tworzenia raportów i wykresów na podstawie zanonimizowanych wyników badań.

System składa się z **klienta desktopowego JavaFX** oraz **API Spring Boot** opartego na **MariaDB**.

## Pobieranie (Windows)

**[Pobierz najnowszy pakiet na Windows](https://github.com/Medicaliticsss/medicalyticsss/releases/latest)**

Bezpośredni link: [medicalytics-windows-portable.zip](https://github.com/Medicaliticsss/medicalyticsss/releases/latest/download/medicalytics-windows-portable.zip)

1. Rozpakuj archiwum zip
2. Kliknij dwukrotnie **`Medicalytics.cmd`**
3. Poczekaj 1–2 minuty przy pierwszym uruchomieniu (konfiguracja lokalnej bazy danych)

Nie trzeba instalować Javy, Dockera ani MariaDB. Dane użytkownika są przechowywane w `%LOCALAPPDATA%\Medicalytics`.

Pakiet jest budowany automatycznie przy każdym pushu do `main` i publikowany w zakładce [Releases](https://github.com/Medicaliticsss/medicalyticsss/releases).

## Podstawowy przepływ pracy

```
Pobierz → Uruchom → Rejestracja / Logowanie → Wgraj CSV → Przetwórz plik → Raporty → Ustawienia
```

| Krok | Co robisz | Co się dzieje |
|------|-----------|---------------|
| 1. **Start** | Uruchom `Medicalytics.cmd` (lub API + aplikację desktopową w trybie deweloperskim) | W pakiecie przenośnym API i baza danych startują automatycznie |
| 2. **Logowanie** | Zarejestruj konto lub zaloguj się | Tworzona jest sesja; akcje są przypisane do użytkownika |
| 3. **Pliki** | Wgraj plik `.csv` z ekranu **Pliki** | Plik trafia na serwer; status to `UPLOADED` |
| 4. **Podgląd** | Otwórz podgląd wybranego pliku | Serwer strumieniuje pierwsze wiersze bez ładowania całego pliku |
| 5. **Przetwarzanie** | Kliknij **Przetwórz plik** | ETL waliduje wiersze, anonimizuje PESEL, ładuje wyniki do hurtowni |
| 6. **Raporty** | Otwórz **Raporty** | Twórz wykresy i tabele z przetworzonych danych (BI / OLAP) |
| 7. **Ustawienia** | Otwórz **Ustawienia** | Zmień hasło, sprawdź konto, zarządzaj słownikiem badań (MDM) |

Przykładowe pliki CSV do testów znajdują się w folderze [`csv/`](csv/).

## Dokumentacja

- **[Przewodnik aplikacji i referencja API](docs/app.md)** — opcje uruchomienia, endpointy, format CSV, ustawienia
- **[Architektura](docs/architecture.md)** — projekt systemu, komponenty, przepływy danych

## Szybki start dla deweloperów

Dla osób uruchamiających projekt ze źródeł:

```bash
docker compose up -d --build
cd frontend && ../backend/mvnw -Pdev javafx:run
```

Pełna instrukcja uruchomienia deweloperskiego: [docs/app.md](docs/app.md).
