package com.medicalyticsss.backend.dto;

import com.medicalyticsss.backend.enums.ReportField;
import java.util.List;

public record CustomReportRequest(
        List<ReportField> selectColumns,
        ReportField aggregateColumn,
        String operation,
        ReportField sortByColumn,
        String sortDirection,
        List<ReportFilter> filters
) {}