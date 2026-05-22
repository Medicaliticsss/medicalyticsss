package com.example.frontend.views;

import com.example.frontend.models.FileItem;
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
        previewPanel.getChildren().addAll(previewLabel, previewArea);
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
                FileService.processFile(selected.id).thenAccept(res -> Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setContentText(res);
                    alert.showAndWait();
                    // Odśwież listę po przetworzeniu
                    refreshBtn.fire();
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
                    refreshBtn.fire();
                }));
            }
        });

// --- AKCJA: PODGLĄD ---
        previewBtn.setOnAction(e -> {
            FileItem selected = fileListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                FileService.getFilePreview(selected.id).thenAccept(lines -> Platform.runLater(() -> {
                    previewArea.setText(String.join("\n", lines));
                }));
            }
        });

        // Inicjalne ładowanie danych
        FileService.fetchFiles().thenAccept(files -> Platform.runLater(() -> fileListView.getItems().setAll(files)));

        controlPanel.getChildren().addAll(welcomeLabel, refreshBtn, fileListView, uploadBtn, processBtn, previewBtn, deleteBtn, fileStatusLabel, backBtn);
        mainLayout.getChildren().addAll(previewPanel, controlPanel);

        return mainLayout;
    }
}
