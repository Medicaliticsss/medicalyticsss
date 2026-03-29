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
        passwordInput.setPromptText("Password");
        passwordInput.setMaxWidth(200);
        Label errorLabel = new Label();
        //przycisk logowania
        Button loginButton = new Button("Log in");
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
        Button registerButton = new Button("Register");
        registerButton.setOnAction(e -> window.setScene(registerScene));

        layout.getChildren().addAll(titleLabel, usernameInput, passwordInput, loginButton, registerButton, errorLabel);
        loginScene = new Scene(layout, 400, 350);
    }

    private void createRegisterScene() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #FFD580;");
        Label titleLabel = new Label("Medicalytics Registration");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        TextField usernameInput = new TextField();
        usernameInput.setPromptText("Create username");
        usernameInput.setMaxWidth(200);
        PasswordField passwordInput = new PasswordField();
        passwordInput.setPromptText("Create password");
        passwordInput.setMaxWidth(200);
        PasswordField confirmPasswordInput = new PasswordField();
        confirmPasswordInput.setPromptText("Confirm password");
        confirmPasswordInput.setMaxWidth(200);
        Label statusLabel = new Label();
        Button registerButton = new Button("Create account");
        registerButton.setOnAction(e -> {
            String username = usernameInput.getText();
            String password = passwordInput.getText();
            String confirmPassword = confirmPasswordInput.getText();
            if (username.equals("") || password.equals("")) {
                statusLabel.setText("Empty Fields");
                return;
            }
            if (!password.equals(confirmPassword)) {
                statusLabel.setText("Passwords do not match!");
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
        Button backButton = new Button("Back to login");
        backButton.setOnAction(e -> window.setScene(loginScene));

        layout.getChildren().addAll(titleLabel, usernameInput, passwordInput, confirmPasswordInput, registerButton, backButton, statusLabel);
        registerScene = new Scene(layout, 400, 400);
    }
    private void createDashboardScene() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #FFF3E0;");

        Label welcomeLabel = new Label("Medicalytics");
        welcomeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label fileStatusLabel = new Label("No file loaded.");

        Button uploadCsvButton = new Button("Upload data file (CSV)");
        uploadCsvButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            File selectedFile = fileChooser.showOpenDialog(window);
            if (selectedFile != null) {
                fileStatusLabel.setText("File: " + selectedFile.getName());
            }
        });

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> window.setScene(loginScene));

        layout.getChildren().addAll(welcomeLabel, uploadCsvButton, fileStatusLabel, logoutButton);
        dashboardScene = new Scene(layout, 500, 400);
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