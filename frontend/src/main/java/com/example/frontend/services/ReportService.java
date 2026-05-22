package com.example.frontend.services;

import com.example.frontend.models.ReportSummary;
import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class ReportService {
    private static final Gson gson = new Gson();

    public static CompletableFuture<ReportSummary> fetchSummary() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/reports/summary"))
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
}