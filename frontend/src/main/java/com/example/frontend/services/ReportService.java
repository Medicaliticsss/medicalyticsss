package com.example.frontend.services;

import com.example.frontend.models.CustomReportRequest;
import com.example.frontend.models.ReportDataPoint;
import com.example.frontend.models.ReportSummary;
import com.example.frontend.models.SeriesReportDataPoint;
import com.example.frontend.models.SeriesReportRequest;
import com.example.frontend.utils.ApiConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ReportService {
    private static final Gson gson = new Gson();

    // Podsumowanie (Kafelki na głównym Dashboardzie)
    public static CompletableFuture<ReportSummary> fetchSummary() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(ApiConfig.apiUri("/api/reports/summary"))
                .GET()
                .build();

        return AuthService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return gson.fromJson(response.body(), ReportSummary.class);
                    }
                    return null;
                });
    }

    // Agregacje i Wykresy (Kreator Raportów BI)
    public static CompletableFuture<List<ReportDataPoint>> fetchCustomReport(CustomReportRequest requestBody) {
        try {
            String jsonBody = gson.toJson(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(ApiConfig.apiUri("/api/reports/custom"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            return AuthService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            Type listType = new TypeToken<List<ReportDataPoint>>(){}.getType();
                            return gson.fromJson(response.body(), listType);
                        } else {
                            System.err.println("Błąd pobierania raportu: " + response.statusCode());
                            return List.of();
                        }
                    });
        } catch (Exception e) {
            System.err.println("Błąd lokalny: " + e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        }
    }

    // Elastyczne wiersze raportowe (agregacja jest opcjonalna)
    public static CompletableFuture<List<Map<String, Object>>> fetchCustomReportRows(CustomReportRequest requestBody) {
        try {
            String jsonBody = gson.toJson(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(ApiConfig.apiUri("/api/reports/custom/table"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            return AuthService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                            return gson.fromJson(response.body(), listType);
                        } else {
                            System.err.println("Błąd pobierania tabeli raportu: " + response.statusCode());
                            return List.of();
                        }
                    });
        } catch (Exception e) {
            System.err.println("Błąd lokalny (tabela raportu): " + e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        }
    }

    // Dane wieloseryjne do wykresów
    public static CompletableFuture<List<SeriesReportDataPoint>> fetchSeriesReport(SeriesReportRequest requestBody) {
        try {
            String jsonBody = gson.toJson(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(ApiConfig.apiUri("/api/reports/series"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            return AuthService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            Type listType = new TypeToken<List<SeriesReportDataPoint>>(){}.getType();
                            return gson.fromJson(response.body(), listType);
                        } else {
                            System.err.println("Błąd pobierania raportu seryjnego: " + response.statusCode());
                            return List.of();
                        }
                    });
        } catch (Exception e) {
            System.err.println("Błąd lokalny (raport seryjny): " + e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        }
    }

    // Surowe dane (Tabela SELECT *)
    public static CompletableFuture<List<Map<String, Object>>> fetchRawData(CustomReportRequest requestBody) {
        try {
            String jsonBody = gson.toJson(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(ApiConfig.apiUri("/api/reports/raw"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            return AuthService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            // Magia: Gson sam zamieni JSON na listę dynamicznych słowników!
                            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                            return gson.fromJson(response.body(), listType);
                        } else {
                            System.err.println("Błąd pobierania danych surowych: " + response.statusCode());
                            return List.of();
                        }
                    });
        } catch (Exception e) {
            System.err.println("Błąd lokalny (dane surowe): " + e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        }
    }
}