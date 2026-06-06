package com.example.frontend.services;

import com.example.frontend.models.UserSession;
import com.example.frontend.utils.ApiConfig;
import java.net.CookieManager;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class AuthService {

    // Jeden wspólny klient dla całej aplikacji (obsługuje ciasteczka)
    private static final HttpClient client = HttpClient.newBuilder()
            .cookieHandler(new CookieManager())
            .build();

    public static HttpClient getClient() {
        return client;
    }

    // --- LOGOWANIE ---
    public static CompletableFuture<String> login(String username, String password) {
        // Przygotowujemy dane (zakładamy prosty format tekstowy lub JSON)
        String credentials = "username=" + username + "&password=" + password;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(ApiConfig.apiUri("/api/auth/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(credentials))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        // Jeśli sukces, zapisujemy użytkownika w sesji lokalnej
                        UserSession.getInstance().startSession(username);
                        return "SUCCESS";
                    } else {
                        return "Błąd logowania: " + response.body();
                    }
                })
                .exceptionally(ex -> "Błąd połączenia z serwerem");
    }

    // --- WYLOGOWANIE ---
    public static CompletableFuture<Boolean> logout() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(ApiConfig.apiUri("/api/auth/logout"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    // Czyścimy wszystko lokalnie niezależnie od odpowiedzi serwera
                    UserSession.getInstance().clearSession();
                    ((CookieManager) client.cookieHandler().get()).getCookieStore().removeAll();
                    return true;
                })
                .exceptionally(ex -> {
                    // Nawet jak serwer padnie, wyloguj nas lokalnie
                    UserSession.getInstance().clearSession();
                    return true;
                });
    }
    // ---- REJESTRACJA
    public static CompletableFuture<String> register(String username, String password) {
        String credentials = "username=" + username + "&password=" + password;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(ApiConfig.apiUri("/api/auth/register"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(credentials))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    // Backend zwraca "Zarejestrowano pomyślnie!" lub komunikat błędu
                    return response.body();
                })
                .exceptionally(ex -> "Błąd połączenia: " + ex.getMessage());
    }

    // --- SPRAWDZANIE SESJI  ---
    public static CompletableFuture<Boolean> checkSession() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(ApiConfig.apiUri("/api/auth/me"))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        String body = response.body();
                        String prefix = "Zalogowany jako: ";
                        String username = body.startsWith(prefix)
                                ? body.substring(prefix.length()).trim()
                                : body.trim();
                        UserSession.getInstance().startSession(username);
                        return true;
                    }
                    return false;
                })
                .exceptionally(ex -> false);
    }
}