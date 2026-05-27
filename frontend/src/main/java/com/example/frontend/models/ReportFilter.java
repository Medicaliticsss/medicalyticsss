package com.example.frontend.models;

public record ReportFilter(
        String field,
        String operator,
        String value
) {}