package com.example.frontend.services;

import com.example.frontend.models.FileItem;
import com.example.frontend.models.ProcessingError;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collections;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class FileService {
    private static final Gson gson = new Gson();


    public static CompletableFuture<List<FileItem>> fetchFiles() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/files"))
                .GET()
                .build();

        return AuthService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        List<FileItem> allFiles = gson.fromJson(response.body(),
                                new TypeToken<List<FileItem>>(){}.getType());

                        // TO JEST TA KLUCZOWA LOGIKA ZE STAREGO MAIN:
                        allFiles.removeIf(f -> "DELETED".equals(f.status));

                        return allFiles;
                    }
                    return List.of();
                });
    }
    public static CompletableFuture<String> uploadFile(java.io.File file) {
        try {
            String boundary = "---" + System.currentTimeMillis();

            // Budowanie ciała zapytania (logika, ktora byla w Main)
            byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());
            String head = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n" +
                    "Content-Type: text/csv\r\n\r\n";
            String tail = "\r\n--" + boundary + "--\r\n";

            java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
            os.write(head.getBytes());
            os.write(fileContent);
            os.write(tail.getBytes());
            byte[] body = os.toByteArray();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/files/upload"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            return AuthService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(res -> res.statusCode() == 200 ? "Wgrano plik!" : "Błąd serwera: " + res.statusCode());

        } catch (Exception e) {
            return CompletableFuture.completedFuture("Błąd lokalny: " + e.getMessage());
        }
    }
    // Metoda do przetwarzania
    public static CompletableFuture<String> processFile(Long id) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/files/" + id + "/process"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return AuthService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
    }

    // Metoda do usuwania
    public static CompletableFuture<String> deleteFile(Long id) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/files/" + id + "/delete"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return AuthService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(res -> res.statusCode() == 200 ? "Usunięto pomyślnie" : "Błąd usuwania");
    }

    // Metoda do podglądu
    public static CompletableFuture<String[]> getFilePreview(Long id) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/files/" + id + "/preview"))
                .GET()
                .build();
        return AuthService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(res -> new com.google.gson.Gson().fromJson(res.body(), String[].class));
    }

    // Metoda do wyswietlania bledow
    public static CompletableFuture<List<ProcessingError>> getFileErrors(Long fileId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/files/" + fileId + "/errors"))
                .GET()
                .build();

        return AuthService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        Type listType = new TypeToken<ArrayList<ProcessingError>>(){}.getType();
                        return gson.fromJson(response.body(), listType);
                    }
                    return Collections.<ProcessingError>emptyList();
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return Collections.<ProcessingError>emptyList();
                });
    }
}
