package com.medicalyticsss.backend.dto;

public record ReportSummaryDto(
        long totalTests,
        long normalResults,
        long abnormalResults
) {
}