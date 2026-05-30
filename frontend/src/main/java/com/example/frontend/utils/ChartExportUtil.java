package com.example.frontend.utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.Chart;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public final class ChartExportUtil {

    private ChartExportUtil() {
    }

    public static Path exportChart(
            Chart chart,
            String dialogTitle,
            String suggestedFileName
    ) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(dialogTitle);
        fileChooser.setInitialFileName(withExtension(suggestedFileName, ".png"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));

        java.io.File selectedFile = fileChooser.showSaveDialog(ViewManager.getPrimaryStage());
        if (selectedFile == null) {
            return null;
        }

        Path targetPath = selectedFile.toPath();
        if (!targetPath.getFileName().toString().toLowerCase().endsWith(".png")) {
            targetPath = targetPath.resolveSibling(targetPath.getFileName() + ".png");
        }

        WritableImage snapshot = chart.snapshot(new SnapshotParameters(), null);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);
        boolean saved = ImageIO.write(bufferedImage, "png", targetPath.toFile());

        if (!saved) {
            throw new IOException("Brak obsługi zapisu obrazu PNG.");
        }

        return targetPath;
    }

    private static String withExtension(String fileName, String extension) {
        if (fileName == null || fileName.isBlank()) {
            return "chart" + extension;
        }
        return fileName.toLowerCase().endsWith(extension) ? fileName : fileName + extension;
    }
}
