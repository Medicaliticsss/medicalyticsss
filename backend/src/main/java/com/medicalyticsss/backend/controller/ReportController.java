package com.medicalyticsss.backend.controller;

import com.medicalyticsss.backend.dto.CustomReportRequest;
import com.medicalyticsss.backend.dto.ReportDataPoint;
import com.medicalyticsss.backend.dto.ReportSummaryDto;
import com.medicalyticsss.backend.dto.SeriesReportDataPoint;
import com.medicalyticsss.backend.dto.SeriesReportRequest;
import com.medicalyticsss.backend.repository.FactTestResultRepository;
import com.medicalyticsss.backend.service.CustomReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final FactTestResultRepository factTestResultRepository;
    private final CustomReportService customReportService; // Wstrzykujemy nasz nowy silnik

    // Zaktualizowany konstruktor
    public ReportController(FactTestResultRepository factTestResultRepository, CustomReportService customReportService) {
        this.factTestResultRepository = factTestResultRepository;
        this.customReportService = customReportService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ReportSummaryDto> getSummary() {
        try {
            // agregacje z bazy danych
            long totalTests = factTestResultRepository.count();
            long abnormalResults = factTestResultRepository.countByIsAbnormal(true);
            long normalResults = totalTests - abnormalResults; // Szybsze niż drugie zapytanie do bazy

            ReportSummaryDto summary = new ReportSummaryDto(totalTests, normalResults, abnormalResults);

            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    //ENDPOINT DLA KREATORA RAPORTÓW )
    @PostMapping("/custom")
    public ResponseEntity<List<ReportDataPoint>> getCustomReport(@RequestBody CustomReportRequest request) {
        try {
            List<ReportDataPoint> reportData = customReportService.generateCustomReport(request);
            return ResponseEntity.ok(reportData);
        } catch (IllegalArgumentException e) {
            System.err.println("Błędne parametry zapytania: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // ENDPOINT DLA DANYCH SUROWYCH
    @PostMapping("/raw")
    public ResponseEntity<List<Map<String, Object>>> getRawData(@RequestBody CustomReportRequest request) {
        try {
            List<Map<String, Object>> rawData = customReportService.getRawData(request.filters());
            return ResponseEntity.ok(rawData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // ENDPOINT DLA ELASTYCZNEJ TABELI RAPORTOWEJ (AGREGACJA OPCJONALNA)
    @PostMapping("/custom/table")
    public ResponseEntity<List<Map<String, Object>>> getCustomReportRows(@RequestBody CustomReportRequest request) {
        try {
            List<Map<String, Object>> reportRows = customReportService.generateCustomReportRows(request);
            return ResponseEntity.ok(reportRows);
        } catch (IllegalArgumentException e) {
            System.err.println("Błędne parametry zapytania: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // ENDPOINT DLA WYKRESÓW WIELOSERYJNYCH
    @PostMapping("/series")
    public ResponseEntity<List<SeriesReportDataPoint>> getSeriesReport(@RequestBody SeriesReportRequest request) {
        try {
            List<SeriesReportDataPoint> reportData = customReportService.generateSeriesReport(request);
            return ResponseEntity.ok(reportData);
        } catch (IllegalArgumentException e) {
            System.err.println("Błędne parametry zapytania: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}