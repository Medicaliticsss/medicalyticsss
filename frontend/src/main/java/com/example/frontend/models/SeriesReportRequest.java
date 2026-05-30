package com.example.frontend.models;

import java.util.List;

public record SeriesReportRequest(
        String xAxis,
        String seriesField,
        String aggregateColumn,
        String operation,
        String sortDirection,
        List<ReportFilter> filters
) {}
