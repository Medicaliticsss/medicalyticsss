package com.example.frontend.utils;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CsvExportUtil {

    private CsvExportUtil() {
    }

    public static <T> Path exportTable(
            TableView<T> table,
            String dialogTitle,
            String suggestedFileName
    ) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(dialogTitle);
        fileChooser.setInitialFileName(withExtension(suggestedFileName, ".csv"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));

        java.io.File selectedFile = fileChooser.showSaveDialog(ViewManager.getPrimaryStage());
        if (selectedFile == null) {
            return null;
        }

        Path targetPath = selectedFile.toPath();
        if (!targetPath.getFileName().toString().toLowerCase().endsWith(".csv")) {
            targetPath = targetPath.resolveSibling(targetPath.getFileName() + ".csv");
        }
        List<TableColumn<T, ?>> columns = getLeafColumns(table.getColumns());

        try (BufferedWriter writer = Files.newBufferedWriter(targetPath, StandardCharsets.UTF_8)) {
            writeRow(writer, columns.stream().map(TableColumn::getText).toList());
            for (T item : table.getItems()) {
                List<String> row = new ArrayList<>();
                for (TableColumn<T, ?> column : columns) {
                    Object value = column.getCellData(item);
                    row.add(value == null ? "" : value.toString());
                }
                writeRow(writer, row);
            }
        }

        return targetPath;
    }

    static void writeRow(BufferedWriter writer, List<String> values) throws IOException {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                writer.write(',');
            }
            writer.write(escape(values.get(index)));
        }
        writer.newLine();
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }

        String normalizedValue = value.replace("\r\n", "\n").replace('\r', '\n');
        boolean requiresQuotes = normalizedValue.contains(",")
                || normalizedValue.contains("\"")
                || normalizedValue.contains("\n");

        if (!requiresQuotes) {
            return normalizedValue;
        }

        return "\"" + normalizedValue.replace("\"", "\"\"") + "\"";
    }

    private static <T> List<TableColumn<T, ?>> getLeafColumns(List<TableColumn<T, ?>> columns) {
        List<TableColumn<T, ?>> leafColumns = new ArrayList<>();
        for (TableColumn<T, ?> column : columns) {
            if (column.getColumns().isEmpty()) {
                leafColumns.add(column);
                continue;
            }

            leafColumns.addAll(getLeafColumns(column.getColumns()));
        }
        return leafColumns;
    }

    private static String withExtension(String fileName, String extension) {
        if (fileName == null || fileName.isBlank()) {
            return "export" + extension;
        }
        return fileName.toLowerCase().endsWith(extension) ? fileName : fileName + extension;
    }
}
