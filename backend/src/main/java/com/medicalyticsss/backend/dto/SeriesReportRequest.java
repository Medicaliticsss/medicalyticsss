package com.medicalyticsss.backend.dto;

import com.medicalyticsss.backend.enums.ReportField;
import java.util.List;

public record SeriesReportRequest(
        ReportField xAxis,
        ReportField seriesField,
        ReportField aggregateColumn,
        String operation,
        String sortDirection,
        List<ReportFilter> filters
) {}
