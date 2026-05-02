package com.example.frontend;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
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
import atlantafx.base.theme.Dracula;
import atlantafx.base.theme.Styles;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

public class Main extends Application {
    Stage window;

    // GŁÓWNA SCENA - trzyma rozmiar okna w ryzach
    Scene mainScene;

    // WIDOKI (Zamiast Scen)
    Parent loginView, registerView, dashboardView, mainMenuView, settingsView, reportView;

    // Pola klasowe
    ListView<FileItem> fileListView;
    Label fileStatusLabel, totalTestsLabel, normalResultsLabel, anomaliesLabel;
    javafx.scene.chart.PieChart reportChart;

    // Jeden wspólny klient dla całej aplikacji (obsługuje sesje/ciasteczka)
    private final HttpClient client = HttpClient.newBuilder()
            .cookieHandler(new java.net.CookieManager())
            .build();

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());
        window = stage;
        window.setTitle("Medicalytics");

        // Inicjalizacja pustej głównej sceny z domyślnym rozmiarem (dla trybu okienkowego)
        mainScene = new Scene(new VBox(), 1200, 800);
        window.setScene(mainScene);
        window.setMaximized(true); // Wymuszamy maksymalizację na starcie

        // Inicjalizacja wszystkich widoków (nie tworzą już nowych obiektów Scene!)
        createLoginScene();
        createRegisterScene();
        createDashboardScene();
        createMainMenuScene();
        createReportScene();
        createSettingsScene();

        // Uruchomienie weryfikacji sesji (ona zadecyduje, co wrzucić do mainScene)
        checkSessionAndStart();
    }

    // Nowa metoda pomocnicza do płynnego przełączania widoków
    private void switchView(Parent view) {
        mainScene.setRoot(view);
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
                                UserSession.getInstance().login(usernameInput.getText());
                                fetchFiles(fileListView, fileStatusLabel);
                                switchView(mainMenuView);
                            } else {
                                errorLabel.setText(responseBody);
                            }
                        });
                    });
        });

        Button registerButton = new Button("Zarejestruj się");
        registerButton.setMaxWidth(250);
        registerButton.getStyleClass().add(Styles.BUTTON_OUTLINED);
        registerButton.setOnAction(e -> switchView(registerView));

        layout.getChildren().addAll(titleLabel, usernameInput, passwordInput, loginButton, registerButton, errorLabel);
        loginView = layout;
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
        backButton.setOnAction(e -> switchView(loginView));

        layout.getChildren().addAll(titleLabel, usernameInput, passwordInput, confirmPasswordInput, registerButton, backButton, statusLabel);
        registerView = layout;
    }

    private void createDashboardScene() {
        javafx.scene.layout.HBox mainLayout = new javafx.scene.layout.HBox(40); // Większy odstęp między lewą a prawą stroną
        mainLayout.setPadding(new Insets(40)); // Marginesy od krawędzi ekranu
        mainLayout.setAlignment(Pos.CENTER);

        //(PODGLĄD)
        VBox previewPanel = new VBox(10);
        Label previewLabel = new Label("Podgląd zawartości pliku:");
        previewLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;"); // Trochę większy tekst

        TextArea previewArea = new TextArea();
        previewArea.setEditable(false);
        // Pozwala polu tekstowemu rosnąć w pionie w nieskończoność
        javafx.scene.layout.VBox.setVgrow(previewArea, javafx.scene.layout.Priority.ALWAYS);

        previewPanel.getChildren().addAll(previewLabel, previewArea);

        //Pozwala całemu lewemu panelowi zająć całą dostępną szerokość okna
        javafx.scene.layout.HBox.setHgrow(previewPanel, javafx.scene.layout.Priority.ALWAYS);

        //PRAWY PANEL (STEROWANIE)
        VBox controlPanel = new VBox(15); // Zwiększony odstęp między przyciskami
        controlPanel.setMinWidth(350); // Szerszy panel boczny
        controlPanel.setAlignment(Pos.TOP_CENTER);

        Label welcomeLabel = new Label("Medicalytics - Pliki");
        welcomeLabel.getStyleClass().add(Styles.TITLE_2); // Większy tytuł dla panelu bocznego

        fileListView = new ListView<>();
        // Pozwala liście plików rosnąć w pionie
        javafx.scene.layout.VBox.setVgrow(fileListView, javafx.scene.layout.Priority.ALWAYS);

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
        backButton.setOnAction(e -> switchView(mainMenuView));

        controlPanel.getChildren().addAll(welcomeLabel, refreshButton, fileListView,
                uploadButton, processButton, previewButton, deleteButton,
                fileStatusLabel, backButton);

        mainLayout.getChildren().addAll(previewPanel, controlPanel);
        dashboardView = mainLayout;
    }

    private void createMainMenuScene() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(40));

        Label titleLabel = new Label("Główne Menu");
        titleLabel.getStyleClass().add(Styles.TITLE_1);

        Button logoutButton = new Button("Wyloguj");
        logoutButton.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER);
        logoutButton.setOnAction(e -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/auth/logout"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                            UserSession.getInstance().logout();
                            ((java.net.CookieManager) client.cookieHandler().get()).getCookieStore().removeAll();
                            switchView(loginView);
                        });
                    });
        });

        javafx.scene.layout.StackPane topContainer = new javafx.scene.layout.StackPane();
        topContainer.getChildren().addAll(titleLabel, logoutButton);
        javafx.scene.layout.StackPane.setAlignment(logoutButton, Pos.CENTER_RIGHT);
        root.setTop(topContainer);

        javafx.scene.layout.HBox cardsContainer = new javafx.scene.layout.HBox(20);
        cardsContainer.setAlignment(Pos.CENTER);

        Button filesButton = createMenuCard("Pliki");
        Button reportsButton = createMenuCard("Raporty");
        Button settingsButton = createMenuCard("Ustawienia");

        filesButton.setOnAction(e -> switchView(dashboardView));
        reportsButton.setOnAction(e -> {
            fetchReportSummary();
            switchView(reportView);
        });
        settingsButton.setOnAction(e -> switchView(settingsView));

        cardsContainer.getChildren().addAll(filesButton, reportsButton, settingsButton);
        root.setCenter(cardsContainer);

        mainMenuView = root;
    }

    private void createReportScene() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(30));

        // NAGŁÓWEK
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label("Raport Globalny");
        titleLabel.getStyleClass().add(Styles.TITLE_1);

        Button backButton = new Button("Powrót do Menu");
        backButton.getStyleClass().add(Styles.BUTTON_OUTLINED);
        backButton.setOnAction(e -> switchView(mainMenuView));

        header.getChildren().addAll(backButton, titleLabel);
        root.setTop(header);

        // KAFELKI KPI
        javafx.scene.layout.HBox kpiContainer = new javafx.scene.layout.HBox(20);
        kpiContainer.setAlignment(Pos.CENTER);
        kpiContainer.setPadding(new Insets(20));

        totalTestsLabel = createKPICard("Wszystkie badania", "0", Styles.TEXT_BOLD);
        normalResultsLabel = createKPICard("W normie", "0", Styles.SUCCESS);
        anomaliesLabel = createKPICard("Anomalie", "0", Styles.DANGER);

        kpiContainer.getChildren().addAll(totalTestsLabel.getParent(), normalResultsLabel.getParent(), anomaliesLabel.getParent());

        // WYKRES
        reportChart = new javafx.scene.chart.PieChart();
        reportChart.setTitle("Proporcja wyników");
        reportChart.setLabelsVisible(true);
        reportChart.setLegendSide(javafx.geometry.Side.BOTTOM);

        VBox centerLayout = new VBox(30, kpiContainer, reportChart);
        centerLayout.setAlignment(Pos.CENTER);
        root.setCenter(centerLayout);

        reportView = root;
    }

    // Pomocnicza metoda do tworzenia ładnych kart z liczbami
    private Label createKPICard(String title, String value, String styleClass) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #2D2D30; -fx-background-radius: 10; -fx-min-width: 200;");

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add(Styles.TEXT_MUTED);

        Label valueLbl = new Label(value);
        valueLbl.getStyleClass().add(Styles.TITLE_2);
        valueLbl.getStyleClass().add(styleClass);

        box.getChildren().addAll(titleLbl, valueLbl);
        return valueLbl;
    }

    private void createSettingsScene() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        Label label = new Label("TU BĘDĄ USTAWIENIA");
        Button backButton = new Button("Wróć do Menu");
        backButton.setOnAction(e -> switchView(mainMenuView));
        root.getChildren().addAll(label, backButton);
        settingsView = root;
    }

    private void checkSessionAndStart() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/auth/me"))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            String username = response.body();
                            UserSession.getInstance().login(username);
                            fetchFiles(fileListView, fileStatusLabel);
                            switchView(mainMenuView);
                        } else {
                            switchView(loginView);
                        }
                        window.show();
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        switchView(loginView);
                        window.show();
                    });
                    return null;
                });
    }

    // Powiększone kafelki w menu głównym
    private Button createMenuCard(String text) {
        Button card = new Button(text);
        card.setPrefSize(300, 300); // Zmiana na 300x300
        card.getStyleClass().addAll(Styles.ELEVATED_2, Styles.TITLE_2); // Zmiana na większą czcionkę (TITLE_2)
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

    private void fetchReportSummary() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/reports/summary"))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    Platform.runLater(() -> {
                        if (res.statusCode() == 200) {
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            ReportSummary summary = gson.fromJson(res.body(), ReportSummary.class);

                            // Aktualizacja liczb
                            totalTestsLabel.setText(String.valueOf(summary.totalTests));
                            normalResultsLabel.setText(String.valueOf(summary.normalResults));
                            anomaliesLabel.setText(String.valueOf(summary.abnormalResults));

                            // Aktualizacja wykresu
                            reportChart.getData().clear();
                            if (summary.totalTests > 0) {
                                reportChart.getData().add(new javafx.scene.chart.PieChart.Data("W normie", summary.normalResults));
                                reportChart.getData().add(new javafx.scene.chart.PieChart.Data("Anomalie", summary.abnormalResults));
                            } else {
                                totalTestsLabel.setText("Brak danych");
                            }
                        }
                    });
                });
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static class FileItem {
        public Long id;
        public String fileName;
        public String status;
        public String uploadTime;

        @Override
        public String toString() {
            return fileName + " [" + status + "]";
        }
    }

    class ReportSummary {
        int totalTests;
        int normalResults;
        int abnormalResults;
    }
}