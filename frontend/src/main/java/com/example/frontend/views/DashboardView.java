package com.example.frontend.views;

import com.example.frontend.models.FileItem;
import com.example.frontend.models.UserSession;
import com.example.frontend.services.FileService;
import com.example.frontend.utils.ViewManager;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.io.File;

public class DashboardView {
    public static Parent getView() {
        HBox mainLayout = new HBox(40);
        mainLayout.setPadding(new Insets(40));
        mainLayout.setAlignment(Pos.CENTER);

        // --- PANEL LEWY (PODGLĄD) ---
        VBox previewPanel = new VBox(10);
        Label previewLabel = new Label("Podgląd zawartości pliku:");
        previewLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        TextArea previewArea = new TextArea();
        previewArea.setEditable(false);
        VBox.setVgrow(previewArea, Priority.ALWAYS);

        // 1. Pole na błędy (usuwamy stąd tło, dajemy tylko czcionkę)
        Label errorDetailsLabel = new Label();
        errorDetailsLabel.setWrapText(true);
        errorDetailsLabel.setStyle("-fx-text-fill: #FF3333; -fx-font-family: 'Consolas';");

        // 2. NOWE: Opakowujemy Label w ScrollPane (suwak)
        ScrollPane errorScrollPane = new ScrollPane(errorDetailsLabel);
        errorScrollPane.setFitToWidth(true); // Ważne: dzięki temu tekst w Labelu nadal będzie się zawijał
        errorScrollPane.setMaxHeight(180);   // Blokujemy wysokość, żeby NIE spychał TextArea!
        errorScrollPane.setStyle("-fx-background: #2A1A1A; -fx-background-color: #2A1A1A; -fx-padding: 10; -fx-background-radius: 5;");

        // Ukrywamy cały suwak, a nie tylko Label
        errorScrollPane.setVisible(false);
        errorScrollPane.setManaged(false);

        // Dodajemy errorScrollPane zamiast samego Labela
        previewPanel.getChildren().addAll(previewLabel, previewArea, errorScrollPane);
        HBox.setHgrow(previewPanel, Priority.ALWAYS);

        // --- PANEL PRAWY (STEROWANIE) ---
        VBox controlPanel = new VBox(15);
        controlPanel.setMinWidth(350);
        controlPanel.setAlignment(Pos.TOP_CENTER);

        Label welcomeLabel = new Label("Medicalytics - Pliki");
        welcomeLabel.getStyleClass().add(Styles.TITLE_2);

        ListView<FileItem> fileListView = new ListView<>();
        VBox.setVgrow(fileListView, Priority.ALWAYS);

        Label fileStatusLabel = new Label("Wybierz plik z listy");

        // Przyciski
        Button refreshBtn = new Button("Odśwież listę");
        Button uploadBtn = new Button("Wgraj nowy plik");
        Button processBtn = new Button("Przetwórz plik");
        Button deleteBtn = new Button("Usuń plik");
        Button backBtn = new Button("Wróć do menu");
        Button previewBtn = new Button("Podgląd");


        // Stylizacja
        deleteBtn.getStyleClass().add(Styles.DANGER);
        processBtn.getStyleClass().add(Styles.ACCENT);
        backBtn.getStyleClass().add(Styles.BUTTON_OUTLINED);

        refreshBtn.setMaxWidth(Double.MAX_VALUE);
        uploadBtn.setMaxWidth(Double.MAX_VALUE);
        processBtn.setMaxWidth(Double.MAX_VALUE);
        previewBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        backBtn.setMaxWidth(Double.MAX_VALUE);

        //nieaktywne przyciski
        fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean isSelected = (newV != null);
            previewBtn.setDisable(!isSelected);
            deleteBtn.setDisable(!isSelected);
            processBtn.setDisable(!isSelected || !"UPLOADED".equals(newV.status));
        });

        // Akcje (Korzystamy z FileService)
        uploadBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));

            // Musimy pobrać Stage, żeby pokazać okno wyboru pliku
            java.io.File file = fc.showOpenDialog(ViewManager.getPrimaryStage());

            if (file != null) {
                // Tutaj wywołujemy FileService (musimy tam zaraz dodać metodę upload)
                FileService.uploadFile(file).thenAccept(res -> {
                    Platform.runLater(() -> {
                        fileStatusLabel.setText(res);
                        // Odśwież listę po wgraniu
                        FileService.fetchFiles().thenAccept(files ->
                                Platform.runLater(() -> fileListView.getItems().setAll(files))
                        );
                    });
                });
            }
        });
        refreshBtn.setOnAction(e -> FileService.fetchFiles().thenAccept(files -> Platform.runLater(() -> {
            previewArea.clear();
            fileListView.getItems().setAll(files);
            fileStatusLabel.setText("Zaktualizowano listę.");
        })));

        backBtn.setOnAction(e -> ViewManager.switchView(MainMenuView.getView()));

        // --- AKCJA: PRZETWÓRZ ---
        processBtn.setOnAction(e -> {
            FileItem selected = fileListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // Zapamiętujemy ID przetwarzanego pliku, zanim lista się odświeży
                Long processedFileId = selected.id;

                FileService.processFile(processedFileId).thenAccept(res -> Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setContentText(res);
                    alert.showAndWait();

                    // Odświeżamy listę ręcznie, żeby po zakończeniu wywołać podgląd
                    FileService.fetchFiles().thenAccept(files -> Platform.runLater(() -> {
                        // Wrzucamy nowe pliki na listę
                        fileListView.getItems().setAll(files);
                        fileStatusLabel.setText("Zaktualizowano listę po przetwarzaniu.");

                        // Szukamy naszego przetworzonego pliku na nowej liście
                        for (FileItem item : files) {
                            if (item.id.equals(processedFileId)) {
                                // Zaznaczamy go ponownie na liście (żeby podświetlił się na niebiesko)
                                fileListView.getSelectionModel().select(item);

                                // Automatycznie "klikamy" przycisk podglądu!
                                previewBtn.fire();
                                break;
                            }
                        }
                    }));
                }));
            }
        });

// --- AKCJA: USUŃ ---
        deleteBtn.setOnAction(e -> {
            FileItem selected = fileListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                FileService.deleteFile(selected.id).thenAccept(res -> Platform.runLater(() -> {
                    fileStatusLabel.setText(res);
                    previewArea.clear();
                    // NOWE: Ukrywamy i czyścimy panel z błędami po usunięciu pliku
                    errorDetailsLabel.setVisible(false);
                    errorDetailsLabel.setManaged(false);
                    errorDetailsLabel.setText("");
                    refreshBtn.fire();
                }));
            }
        });

// --- AKCJA: PODGLĄD ---
        previewBtn.setOnAction(e -> {
            FileItem selected = fileListView.getSelectionModel().getSelectedItem();
            if (selected != null) {

                // 1. Pobieranie normalnego podglądu tekstu
                FileService.getFilePreview(selected.id).thenAccept(lines -> Platform.runLater(() -> {
                    previewArea.setText(String.join("\n", lines));
                }));

// 2. NOWE: Pobieranie błędów, jeśli plik ma status ERROR lub PARTIAL_SUCCESS
                if ("ERROR".equals(selected.status) || "PARTIAL_SUCCESS".equals(selected.status)) {
                    FileService.getFileErrors(selected.id).thenAccept(errors -> {
                        Platform.runLater(() -> {
                            if (errors != null && !errors.isEmpty()) {
                                StringBuilder errorText = new StringBuilder();
                                errorText.append("⚠️ Znaleziono błędy:\n\n");

                                for (var error : errors) {
                                    errorText.append("🔴 ");
                                    if (error.errorRowNumber != null) {
                                        errorText.append("[Wiersz ").append(error.errorRowNumber).append("] ");
                                    }
                                    errorText.append(error.errorMessage != null ? error.errorMessage : "Nieznany błąd");
                                    if (error.rawLineData != null && !error.rawLineData.isBlank()) {
                                        errorText.append("\n    └─ Błędne dane: ").append(error.rawLineData);
                                    }
                                    errorText.append("\n");
                                }
                                errorDetailsLabel.setText(errorText.toString());
                            } else {
                                errorDetailsLabel.setText("⚠️ Plik ma status ERROR, ale w bazie nie ma szczegółów.");
                            }
                            // TUTAJ: Pokazujemy cały przewijany panel, a nie tylko napis
                            errorScrollPane.setVisible(true);
                            errorScrollPane.setManaged(true);
                        });
                    });
                } else {
                    // TUTAJ: Ukrywamy cały przewijany panel, jeśli plik jest czysty
                    errorScrollPane.setVisible(false);
                    errorScrollPane.setManaged(false);
                    errorDetailsLabel.setText("");
                }
            }
        });

        // Inicjalne ładowanie danych
        FileService.fetchFiles().thenAccept(files -> Platform.runLater(() -> fileListView.getItems().setAll(files)));

        controlPanel.getChildren().addAll(welcomeLabel, refreshBtn, fileListView, uploadBtn, processBtn, previewBtn, deleteBtn, fileStatusLabel, backBtn);
        mainLayout.getChildren().addAll(previewPanel, controlPanel);
        BorderPane root = new BorderPane();
        root.setCenter(mainLayout);
        root.setBottom(UserSession.getInstance().createFooter());

        return root;
    }
}
