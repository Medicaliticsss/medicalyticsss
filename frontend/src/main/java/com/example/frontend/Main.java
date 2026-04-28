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
        createMainMenuScene();
        createReportScene();
        createSettingsScene();

        window.setScene(loginScene);
        window.show();
    }
    Scene loginScene, registerScene, dashboardScene;
    private final HttpClient client = HttpClient.newBuilder()
            .cookieHandler(new java.net.CookieManager())
            .build();

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
            window.setMaximized(false);
            window.setMaximized(true);
        });

        layout.getChildren().addAll(titleLabel, usernameInput, passwordInput, confirmPasswordInput, registerButton, backButton, statusLabel);
        registerScene = new Scene(layout, 400, 550);
    }
    private void createMainMenuScene() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(40));
        Label titleLabel = new Label("Główne Menu");
        titleLabel.getStyleClass().add(Styles.TITLE_1);
        titleLabel.setStyle("-fx-text-fill: #FF0055; -fx-font-weight: bold;");
        DropShadow neonGlow = new DropShadow();
        neonGlow.setColor(Color.web("#FF0055"));
        titleLabel.setEffect(neonGlow);
        Button logoutButton = new Button("Wyloguj");
        logoutButton.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER);
        logoutButton.setStyle("-fx-cursor: hand;");
        logoutButton.setOnAction(e -> {
            window.setScene(loginScene);
            window.setMaximized(false);
            window.setMaximized(true);
        });
        javafx.scene.layout.StackPane topContainer = new javafx.scene.layout.StackPane();
        topContainer.setPadding(new Insets(10, 0, 40, 0));
        topContainer.getChildren().addAll(titleLabel, logoutButton);
        javafx.scene.layout.StackPane.setAlignment(logoutButton, Pos.CENTER_RIGHT);
        root.setTop(topContainer);
        javafx.scene.layout.HBox cardsContainer = new javafx.scene.layout.HBox(60);
        cardsContainer.setAlignment(Pos.CENTER);
        cardsContainer.setPadding(new Insets(0, 60, 0, 60));
        Button filesButton = createMenuCard("Pliki");
        Button reportsButton = createMenuCard("Raporty");
        Button settingsButton = createMenuCard("Ustawienia");
        //szerokość i wysokość- 1/3 ekaranu
        filesButton.prefHeightProperty().bind(root.heightProperty().divide(3));
        filesButton.prefWidthProperty().bind(root.heightProperty().divide(3));
        reportsButton.prefHeightProperty().bind(root.heightProperty().divide(3));
        reportsButton.prefWidthProperty().bind(root.heightProperty().divide(3));
        settingsButton.prefHeightProperty().bind(root.heightProperty().divide(3));
        settingsButton.prefWidthProperty().bind(root.heightProperty().divide(3));
        filesButton.setOnAction(e -> {
            window.setScene(dashboardScene);
            window.getHeight();
            window.setMaximized(false);
            window.setMaximized(true);
        });
        reportsButton.setOnAction(e -> {
            window.setScene(reportScene);
            window.getHeight();
            window.setMaximized(false);
            window.setMaximized(true);
        });
        settingsButton.setOnAction(e -> {
            window.setScene(settingsScene);
            window.getHeight();
            window.setMaximized(false);
            window.setMaximized(true);
        });

        cardsContainer.getChildren().addAll(filesButton, reportsButton, settingsButton);
        root.setCenter(cardsContainer);

        javafx.scene.layout.HBox bottomBar = new javafx.scene.layout.HBox();
        bottomBar.setMinHeight(30);
        bottomBar.setStyle("-fx-background-color: #444444; -fx-background-radius: 10;");

        VBox bottomContainer = new VBox(bottomBar);
        bottomContainer.setPadding(new Insets(50, 0, 10, 0)); // Odstęp od kafelków
        root.setBottom(bottomContainer);

        mainMenuScene = new Scene(root, 1000, 700);
    }
    private void createReportScene() {
        VBox root = new VBox(40);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        Label label = new Label("TU BĘDĄ RAPORTY");
        label.getStyleClass().add(Styles.TITLE_1);
        label.setStyle("-fx-text-fill: #FF0055; -fx-font-weight: bold; -fx-font-size: 50px;");
        DropShadow neon = new DropShadow();
        neon.setColor(Color.web("#FF0055"));
        label.setEffect(neon);
        Button backButton = new Button("Wróć do Menu");
        backButton.getStyleClass().add(Styles.BUTTON_OUTLINED);
        backButton.setOnAction(e -> {
            window.setScene(mainMenuScene);
            window.getHeight();
            window.setMaximized(false);
            window.setMaximized(true);
        });

        root.getChildren().addAll(label, backButton);
        reportScene = new Scene(root, 1280, 720);
    }

    private void createSettingsScene() {
        VBox root = new VBox(40);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        Label label = new Label("TU BĘDĄ USTAWIENIA");
        label.getStyleClass().add(Styles.TITLE_1);
        label.setStyle("-fx-text-fill: #FF0055; -fx-font-weight: bold; -fx-font-size: 50px;");
        DropShadow neon = new DropShadow();
        neon.setColor(Color.web("#FF0055"));
        label.setEffect(neon);
        Button backButton = new Button("Wróć do Menu");
        backButton.getStyleClass().add(Styles.BUTTON_OUTLINED);
        backButton.setOnAction(e -> {
            window.setScene(mainMenuScene);
            window.getHeight();
            window.setMaximized(false);
            window.setMaximized(true);
        });

        root.getChildren().addAll(label, backButton);
        settingsScene = new Scene(root, 1280, 720);
    }
    // Metoda pomocnicza do generowania spójnych kafelków
    private Button createMenuCard(String text) {
        Button card = new Button(text);
        card.getStyleClass().addAll(Styles.ELEVATED_2, Styles.TITLE_3);
        card.setStyle(
                "-fx-background-color: #2D2D30; " +
                        "-fx-background-radius: 15; " +
                        "-fx-text-alignment: center; " +
                        "-fx-cursor: hand;"
        );

        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #3E3E42; -fx-background-radius: 15; -fx-text-alignment: center; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #2D2D30; -fx-background-radius: 15; -fx-text-alignment: center; -fx-cursor: hand;"));

        return card;
    }

    private void createDashboardScene() {
        VBox layout = new VBox(25);
        layout.setPadding(new Insets(50));
        layout.setAlignment(Pos.CENTER);
        layout.setFillWidth(true);
        javafx.scene.layout.HBox mainLayout = new javafx.scene.layout.HBox(20);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.CENTER); // Wyśrodkowanie paneli
        mainLayout.setStyle("-fx-background-color: #FFF3E0;");

        Label welcomeLabel = new Label("Medicalytics - Panel");
        welcomeLabel.getStyleClass().add(Styles.TITLE_2);
        fileStatusLabel = new Label("Wybierz plik z listy");
        // LEWY PANEL
        VBox previewPanel = new VBox(10);
        Label previewLabel = new Label("Podgląd zawartości pliku:");
        previewLabel.setStyle("-fx-font-weight: bold;");
        javafx.scene.control.TextArea previewArea = new javafx.scene.control.TextArea();
        previewArea.setEditable(false);
        previewArea.setPrefWidth(450); // Trochę szerzej dla wygody
        previewArea.setPrefHeight(500);
        previewPanel.getChildren().addAll(previewLabel, previewArea);

        fileListView = new ListView<>();
        fileListView.setMinHeight(400);
        fileListView.setMaxWidth(900);
        VBox.setVgrow(fileListView, javafx.scene.layout.Priority.ALWAYS);
        // PRAWY PANEL
        VBox controlPanel = new VBox(10);
        controlPanel.setMinWidth(250); // Stała szerokość panelu sterowania

        ListView<FileItem> fileListView = new ListView<>();
        fileListView.setPrefHeight(200);

        Button refreshButton = new Button("Odśwież listę");
        Button uploadButton = new Button("Wgraj nowy plik");
        Button processButton = new Button("Przetwórz wybrany plik");
        processButton.getStyleClass().add(Styles.ACCENT);
        processButton.setDisable(true);

        Button processButton = new Button("Przetwórz plik");
        Button previewButton = new Button("Podgląd");
        Button deleteButton = new Button("Usuń plik");
        deleteButton.getStyleClass().add(Styles.DANGER);
        deleteButton.setDisable(true);
        Button logoutButton = new Button("Wyloguj");
        Label fileStatusLabel = new Label("Wybierz plik");

        Button backButton = new Button("Wróć do menu");
        backButton.getStyleClass().add(Styles.BUTTON_OUTLINED);
        refreshButton.setMaxWidth(Double.MAX_VALUE);
        uploadButton.setMaxWidth(Double.MAX_VALUE);
        processButton.setMaxWidth(Double.MAX_VALUE);
        previewButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        logoutButton.setMaxWidth(Double.MAX_VALUE);

        double btnWidth = 300;
        refreshButton.setMinWidth(btnWidth);
        uploadButton.setMinWidth(btnWidth);
        processButton.setMinWidth(btnWidth);
        deleteButton.setMinWidth(btnWidth);
        backButton.setMinWidth(btnWidth);

        fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isSelected = (newVal != null);
            deleteButton.setDisable(!isSelected);
            processButton.setDisable(!isSelected || !"UPLOADED".equals(newVal.status));
        });

        // Akcje
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
            FileItem s = fileListView.getSelectionModel().getSelectedItem();
            if (s != null) deleteFileOnBackend(s, fileStatusLabel, fileListView);
        });

        backButton.setOnAction(e -> {
            window.setScene(mainMenuScene);
            window.setMaximized(false);
            window.setMaximized(true);
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

        layout.getChildren().addAll(welcomeLabel, refreshButton, fileListView, uploadButton, processButton, deleteButton, fileStatusLabel, backButton);
        dashboardScene = new Scene(layout);
        controlPanel.getChildren().addAll(new Label("Medicalytics - Panel"), refreshButton, fileListView,
                uploadButton, processButton, previewButton, deleteButton,
                fileStatusLabel, logoutButton);

        mainLayout.getChildren().addAll(previewPanel, controlPanel);
        dashboardScene = new Scene(mainLayout, 900, 600);
    }

    private void sendCsvToBackend(File file, Label statusLabel) {
        try {
            String boundary = "---" + System.currentTimeMillis();
            HttpClient client = HttpClient.newHttpClient();
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

        // Zmieniamy na POST i używamy ID w URL - dokładnie tak, jak zrobiłaś w Controllerze!
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

        // Adres URL zgodny z planem Twojego kolegi
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