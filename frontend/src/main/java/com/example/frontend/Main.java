package com.example.frontend;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
    Scene loginScene, registerScene, dashboardScene;
    ListView<FileItem> fileListView;
    Label fileStatusLabel;

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());
        window = stage;
        window.setTitle("Medicalytics");
        window.setMaximized(true);

        createLoginScene();
        createRegisterScene();
        createDashboardScene();

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
            HttpClient client = HttpClient.newHttpClient();
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
                                window.setScene(dashboardScene);
                                window.getHeight();
                                window.setMaximized(false);
                                window.setMaximized(true);
                            } else {
                                errorLabel.setText(responseBody);
                            }
                        });
                    });
        });

        Button registerButton = new Button("Zarejestruj się");
        registerButton.setMaxWidth(250);
        registerButton.getStyleClass().add(Styles.BUTTON_OUTLINED);
        registerButton.setOnAction(e -> {
            window.setScene(registerScene);
            window.getHeight();
            window.setMaximized(false);
            window.setMaximized(true);
        });

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
        statusLabel.getStyleClass().add(Styles.DANGER);

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
                            if (response.statusCode() == 200 || responseBody.toLowerCase().contains("sukces")) {
                                statusLabel.getStyleClass().setAll("label", Styles.SUCCESS);
                            } else {
                                statusLabel.getStyleClass().setAll("label", Styles.DANGER);
                            }
                        });
                    });
        });

        Button backButton = new Button("Powrót do logowania");
        backButton.setMaxWidth(250);
        backButton.getStyleClass().add(Styles.BUTTON_OUTLINED);
        backButton.setOnAction(e -> {
            window.setScene(loginScene);
            window.getHeight();
            window.setMaximized(false);
            window.setMaximized(true);
        });

        layout.getChildren().addAll(titleLabel, usernameInput, passwordInput, confirmPasswordInput, registerButton, backButton, statusLabel);
        registerScene = new Scene(layout, 400, 550);
    }

    private void createDashboardScene() {
        VBox layout = new VBox(25);
        layout.setPadding(new Insets(50));
        layout.setAlignment(Pos.CENTER);
        layout.setFillWidth(true);

        Label welcomeLabel = new Label("Medicalytics - Panel");
        welcomeLabel.getStyleClass().add(Styles.TITLE_2);
        fileStatusLabel = new Label("Wybierz plik z listy");

        fileListView = new ListView<>();
        fileListView.setMinHeight(400);
        fileListView.setMaxWidth(900);
        VBox.setVgrow(fileListView, javafx.scene.layout.Priority.ALWAYS);

        Button refreshButton = new Button("Odśwież listę");
        Button uploadButton = new Button("Wgraj nowy plik");
        Button processButton = new Button("Przetwórz wybrany plik");
        processButton.getStyleClass().add(Styles.ACCENT);
        processButton.setDisable(true);

        Button deleteButton = new Button("Usuń plik");
        deleteButton.getStyleClass().add(Styles.DANGER);
        deleteButton.setDisable(true);

        Button logoutButton = new Button("Wyloguj");
        logoutButton.getStyleClass().add(Styles.FLAT);

        double btnWidth = 300;
        refreshButton.setMinWidth(btnWidth);
        uploadButton.setMinWidth(btnWidth);
        processButton.setMinWidth(btnWidth);
        deleteButton.setMinWidth(btnWidth);
        logoutButton.setMinWidth(btnWidth);

        fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isSelected = (newVal != null);
            deleteButton.setDisable(!isSelected);
            processButton.setDisable(!isSelected || !"UPLOADED".equals(newVal.status));
        });

        refreshButton.setOnAction(e -> fetchFiles(fileListView, fileStatusLabel));

        uploadButton.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
            File file = fc.showOpenDialog(window);
            if (file != null) {
                String fileName = file.getName();
                boolean existsOnList = fileListView.getItems().stream()
                        .anyMatch(item -> {
                            if (item.fileName == null) return false;
                            String cleanName = fileName.replace(".csv", "");
                            return item.fileName.toLowerCase().startsWith(cleanName.toLowerCase());
                        });
                if (existsOnList) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.initOwner(window);
                    alert.setTitle("Plik już istnieje");
                    alert.setHeaderText("Plik o nazwie '" + fileName + "' jest już na liście.");
                    alert.setContentText("Czy na pewno chcesz go dodać?");

                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        sendCsvToBackend(file, fileStatusLabel);
                    }
                } else {
                    sendCsvToBackend(file, fileStatusLabel);
                }
            }
        });

        processButton.setOnAction(e -> {
            FileItem selected = fileListView.getSelectionModel().getSelectedItem();
            if (selected != null) processFileOnBackend(selected, fileStatusLabel, fileListView);
        });

        deleteButton.setOnAction(e -> {
            FileItem selected = fileListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.initOwner(window);
                alert.setTitle("Usuwanie");
                alert.setHeaderText("Plik zostanie usunięty");
                alert.setContentText("Czy na pewno chcesz go usunąć?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    deleteFileOnBackend(selected, fileStatusLabel, fileListView);
                }
            }
        });

        logoutButton.setOnAction(e -> {
            window.setScene(loginScene);
            window.getHeight();
            window.setMaximized(false);
            window.setMaximized(true);
        });

        layout.getChildren().addAll(welcomeLabel, refreshButton, fileListView, uploadButton, processButton, deleteButton, fileStatusLabel, logoutButton);
        dashboardScene = new Scene(layout);
    }

    private void sendCsvToBackend(File file, Label statusLabel) {
        try {
            String boundary = "---" + System.currentTimeMillis();
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
                                statusLabel.setText("Sukces: Wgrano plik");
                                fetchFiles(fileListView, statusLabel);
                            } else {
                                statusLabel.setText("Błąd (" + response.statusCode() + "): " + response.body());
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

    // POBIERANIE Z BACKENDU
    private void fetchFiles(ListView<FileItem> listView, Label statusLabel) {
        HttpClient client = HttpClient.newHttpClient();
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
                            statusLabel.setText("Lista odświeżona.");
                        }
                    });
                });
    }

    // PRZETWARZANIE
    private void processFileOnBackend(FileItem item, Label statusLabel, ListView<FileItem> listView) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/files/" + item.id + "/process"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    Platform.runLater(() -> {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.setTitle("Wynik przetwarzania");
                        alert.setHeaderText(null);
                        alert.setContentText(res.body());
                        alert.showAndWait();
                        fetchFiles(listView, statusLabel);
                    });
                });
    }

    // USUWANIE
    private void deleteFileOnBackend(FileItem item, Label statusLabel, ListView<FileItem> listView) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/files/" + item.id + "/delete"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    Platform.runLater(() -> {
                        if (res.statusCode() == 200) {
                            statusLabel.setText("Plik został usunięty.");
                            fetchFiles(listView, statusLabel);
                        } else {
                            statusLabel.setText("Błąd: " + res.body());
                        }
                    });
                }).exceptionally(ex -> {
                    Platform.runLater(() -> statusLabel.setText("Błąd połączenia: " + ex.getMessage()));
                    return null;
                });
    }

    public static void main(String[] args) {
        launch(args);
    }
}

class FileItem {
    Long id;
    String fileName;
    String status;

    @Override
    public String toString() {
        return fileName + " [" + status + "]";
    }
}