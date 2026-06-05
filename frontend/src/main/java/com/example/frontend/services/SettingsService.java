package com.example.frontend.services;

import com.example.frontend.models.AccountInfo;
import com.example.frontend.models.PasswordChangeRequest;
import com.example.frontend.utils.ApiConfig;
import com.google.gson.Gson;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class SettingsService {

    private static final Gson gson = new Gson();
    private static final String SESSION_PREFIX = "Zalogowany jako: ";

    private SettingsService() {
    }

    public static CompletableFuture<AccountInfo> fetchAccountInfo() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(ApiConfig.apiUri("/api/auth/me"))
                .GET()
                .build();

        return AuthService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        String message = response.body();
                        String username = message.startsWith(SESSION_PREFIX)
                                ? message.substring(SESSION_PREFIX.length()).trim()
                                : message.trim();
                        return new AccountInfo(username, true, message);
                    }
                    return new AccountInfo(null, false, response.body());
                })
                .exceptionally(ex -> new AccountInfo(null, false, "Błąd połączenia z serwerem"));
    }

    public static CompletableFuture<String> changePassword(String oldPassword, String newPassword) {
        String jsonBody = gson.toJson(new PasswordChangeRequest(oldPassword, newPassword));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(ApiConfig.apiUri("/api/settings/password"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return AuthService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return response.body();
                    }
                    return response.body();
                })
                .exceptionally(ex -> "Błąd połączenia: " + ex.getMessage());
    }
}
