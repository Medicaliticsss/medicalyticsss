package com.example.frontend.views;

import com.example.frontend.models.TestTypeEntry;
import com.example.frontend.services.SettingsService;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.Optional;

public class DictionarySettingsView {

    private static ObservableList<TestTypeEntry> dictionaryEntries = FXCollections.observableArrayList();
    private static FilteredList<TestTypeEntry> filteredEntries;
    private static TableView<TestTypeEntry> dictionaryTable;
    private static Label statusLabel;

    public static Parent getView() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10, 0, 0, 0));

        Label description = new Label("Słownik badań (MDM) — źródło norm używanych przy analizie anomalii.");
        description.getStyleClass().add(Styles.TEXT_MUTED);
        description.setWrapText(true);

        TextField searchField = new TextField();
        searchField.setPromptText("Szukaj po kodzie, nazwie lub kategorii...");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button refreshButton = new Button("Odśwież");
        Button editButton = new Button("Edytuj wpis");
        Button exportButton = new Button("Eksportuj JSON");
        Button importButton = new Button("Importuj JSON");
        editButton.getStyleClass().add(Styles.ACCENT);

        HBox toolbar = new HBox(10, searchField, refreshButton, editButton, exportButton, importButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        dictionaryTable = createDictionaryTable();
        filteredEntries = new FilteredList<>(dictionaryEntries, entry -> true);
        dictionaryTable.setItems(filteredEntries);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase();
            filteredEntries.setPredicate(entry -> {
                if (query.isEmpty()) {
                    return true;
                }
                return containsIgnoreCase(entry.getTestCode(), query)
                        || containsIgnoreCase(entry.getTestName(), query)
                        || containsIgnoreCase(entry.getCategoryName(), query);
            });
        });

        statusLabel = new Label();
        statusLabel.setWrapText(true);

        refreshButton.setOnAction(e -> loadDictionary(true));
        editButton.setOnAction(e -> editSelectedEntry());
        exportButton.setOnAction(e -> exportDictionary());
        importButton.setOnAction(e -> importDictionary());

        VBox top = new VBox(12, description, toolbar, statusLabel);
        root.setTop(top);
        root.setCenter(dictionaryTable);

        loadDictionary(false);
        return root;
    }

    private static TableView<TestTypeEntry> createDictionaryTable() {
        TableView<TestTypeEntry> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Brak wpisów słownika."));

        TableColumn<TestTypeEntry, String> codeCol = new TableColumn<>("Kod");
        codeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTestCode()));

        TableColumn<TestTypeEntry, String> nameCol = new TableColumn<>("Nazwa");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTestName()));

        TableColumn<TestTypeEntry, String> categoryCol = new TableColumn<>("Kategoria");
        categoryCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCategoryName() == null ? "" : data.getValue().getCategoryName()));

        TableColumn<TestTypeEntry, String> unitCol = new TableColumn<>("Jednostka");
        unitCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getUnit() == null ? "" : data.getValue().getUnit()));

        TableColumn<TestTypeEntry, String> minCol = new TableColumn<>("Norma min");
        minCol.setCellValueFactory(data -> new SimpleStringProperty(formatDecimal(data.getValue().getNormMin())));

        TableColumn<TestTypeEntry, String> maxCol = new TableColumn<>("Norma max");
        maxCol.setCellValueFactory(data -> new SimpleStringProperty(formatDecimal(data.getValue().getNormMax())));

        table.getColumns().addAll(codeCol, nameCol, categoryCol, unitCol, minCol, maxCol);
        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    private static void loadDictionary(boolean userInitiated) {
        if (userInitiated) {
            setStatus("Ładowanie słownika...", null);
        }

        SettingsService.fetchDictionary().thenAccept(entries -> Platform.runLater(() -> {
            dictionaryEntries.setAll(entries);
            if (userInitiated || !statusLabel.getText().toLowerCase().contains("pomyślnie")) {
                setStatus("Załadowano wpisów: " + entries.size(), true);
            }
        }));
    }

    private static void editSelectedEntry() {
        TestTypeEntry selected = dictionaryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Wybierz wpis słownika do edycji.", false);
            return;
        }

        Dialog<TestTypeEntry> dialog = new Dialog<>();
        dialog.setTitle("Edytuj badanie");
        dialog.setHeaderText("Kod: " + selected.getTestCode());

        ButtonType saveButtonType = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField nameField = new TextField(selected.getTestName());
        TextField categoryField = new TextField(
                selected.getCategoryName() == null ? "" : selected.getCategoryName());
        TextField unitField = new TextField(selected.getUnit() == null ? "" : selected.getUnit());
        TextField normMinField = new TextField(formatDecimal(selected.getNormMin()));
        TextField normMaxField = new TextField(formatDecimal(selected.getNormMax()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.add(new Label("Nazwa:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Kategoria:"), 0, 1);
        grid.add(categoryField, 1, 1);
        grid.add(new Label("Jednostka:"), 0, 2);
        grid.add(unitField, 1, 2);
        grid.add(new Label("Norma min:"), 0, 3);
        grid.add(normMinField, 1, 3);
        grid.add(new Label("Norma max:"), 0, 4);
        grid.add(normMaxField, 1, 4);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button != saveButtonType) {
                return null;
            }

            TestTypeEntry updated = new TestTypeEntry();
            updated.setTestCode(selected.getTestCode());
            updated.setTestName(nameField.getText().trim());
            updated.setCategoryName(categoryField.getText().trim());
            updated.setUnit(unitField.getText().trim());
            updated.setNormMin(parseDecimal(normMinField.getText()));
            updated.setNormMax(parseDecimal(normMaxField.getText()));
            return updated;
        });

        Optional<TestTypeEntry> result = dialog.showAndWait();
        result.ifPresent(entry -> {
            if (entry.getTestName().isBlank()) {
                setStatus("Nazwa badania jest wymagana.", false);
                return;
            }
            if (entry.getNormMin() != null && entry.getNormMax() != null
                    && entry.getNormMin().compareTo(entry.getNormMax()) > 0) {
                setStatus("Norma min nie może być większa od normy max.", false);
                return;
            }

            setStatus("Zapisywanie wpisu...", null);
            SettingsService.updateDictionaryEntry(entry).thenAccept(message -> Platform.runLater(() -> {
                if (message.toLowerCase().contains("zaktualizowany")) {
                    setStatus(message, true);
                    loadDictionary(false);
                } else {
                    setStatus(message, false);
                }
            }));
        });
    }

    private static void exportDictionary() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Eksportuj słownik badań");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        chooser.setInitialFileName("test-types.json");

        java.io.File file = chooser.showSaveDialog(dictionaryTable.getScene().getWindow());
        if (file == null) {
            return;
        }

        setStatus("Eksportowanie słownika...", null);
        SettingsService.exportDictionaryJson().thenAccept(json -> Platform.runLater(() -> {
            if (!json.trim().startsWith("[")) {
                setStatus(json, false);
                return;
            }

            try {
                Files.writeString(file.toPath(), json);
                setStatus("Słownik wyeksportowany do: " + file.getAbsolutePath(), true);
            } catch (IOException ex) {
                setStatus("Nie udało się zapisać pliku: " + ex.getMessage(), false);
            }
        }));
    }

    private static void importDictionary() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importuj słownik badań");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));

        java.io.File file = chooser.showOpenDialog(dictionaryTable.getScene().getWindow());
        if (file == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Import słownika");
        confirm.setHeaderText("Zaimportować słownik z pliku?");
        confirm.setContentText("Istniejące wpisy zostaną zaktualizowane, a brakujące dodane.\nPlik: " + file.getName());

        Optional<ButtonType> decision = confirm.showAndWait();
        if (decision.isEmpty() || decision.get() != ButtonType.OK) {
            return;
        }

        try {
            String json = Files.readString(file.toPath());
            setStatus("Importowanie słownika...", null);
            SettingsService.importDictionaryJson(json).thenAccept(message -> Platform.runLater(() -> {
                if (message.toLowerCase().contains("pomyślnie") || message.toLowerCase().contains("zsynchronizowano")) {
                    setStatus(message, true);
                    loadDictionary(false);
                } else {
                    setStatus(message, false);
                }
            }));
        } catch (IOException ex) {
            setStatus("Nie udało się odczytać pliku: " + ex.getMessage(), false);
        }
    }

    private static boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private static String formatDecimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value.trim().replace(",", "."));
    }

    private static void setStatus(String message, Boolean success) {
        statusLabel.setText(message);
        if (success == null) {
            statusLabel.setStyle("");
        } else {
            statusLabel.setStyle(success ? "-fx-text-fill: #3DDC84;" : "-fx-text-fill: #FF6B6B;");
        }
    }
}
