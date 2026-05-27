package com.example.frontend.models;

import java.util.List;

public record CustomReportRequest(
        List<String> selectColumns,
        String aggregateColumn,
        String operation,
        String sortByColumn,
        String sortDirection,
        List<ReportFilter> filters
) {}