package com.medicalyticsss.backend.controller;

import com.medicalyticsss.backend.dto.ReportSummaryDto;
import com.medicalyticsss.backend.repository.FactTestResultRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final FactTestResultRepository factTestResultRepository;

    public ReportController(FactTestResultRepository factTestResultRepository) {
        this.factTestResultRepository = factTestResultRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<ReportSummaryDto> getSummary() {
        try {
            // agregacje z bazy danych
            long totalTests = factTestResultRepository.count();
            long abnormalResults = factTestResultRepository.countByIsAbnormal(true);
            long normalResults = totalTests - abnormalResults; // Szybsze niż drugie zapytanie do bazy

            // Pakujemy w nowy rekord i wysyłamy
            ReportSummaryDto summary = new ReportSummaryDto(totalTests, normalResults, abnormalResults);

            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}