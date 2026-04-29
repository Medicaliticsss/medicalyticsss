package com.example.frontend;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import atlantafx.base.theme.Dracula;
import atlantafx.base.theme.Styles;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

public class Main extends Application {
    Stage window;
    Scene loginScene, registerScene, dashboardScene, mainMenuScene, settingsScene, reportScene;

    // Pola klasowe - używamy ich w wielu metodach
    ListView<FileItem> fileListView;
    Label fileStatusLabel;

    // Jeden wspólny klient dla całej aplikacji (obsługuje sesje/ciasteczka)
    private final HttpClient client = HttpClient.newBuilder()
            .cookieHandler(new java.net.CookieManager())
            .build();

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());
        window = stage;
        window.setTitle("Medicalytics");
        window.setMaximized(true);

        // Inicjalizacja scen
        createLoginScene();
        createRegisterScene();
        createDashboardScene();
        createMainMenuScene();
        createReportScene();
        createSettingsScene();

        window.setScene(loginScene);
        window.show();
    }

    private void createLoginScene() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Medicalytics");
        titleLabel.getStyleClass().add(Styles.TITLE_2);
        titleLabel.setStyle("-fx-text-fill: #FF0055;");
        DropShadow neonGlow = new DropShadow();
        neonGlow.setColor(Color.web("#FF0055"));
        titleLabel.setEffect(neonGlow);

        TextField usernameInput = new TextField();
        usernameInput.setPromptText("Login");
        usernameInput.setMaxWidth(250);

        PasswordField passwordInput = new PasswordField();
        passwordInput.setPromptText("Hasło");
        passwordInput.setMaxWidth(250);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add(Styles.DANGER);

        Button loginButton = new Button("Zaloguj się");
        loginButton.setMaxWidth(250);
        loginButton.getStyleClass().add(Styles.ACCENT);

        loginButton.setOnAction(e -> {
            String formBody = "username=" + usernameInput.getText() + "&password=" + passwordInput.getText();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/auth/login"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                            String responseBody = response.body();
                            if (responseBody.equals("Zalogowano pomyślnie!")) {
                                fetchFiles(fileListView, fileStatusLabel);
                                window.setScene(mainMenuScene);
                            } else {
                                errorLabel.setText(responseBody);
                            }
                        });
                    });
        });

        Button registerButton = new Button("Zarejestruj się");
        registerButton.setMaxWidth(250);
        registerButton.getStyleClass().add(Styles.BUTTON_OUTLINED);
        registerButton.setOnAction(e -> window.setScene(registerScene));

        layout.getChildren().addAll(titleLabel, usernameInput, passwordInput, loginButton, registerButton, errorLabel);
        loginScene = new Scene(layout, 400, 450);
    }

    private void createRegisterScene() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Rejestracja");
        titleLabel.getStyleClass().add(Styles.TITLE_2);

        TextField usernameInput = new TextField();
        usernameInput.setPromptText("Stwórz login");
        usernameInput.setMaxWidth(250);

        PasswordField passwordInput = new PasswordField();
        passwordInput.setPromptText("Stwórz hasło");
        passwordInput.setMaxWidth(250);

        PasswordField confirmPasswordInput = new PasswordField();
        confirmPasswordInput.setPromptText("Potwierdź hasło");
        confirmPasswordInput.setMaxWidth(250);

        Label statusLabel = new Label();

        Button registerButton = new Button("Stwórz konto");
        registerButton.setMaxWidth(250);
        registerButton.getStyleClass().add(Styles.ACCENT);

        registerButton.setOnAction(e -> {
            String username = usernameInput.getText();
            String password = passwordInput.getText();
            String confirmPassword = confirmPasswordInput.getText();
            if (username.isEmpty() || password.isEmpty()) {
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
        backButton.getStyleClass().add(Styles.BUTTON_OUTLINED);
        backButton.setOnAction(e -> window.setScene(loginScene));

        layout.getChildren().addAll(titleLabel, usernameInput, passwordInput, confirmPasswordInput, registerButton, backButton, statusLabel);
        registerScene = new Scene(layout, 400, 550);
    }

    private void createDashboardScene() {
        javafx.scene.layout.HBox mainLayout = new javafx.scene.layout.HBox(20);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.CENTER);

        // LEWY PANEL (PODGLĄD)
        VBox previewPanel = new VBox(10);
        Label previewLabel = new Label("Podgląd zawartości pliku:");
        previewLabel.setStyle("-fx-font-weight: bold;");
        TextArea previewArea = new TextArea();
        previewArea.setEditable(false);
        previewArea.setPrefWidth(450);
        previewArea.setPrefHeight(500);
        previewPanel.getChildren().addAll(previewLabel, previewArea);

        // PRAWY PANEL (STEROWANIE)
        VBox controlPanel = new VBox(10);
        controlPanel.setMinWidth(300);
        controlPanel.setAlignment(Pos.TOP_CENTER);

        Label welcomeLabel = new Label("Medicalytics - Pliki");
        welcomeLabel.getStyleClass().add(Styles.TITLE_3);

        fileListView = new ListView<>(); // Używamy pola klasowego
        fileListView.setPrefHeight(300);

        Button refreshButton = new Button("Odśwież listę");
        Button uploadButton = new Button("Wgraj nowy plik");
        Button processButton = new Button("Przetwórz plik");
        Button previewButton = new Button("Podgląd");
        Button deleteButton = new Button("Usuń plik");
        Button backButton = new Button("Wróć do menu");
        fileStatusLabel = new Label("Wybierz plik z listy");

        // Stylizacja
        deleteButton.getStyleClass().add(Styles.DANGER);
        processButton.getStyleClass().add(Styles.ACCENT);
        backButton.getStyleClass().add(Styles.BUTTON_OUTLINED);

        // Szerokość przycisków
        refreshButton.setMaxWidth(Double.MAX_VALUE);
        uploadButton.setMaxWidth(Double.MAX_VALUE);
        processButton.setMaxWidth(Double.MAX_VALUE);
        previewButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setMaxWidth(Double.MAX_VALUE);

        // Listener blokowania
        fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean isSelected = (newV != null);
            boolean isDeleted = isSelected && "DELETED".equals(newV.status);
            previewButton.setDisable(!isSelected || isDeleted);
            deleteButton.setDisable(!isSelected || isDeleted);
            processButton.setDisable(!isSelected || !"UPLOADED".equals(newV.status));
        });

        // Akcje
        refreshButton.setOnAction(e -> fetchFiles(fileListView, fileStatusLabel));
        uploadButton.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
            File file = fc.showOpenDialog(window);
            if (file != null) sendCsvToBackend(file, fileStatusLabel);
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
        backButton.setOnAction(e -> window.setScene(mainMenuScene));

        controlPanel.getChildren().addAll(welcomeLabel, refreshButton, fileListView,
                uploadButton, processButton, previewButton, deleteButton,
                fileStatusLabel, backButton);

        mainLayout.getChildren().addAll(previewPanel, controlPanel);
        dashboardScene = new Scene(mainLayout, 900, 650);
    }

    private void createMainMenuScene() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(40));

        Label titleLabel = new Label("Główne Menu");
        titleLabel.getStyleClass().add(Styles.TITLE_1);

        Button logoutButton = new Button("Wyloguj");
        logoutButton.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER);
        logoutButton.setOnAction(e -> window.setScene(loginScene));

        javafx.scene.layout.StackPane topContainer = new javafx.scene.layout.StackPane();
        topContainer.getChildren().addAll(titleLabel, logoutButton);
        javafx.scene.layout.StackPane.setAlignment(logoutButton, Pos.CENTER_RIGHT);
        root.setTop(topContainer);

        javafx.scene.layout.HBox cardsContainer = new javafx.scene.layout.HBox(20);
        cardsContainer.setAlignment(Pos.CENTER);

        Button filesButton = createMenuCard("Pliki");
        Button reportsButton = createMenuCard("Raporty");
        Button settingsButton = createMenuCard("Ustawienia");

        filesButton.setOnAction(e -> window.setScene(dashboardScene));
        reportsButton.setOnAction(e -> window.setScene(reportScene));
        settingsButton.setOnAction(e -> window.setScene(settingsScene));

        cardsContainer.getChildren().addAll(filesButton, reportsButton, settingsButton);
        root.setCenter(cardsContainer);

        mainMenuScene = new Scene(root, 1000, 700);
    }

    private void createReportScene() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        Label label = new Label("TU BĘDĄ RAPORTY");
        Button backButton = new Button("Wróć do Menu");
        backButton.setOnAction(e -> window.setScene(mainMenuScene));
        root.getChildren().addAll(label, backButton);
        reportScene = new Scene(root, 800, 600);
    }

    private void createSettingsScene() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        Label label = new Label("TU BĘDĄ USTAWIENIA");
        Button backButton = new Button("Wróć do Menu");
        backButton.setOnAction(e -> window.setScene(mainMenuScene));
        root.getChildren().addAll(label, backButton);
        settingsScene = new Scene(root, 800, 600);
    }

    private Button createMenuCard(String text) {
        Button card = new Button(text);
        card.setPrefSize(200, 200);
        card.getStyleClass().addAll(Styles.ELEVATED_2, Styles.TITLE_3);
        return card;
    }

    private void sendCsvToBackend(File file, Label statusLabel) {
        try {
            String boundary = "---" + System.currentTimeMillis();
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
                                statusLabel.setText("Sukces: Wgrano plik");
                                fetchFiles(fileListView, statusLabel);
                            } else {
                                statusLabel.setText("Błąd " + response.statusCode());
                            }
                        });
                    });
        } catch (Exception e) {
            statusLabel.setText("Błąd: " + e.getMessage());
        }
    }

    private byte[] createMultipartBody(File file, String boundary) throws Exception {
        String fileName = file.getName();
        byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());
        String head = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" +
                "Content-Type: text/csv\r\n\r\n";
        String tail = "\r\n--" + boundary + "--\r\n";
        java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
        os.write(head.getBytes());
        os.write(fileContent);
        os.write(tail.getBytes());
        return os.toByteArray();
    }

    private void fetchFiles(ListView<FileItem> listView, Label statusLabel) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/files"))
                .GET().build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    Platform.runLater(() -> {
                        if (res.statusCode() == 200) {
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<FileItem>>(){}.getType();
                            java.util.List<FileItem> files = gson.fromJson(res.body(), listType);
                            files.removeIf(f -> "DELETED".equals(f.status));
                            listView.setItems(javafx.collections.FXCollections.observableArrayList(files));
                        }
                    });
                });
    }

    private void processFileOnBackend(FileItem item, Label statusLabel, ListView<FileItem> listView) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/files/" + item.id + "/process"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setContentText(res.body());
                        alert.showAndWait();
                        fetchFiles(listView, statusLabel);
                    });
                });
    }

    private void deleteFileOnBackend(FileItem item, Label statusLabel, ListView<FileItem> listView) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/files/" + item.id + "/delete"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    Platform.runLater(() -> {
                        if (res.statusCode() == 200) {
                            statusLabel.setText("Usunięto.");
                            fetchFiles(listView, statusLabel);
                        }
                    });
                });
    }

    private void loadFilePreview(FileItem item, TextArea previewArea) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/files/" + item.id + "/preview"))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    Platform.runLater(() -> {
                        if (res.statusCode() == 200) {
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            String[] lines = gson.fromJson(res.body(), String[].class);
                            previewArea.setText(String.join("\n", lines));
                        }
                    });
                });
    }

    public static void main(String[] args) {
        launch(args);
    }

    //Klasa FileItem, nwm czemu jej nie bylo???
    public static class FileItem {
        public Long id;
        public String fileName;
        public String status;
        public String uploadTime;

        // Nadpisujemy metodę toString, aby ListView ładnie wyświetlało nazwy, a nie "krzaczki" w pamięci
        @Override
        public String toString() {
            return fileName + " [" + status + "]";
        }
    }
}

