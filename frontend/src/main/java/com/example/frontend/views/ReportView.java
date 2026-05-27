package com.example.frontend.views;

import atlantafx.base.theme.Styles;
import com.example.frontend.models.CustomReportRequest;
import com.example.frontend.models.ReportDataPoint;
import com.example.frontend.models.ReportFilter;
import com.example.frontend.services.ReportService;
import com.example.frontend.utils.ViewManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReportView {

    // Główne elementy interfejsu
    private static TableView<ReportDataPoint> resultsTable;
    private static TableView<Map<String, Object>> rawDataTable; // Tabela dynamiczna (Słowniki)
    private static StackPane chartArea;

    // Elementy paska bocznego
    private static ComboBox<String> groupByCombo;
    private static ComboBox<String> operationCombo;
    private static ComboBox<String> targetColCombo;
    private static ComboBox<String> chartTypeCombo;
    private static VBox filtersContainer;
    private static List<ReportFilterRow> activeFilterRows = new ArrayList<>();

    public static Parent getView() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        activeFilterRows.clear();

        // PASEK BOCZNY
        VBox sidebar = createSidebar();
        root.setLeft(sidebar);

        // OBSZAR GŁÓWNY
        VBox mainArea = new VBox(20);
        mainArea.setPadding(new Insets(0, 0, 0, 20));

        // Nagłówek i Powrót
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        Button backButton = new Button("Powrót");
        backButton.getStyleClass().add(Styles.BUTTON_OUTLINED);
        backButton.setOnAction(e -> ViewManager.switchView(MainMenuView.getView()));
        Label titleLabel = new Label("Analiza Danych Medycznych (BI)");
        titleLabel.getStyleClass().add(Styles.TITLE_2);
        header.getChildren().addAll(backButton, titleLabel);

        // Panele wyników (Zakładki)
        TabPane resultsTabs = new TabPane();
        resultsTabs.getStyleClass().add(TabPane.STYLE_CLASS_FLOATING);

        // ZAKŁADKA 1: Data Grid (Agregacje)
        resultsTable = new TableView<>();
        resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ReportDataPoint, String> labelCol = new TableColumn<>("Wymiar (Kategoria)");
        labelCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().label()));

        TableColumn<ReportDataPoint, Number> valueCol = new TableColumn<>("Wynik (Wartość)");
        valueCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().value()));

        resultsTable.getColumns().addAll(labelCol, valueCol);
        Tab tableTab = new Tab("Agregacja (Tabela)", resultsTable);
        tableTab.setClosable(false);

        // ZAKŁADKA 2: Wizualizacja (Wykresy)
        chartArea = new StackPane(new Label("Skonfiguruj i wygeneruj raport, aby zobaczyć wykres"));
        Tab chartTab = new Tab("Wizualizacja (Wykres)", chartArea);
        chartTab.setClosable(false);

        // ZAKŁADKA 3: Surowe Dane (Dynamiczny SELECT *)
        rawDataTable = new TableView<>();
        Tab rawTab = new Tab("Surowe Dane (SELECT *)", rawDataTable);
        rawTab.setClosable(false);

        // Złożenie zakładek
        resultsTabs.getTabs().addAll(tableTab, chartTab, rawTab);
        VBox.setVgrow(resultsTabs, Priority.ALWAYS);

        mainArea.getChildren().addAll(header, resultsTabs);
        root.setCenter(mainArea);

        return root;
    }

    private static VBox createSidebar() {
        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(15));
        sidebar.setPrefWidth(320);
        sidebar.getStyleClass().add(Styles.BG_DEFAULT);
        sidebar.setStyle("-fx-background-color: #2D2D30; -fx-background-radius: 10;");

        Label configTitle = new Label("Konfiguracja");
        configTitle.getStyleClass().add(Styles.TITLE_4);

        groupByCombo = new ComboBox<>();
        groupByCombo.getItems().addAll("FACILITY_CITY", "FACILITY_NAME", "PATIENT_GENDER", "TEST_CATEGORY", "TEST_NAME", "PATIENT_BIRTH_YEAR");
        groupByCombo.setValue("PATIENT_GENDER");
        groupByCombo.setMaxWidth(Double.MAX_VALUE);

        operationCombo = new ComboBox<>();
        operationCombo.getItems().addAll("COUNT", "SUM", "AVG");
        operationCombo.setValue("COUNT");
        operationCombo.setMaxWidth(Double.MAX_VALUE);

        targetColCombo = new ComboBox<>();
        targetColCombo.getItems().addAll("RESULT_VALUE", "IS_ABNORMAL", "TEST_CODE");
        targetColCombo.setValue("TEST_CODE");
        targetColCombo.setMaxWidth(Double.MAX_VALUE);

        Label chartTypeTitle = new Label("Wizualizacja");
        chartTypeTitle.getStyleClass().add(Styles.TEXT_MUTED);

        chartTypeCombo = new ComboBox<>();
        chartTypeCombo.getItems().addAll("Słupkowy (Bar)", "Kołowy (Pie)", "Liniowy (Line)", "Punktowy (Scatter)");
        chartTypeCombo.setValue("Słupkowy (Bar)");
        chartTypeCombo.setMaxWidth(Double.MAX_VALUE);

        Label filterTitle = new Label("Filtry (WHERE)");
        filterTitle.getStyleClass().add(Styles.TEXT_MUTED);
        filtersContainer = new VBox(10);
        Button addFilterBtn = new Button("+ Dodaj filtr");
        addFilterBtn.getStyleClass().add(Styles.SMALL);
        addFilterBtn.setOnAction(e -> addFilterRow());

        Button runBtn = new Button("Uruchom Analizę");
        runBtn.getStyleClass().addAll(Styles.ACCENT, Styles.LARGE);
        runBtn.setMaxWidth(Double.MAX_VALUE);
        runBtn.setOnAction(e -> runReport());

        sidebar.getChildren().addAll(
                configTitle,
                new Label("Wymiar (Oś X):"), groupByCombo,
                new Label("Agregacja (Oś Y):"), operationCombo, targetColCombo,
                new Separator(),
                chartTypeTitle, chartTypeCombo,
                new Separator(),
                filterTitle, filtersContainer, addFilterBtn,
                new Separator(),
                runBtn
        );

        return sidebar;
    }

    private static void addFilterRow() {
        ReportFilterRow row = new ReportFilterRow();
        activeFilterRows.add(row);
        filtersContainer.getChildren().add(row.getView());
    }

    private static void runReport() {
        // Zbieranie aktywnych filtrów
        List<ReportFilter> filters = new ArrayList<>();
        for (ReportFilterRow row : activeFilterRows) {
            ReportFilter f = row.getFilter();
            if (f != null) filters.add(f);
        }

        CustomReportRequest request = new CustomReportRequest(
                List.of(groupByCombo.getValue()),
                targetColCombo.getValue(),
                operationCombo.getValue(),
                targetColCombo.getValue(),
                "DESC",
                filters
        );

        // STRZAŁ PO DANE DO WYKRESÓW I AGREGACJI
        ReportService.fetchCustomReport(request).thenAccept(data -> {
            Platform.runLater(() -> updateChartAndTable(data));
        });

        // STRZAŁ PO SUROWE DANE
        ReportService.fetchRawData(request).thenAccept(rawData -> {
            Platform.runLater(() -> buildDynamicRawTable(rawData));
        });
    }

    private static void updateChartAndTable(List<ReportDataPoint> data) {
        // Tabela Agregacji
        resultsTable.getItems().clear();
        resultsTable.getItems().addAll(data);

        // Wykres
        chartArea.getChildren().clear();

        if (data.isEmpty()) {
            chartArea.getChildren().add(new Label("Brak danych spełniających kryteria."));
            return;
        }

        String selectedChart = chartTypeCombo.getValue();

        if (selectedChart.equals("Kołowy (Pie)")) {
            PieChart pieChart = new PieChart();
            pieChart.setTitle("Proporcja wyników");
            for (ReportDataPoint dp : data) {
                pieChart.getData().add(new PieChart.Data(dp.label(), dp.value().doubleValue()));
            }
            chartArea.getChildren().add(pieChart);
            return;
        }

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Wymiar / Kategoria");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Wartość");

        XYChart<String, Number> chart;

        switch (selectedChart) {
            case "Liniowy (Line)":
                chart = new LineChart<>(xAxis, yAxis);
                break;
            case "Punktowy (Scatter)":
                chart = new ScatterChart<>(xAxis, yAxis);
                break;
            default:
                chart = new BarChart<>(xAxis, yAxis);
                break;
        }

        chart.setTitle("Analiza trendów i wartości");
        chart.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (ReportDataPoint dp : data) {
            series.getData().add(new XYChart.Data<>(dp.label(), dp.value()));
        }

        chart.getData().add(series);
        chartArea.getChildren().add(chart);
    }

    private static void buildDynamicRawTable(List<Map<String, Object>> rawData) {
        rawDataTable.getColumns().clear();
        rawDataTable.getItems().clear();

        if (rawData == null || rawData.isEmpty()) {
            rawDataTable.setPlaceholder(new Label("Brak surowych danych dla tych kryteriów."));
            return;
        }

        // Zbudowanie kolumn na podstawie kluczy z pierwszego wiersza
        Map<String, Object> firstRow = rawData.get(0);

        for (String key : firstRow.keySet()) {
            TableColumn<Map<String, Object>, Object> column = new TableColumn<>(key);

            // Kolumna szuka wartości w słowniku używając klucza
            column.setCellValueFactory(param -> {
                Object value = param.getValue().get(key);
                return new SimpleObjectProperty<>(value != null ? value : "Brak danych");
            });

            column.setPrefWidth(130);
            rawDataTable.getColumns().add(column);
        }

        // Wrzucenie wszystkich wierszy do tabeli
        rawDataTable.getItems().addAll(rawData);
    }

    //KLASA POMOCNICZA DLA WYSZUKIWARKI
    private static class ReportFilterRow {
        private final ComboBox<String> field = new ComboBox<>();
        private final ComboBox<String> op = new ComboBox<>();
        private final TextField val = new TextField();
        private final HBox view;

        public ReportFilterRow() {
            field.getItems().addAll("FACILITY_CITY", "PATIENT_GENDER", "TEST_CODE", "IS_ABNORMAL");
            field.setPrefWidth(120);
            op.getItems().addAll("EQUALS", "CONTAINS", "GREATER_THAN", "LESS_THAN");
            op.setPrefWidth(100);
            val.setPrefWidth(80);
            view = new HBox(5, field, op, val);
        }

        public HBox getView() { return view; }
        public ReportFilter getFilter() {
            if (field.getValue() == null || op.getValue() == null || val.getText().isBlank()) return null;
            return new ReportFilter(field.getValue(), op.getValue(), val.getText());
        }
    }
}