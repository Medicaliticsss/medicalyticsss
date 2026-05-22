package com.example.frontend.views;

import com.example.frontend.models.ReportSummary;
import com.example.frontend.services.ReportService;
import com.example.frontend.utils.ViewManager;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ReportView {

    public static Parent getView() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(30));

        // --- NAGŁÓWEK ---
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label("Raport Globalny");
        titleLabel.getStyleClass().add(Styles.TITLE_1);

        Button backButton = new Button("Powrót do Menu");
        backButton.getStyleClass().add(Styles.BUTTON_OUTLINED);
        backButton.setOnAction(e -> ViewManager.switchView(MainMenuView.getView()));

        header.getChildren().addAll(backButton, titleLabel);
        root.setTop(header);

        // --- KAFELKI KPI (Liczby) ---
        HBox kpiContainer = new HBox(20);
        kpiContainer.setAlignment(Pos.CENTER);
        kpiContainer.setPadding(new Insets(20));

        // Tworzymy etykiety na dane (zaraz je wypełnimy)
        Label totalVal = new Label("0");
        Label normalVal = new Label("0");
        Label anomaliesVal = new Label("0");

        kpiContainer.getChildren().addAll(
                createKPICard("Wszystkie badania", totalVal, Styles.TEXT_BOLD),
                createKPICard("W normie", normalVal, Styles.SUCCESS),
                createKPICard("Anomalie", anomaliesVal, Styles.DANGER)
        );

        // --- WYKRES ---
        PieChart reportChart = new PieChart();
        reportChart.setTitle("Proporcja wyników");
        reportChart.setLabelsVisible(true);

        VBox centerLayout = new VBox(30, kpiContainer, reportChart);
        centerLayout.setAlignment(Pos.CENTER);
        root.setCenter(centerLayout);

        // --- ŁADOWANIE DANYCH Z SERWISU ---
        ReportService.fetchSummary().thenAccept(summary -> {
            Platform.runLater(() -> {
                if (summary != null) {
                    // Aktualizacja liczb
                    totalVal.setText(String.valueOf(summary.totalTests));
                    normalVal.setText(String.valueOf(summary.normalResults));
                    anomaliesVal.setText(String.valueOf(summary.abnormalResults));

                    // Aktualizacja wykresu
                    reportChart.getData().clear();
                    if (summary.totalTests > 0) {
                        reportChart.getData().add(new PieChart.Data("W normie", summary.normalResults));
                        reportChart.getData().add(new PieChart.Data("Anomalie", summary.abnormalResults));
                    }
                } else {
                    totalVal.setText("Brak danych");
                }
            });
        });

        return root;
    }

    // Pomocnicza metoda do tworzenia tych ładnych kart, którą miałaś w Main
    private static VBox createKPICard(String title, Label valueLabel, String styleClass) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #2D2D30; -fx-background-radius: 10; -fx-min-width: 200;");

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add(Styles.TEXT_MUTED);

        valueLabel.getStyleClass().add(Styles.TITLE_2);
        valueLabel.getStyleClass().add(styleClass);

        box.getChildren().addAll(titleLbl, valueLabel);
        return box;
    }
}