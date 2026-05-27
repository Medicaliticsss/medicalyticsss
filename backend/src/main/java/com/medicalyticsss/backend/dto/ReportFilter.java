package com.medicalyticsss.backend.dto;

import com.medicalyticsss.backend.enums.FilterOperator;
import com.medicalyticsss.backend.enums.ReportField;

public record ReportFilter(
        ReportField field,        // Jakie pole filtruje
        FilterOperator operator,  // Jak operuje
        String value              // Z czym porównujemy
) {}