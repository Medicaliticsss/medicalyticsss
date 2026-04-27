package com.example.frontend;
//Importy
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.ListView;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Main extends Application {
    //główne okno i 3 sceny
    Stage window;
    Scene loginScene, registerScene, dashboardScene;
    private final HttpClient client = HttpClient.newBuilder()
            .cookieHandler(new java.net.CookieManager())
            .build();

    private void createLoginScene() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: orange;");
        //towrzenie formualrza
        Label titleLabel = new Label("Medicalytics");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        TextField usernameInput = new TextField();
        usernameInput.setPromptText("Login");
        usernameInput.setMaxWidth(200);
        PasswordField passwordInput = new PasswordField();
        passwordInput.setPromptText("Hasło");
        passwordInput.setMaxWidth(200);
        Label errorLabel = new Label();
        //przycisk logowania
        Button loginButton = new Button("Zaloguj się");
        loginButton.setOnAction(e -> {

            String formBody = "username=" + usernameInput.getText() + "&password=" + passwordInput.getText();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/auth/login")) // Dobry URL
                    .header("Content-Type", "application/x-www-form-urlencoded") // Dobry nagłówek
                    .POST(HttpRequest.BodyPublishers.ofString(formBody)) // Wysyłamy formBody
                    .build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                            // wyciągamy tekst odpowiedzi z backendu
                            String responseBody = response.body();
                            // spraedzenie czy jest ok
                            if (responseBody.equals("Zalogowano pomyślnie!")) {
                                window.setScene(dashboardScene);
                            } else {
                                errorLabel.setText(responseBody);
                            }
                        });
                    });
        });
        //przycisk rejestracji
        Button registerButton = new Button("Zarejestruj się");
        registerButton.setOnAction(e -> window.setScene(registerScene));

        layout.getChildren().addAll(titleLabel, usernameInput, passwordInput, loginButton, registerButton, errorLabel);
        loginScene = new Scene(layout, 400, 350);
    }

    private void createRegisterScene() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #FFD580;");
        Label titleLabel = new Label("Medicalytics Rejestracja");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        TextField usernameInput = new TextField();
        usernameInput.setPromptText("Stwórz login");
        usernameInput.setMaxWidth(200);
        PasswordField passwordInput = new PasswordField();
        passwordInput.setPromptText("Stwórz hasło");
        passwordInput.setMaxWidth(200);
        PasswordField confirmPasswordInput = new PasswordField();
        confirmPasswordInput.setPromptText("Potwierdź hasło");
        confirmPasswordInput.setMaxWidth(200);
        Label statusLabel = new Label();
        Button registerButton = new Button("Stwórz konto");
        registerButton.setOnAction(e -> {
            String username = usernameInput.getText();
            String password = passwordInput.getText();
            String confirmPassword = confirmPasswordInput.getText();
            if (username.equals("") || password.equals("")) {
                statusLabel.setText("Puste pola");
                return;
            }
            if (!password.equals(confirmPassword)) {
                statusLabel.setText("Hasła nie są takie same!");
                return;
            }

            String formBody = "username=" + username + "&password=" + password;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/auth/register"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                            String responseBody = response.body();
                            statusLabel.setText(responseBody);
                        });
                    });
        });
        Button backButton = new Button("Powrót do logowania");
        backButton.setOnAction(e -> window.setScene(loginScene));

        layout.getChildren().addAll(titleLabel, usernameInput, passwordInput, confirmPasswordInput, registerButton, backButton, statusLabel);
        registerScene = new Scene(layout, 400, 400);
    }
    private void createDashboardScene() {
        javafx.scene.layout.HBox mainLayout = new javafx.scene.layout.HBox(20);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.CENTER); // Wyśrodkowanie paneli
        mainLayout.setStyle("-fx-background-color: #FFF3E0;");

        // LEWY PANEL
        VBox previewPanel = new VBox(10);
        Label previewLabel = new Label("Podgląd zawartości pliku:");
        previewLabel.setStyle("-fx-font-weight: bold;");
        javafx.scene.control.TextArea previewArea = new javafx.scene.control.TextArea();
        previewArea.setEditable(false);
        previewArea.setPrefWidth(450); // Trochę szerzej dla wygody
        previewArea.setPrefHeight(500);
        previewPanel.getChildren().addAll(previewLabel, previewArea);

        // PRAWY PANEL
        VBox controlPanel = new VBox(10);
        controlPanel.setMinWidth(250); // Stała szerokość panelu sterowania

        ListView<FileItem> fileListView = new ListView<>();
        fileListView.setPrefHeight(200);

        Button refreshButton = new Button("Odśwież listę");
        Button uploadButton = new Button("Wgraj nowy plik");
        Button processButton = new Button("Przetwórz plik");
        Button previewButton = new Button("Podgląd");
        Button deleteButton = new Button("Usuń plik");
        Button logoutButton = new Button("Wyloguj");
        Label fileStatusLabel = new Label("Wybierz plik");

        refreshButton.setMaxWidth(Double.MAX_VALUE);
        uploadButton.setMaxWidth(Double.MAX_VALUE);
        processButton.setMaxWidth(Double.MAX_VALUE);
        previewButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        logoutButton.setMaxWidth(Double.MAX_VALUE);

        // Akcje
        refreshButton.setOnAction(e -> fetchFiles(fileListView, fileStatusLabel));
        uploadButton.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            File f = fc.showOpenDialog(window);
            if (f != null) sendCsvToBackend(f, fileStatusLabel);
        });
        processButton.setOnAction(e -> {
            FileItem s = fileListView.getSelectionModel().getSelectedItem();
            if (s != null) processFileOnBackend(s, fileStatusLabel, fileListView);
        });
        previewButton.setOnAction(e -> {
            FileItem s = fileListView.getSelectionModel().getSelectedItem();
            if (s != null) loadFilePreview(s, previewArea);
        });
        deleteButton.setOnAction(e -> {
            FileItem s = fileListView.getSelectionModel().getSelectedItem();
            if (s != null) deleteFileOnBackend(s, fileStatusLabel, fileListView);
        });
        logoutButton.setOnAction(e -> window.setScene(loginScene));

        // Listener blokowania
        fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean isSelected = (newV != null);
            boolean isDeleted = isSelected && "DELETED".equals(newV.status);
            previewButton.setDisable(!isSelected || isDeleted);
            deleteButton.setDisable(!isSelected || isDeleted);
            processButton.setDisable(!isSelected || !"UPLOADED".equals(newV.status));
        });

        controlPanel.getChildren().addAll(new Label("Medicalytics - Panel"), refreshButton, fileListView,
                uploadButton, processButton, previewButton, deleteButton,
                fileStatusLabel, logoutButton);

        mainLayout.getChildren().addAll(previewPanel, controlPanel);
        dashboardScene = new Scene(mainLayout, 900, 600);
    }

    private void sendCsvToBackend(File file, Label statusLabel) {
        try {
            String boundary = "---" + System.currentTimeMillis(); // Unikalny separator dla danych


            byte[] multipartBody = createMultipartBody(file, boundary);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/files/upload"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                            if (response.statusCode() == 200) {
                                statusLabel.setText("Sukces: " + response.body());
                            } else {
                                statusLabel.setText("Błąd (" + response.statusCode() + "): " + response.body());
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> statusLabel.setText("Błąd połączenia: " + ex.getMessage()));
                        return null;
                    });

        } catch (Exception e) {
            statusLabel.setText("Błąd systemowy: " + e.getMessage());
        }
    }

    // Ta metoda buduje strukturę pod @RequestParam("file") w Springu
    private byte[] createMultipartBody(File file, String boundary) throws Exception {
        String fileName = file.getName();
        byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());

        // Nagłówki części pliku
        String head = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" +
                "Content-Type: text/csv\r\n\r\n";

        String tail = "\r\n--" + boundary + "--\r\n";

        // Łączymy wszystko w jedną tablicę
        java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
        os.write(head.getBytes());
        os.write(fileContent);
        os.write(tail.getBytes());

        return os.toByteArray();
    }

    @Override
    public void start(Stage stage) {
        window = stage;
        window.setTitle("Medicalytics");

        createLoginScene();
        createRegisterScene();
        createDashboardScene();

        window.setScene(loginScene);
        window.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
    private void fetchFiles(ListView<FileItem> listView, Label statusLabel) {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/files"))
                .GET().build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    Platform.runLater(() -> {
                        if (res.statusCode() == 200) {
                            // Używamy Gson do zamiany JSON na Listę FileItem
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<FileItem>>(){}.getType();
                            java.util.List<FileItem> files = gson.fromJson(res.body(), listType);

                            files.removeIf(f -> "DELETED".equals(f.status));

                            listView.setItems(javafx.collections.FXCollections.observableArrayList(files));
                            statusLabel.setText("Lista odświeżona.");
                        }
                    });
                });
    }

    private void processFileOnBackend(FileItem item, Label statusLabel, ListView<FileItem> listView) {

        // Zmieniamy na POST i używamy ID w URL - dokładnie tak, jak zrobiłaś w Controllerze!
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/files/" + item.id + "/process"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    Platform.runLater(() -> {
                        // Wyświetlamy Alert z wynikiem (wymaganie funkcjonalne)
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.setTitle("Wynik przetwarzania");
                        alert.setHeaderText(null);
                        alert.setContentText(res.body());
                        alert.showAndWait();

                        // Odświeżamy listę, żeby zobaczyć nowy status (np. SUCCESS)
                        fetchFiles(listView, statusLabel);
                    });
                });
    }
    private void deleteFileOnBackend(FileItem item, Label statusLabel, ListView<FileItem> listView) {

        // Adres URL zgodny z planem Twojego kolegi
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/files/" + item.id + "/delete"))
                .POST(HttpRequest.BodyPublishers.noBody()) // Używamy POST, bo zmieniamy status w bazie
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    Platform.runLater(() -> {
                        if (res.statusCode() == 200) {
                            statusLabel.setText("Plik został usunięty.");
                            fetchFiles(listView, statusLabel); // Odświeżamy listę, żeby plik zniknął
                        } else {
                            statusLabel.setText("Błąd: " + res.body());
                        }
                    });
                }).exceptionally(ex -> {
                    Platform.runLater(() -> statusLabel.setText("Błąd połączenia: " + ex.getMessage()));
                    return null;
                });
    }
    private void loadFilePreview(FileItem item, javafx.scene.control.TextArea previewArea) {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/files/" + item.id + "/preview"))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    Platform.runLater(() -> {
                        if (res.statusCode() == 200) {
                            // Parsowanie JSON (tablica stringów)
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            String[] lines = gson.fromJson(res.body(), String[].class);

                            // Łączenie linii w jeden tekst z nowymi liniami
                            previewArea.setText(String.join("\n", lines));
                        } else {
                            // OBSŁUGA BŁĘDÓW (Alert)
                            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                            alert.setTitle("Błąd podglądu");
                            alert.setHeaderText("Nie udało się pobrać podglądu pliku.");
                            alert.setContentText("Kod błędu: " + res.statusCode() + "\nSerwer mówi: " + res.body());
                            alert.showAndWait();
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        previewArea.setText("Błąd połączenia: " + ex.getMessage());
                    });
                    return null;
                });
    }
}
class FileItem {
    Long id;
    String fileName; // Musi być fileName (zgodnie z backendem)
    String status;

    @Override
    public String toString() {
        return fileName + " [" + status + "]";
    }
}