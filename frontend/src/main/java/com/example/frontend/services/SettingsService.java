package com.example.frontend.services;

import com.example.frontend.models.AccountInfo;
import com.example.frontend.models.DictionaryFetchResult;
import com.example.frontend.models.DictionaryImportResult;
import com.example.frontend.models.PasswordChangeRequest;
import com.example.frontend.models.TestTypeEntry;
import com.example.frontend.utils.ApiConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SettingsService {

    private static final Gson gson = new Gson();
    private static final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
    private static final String SESSION_PREFIX = "Zalogowany jako: ";
    private static final Type DICTIONARY_LIST_TYPE = new TypeToken<List<TestTypeEntry>>() {}.getType();

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
                .thenApply(HttpResponse::body)
                .exceptionally(ex -> "Błąd połączenia: " + ex.getMessage());
    }

    public static CompletableFuture<DictionaryFetchResult> fetchDictionary() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(ApiConfig.apiUri("/api/settings/dictionary"))
                .GET()
                .build();

        return AuthService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        List<TestTypeEntry> entries = gson.fromJson(response.body(), DICTIONARY_LIST_TYPE);
                        if (entries == null) {
                            entries = Collections.emptyList();
                        }
                        return new DictionaryFetchResult(entries, null);
                    }
                    if (response.statusCode() == 401) {
                        return new DictionaryFetchResult(Collections.emptyList(),
                                "Brak autoryzacji. Zaloguj się ponownie.");
                    }
                    return new DictionaryFetchResult(Collections.emptyList(),
                            "Błąd API (" + response.statusCode() + "): " + response.body());
                })
                .exceptionally(ex -> new DictionaryFetchResult(Collections.emptyList(),
                        "Błąd połączenia: " + ex.getMessage()));
    }

    public static CompletableFuture<String> updateDictionaryEntry(TestTypeEntry entry) {
        String jsonBody = gson.toJson(entry);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(ApiConfig.apiUri("/api/settings/dictionary/" + entry.getTestCode()))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return AuthService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() == 200
                        ? "Wpis słownika został zaktualizowany."
                        : response.body())
                .exceptionally(ex -> "Błąd połączenia: " + ex.getMessage());
    }

    public static CompletableFuture<String> exportDictionaryJson() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(ApiConfig.apiUri("/api/settings/dictionary/export"))
                .GET()
                .build();

        return AuthService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() == 200
                        ? prettyGson.toJson(gson.fromJson(response.body(), DICTIONARY_LIST_TYPE))
                        : response.body())
                .exceptionally(ex -> "Błąd połączenia: " + ex.getMessage());
    }

    public static CompletableFuture<String> importDictionaryJson(String jsonContent) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(ApiConfig.apiUri("/api/settings/dictionary/import"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonContent))
                .build();

        return AuthService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        DictionaryImportResult result = gson.fromJson(response.body(), DictionaryImportResult.class);
                        return result != null && result.getMessage() != null
                                ? result.getMessage()
                                : "Słownik został zaimportowany.";
                    }
                    return response.body();
                })
                .exceptionally(ex -> "Błąd połączenia: " + ex.getMessage());
    }
}
