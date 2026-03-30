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

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Main extends Application {
    //główne okno i 3 sceny
    Stage window;
    Scene loginScene, registerScene, dashboardScene;

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
            HttpClient client = HttpClient.newHttpClient();
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
            HttpClient client = HttpClient.newHttpClient();
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
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #FFF3E0;");

        Label welcomeLabel = new Label("Medicalytics - Panel");
        welcomeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label fileStatusLabel = new Label("Brak wybranych plików");

        Button uploadCsvButton = new Button("Upload data file (CSV)");
        uploadCsvButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            // Filtrujemy tylko pliki CSV
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pliki CSV", "*.csv"));

            File selectedFile = fileChooser.showOpenDialog(window);

            if (selectedFile != null) {
                fileStatusLabel.setText("Wysyłanie: " + selectedFile.getName() + "...");
                // Wywołujemy nową metodę wysyłającą plik
                sendCsvToBackend(selectedFile, fileStatusLabel);
            }
        });

        Button logoutButton = new Button("Wyloguj się");
        logoutButton.setOnAction(e -> window.setScene(loginScene));

        layout.getChildren().addAll(welcomeLabel, uploadCsvButton, fileStatusLabel, logoutButton);
        dashboardScene = new Scene(layout, 500, 400);
    }

    private void sendCsvToBackend(File file, Label statusLabel) {
        try {
            String boundary = "---" + System.currentTimeMillis(); // Unikalny separator dla danych
            HttpClient client = HttpClient.newHttpClient();

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
}