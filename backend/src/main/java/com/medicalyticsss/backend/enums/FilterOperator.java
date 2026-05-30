package com.medicalyticsss.backend.enums;

public enum FilterOperator {
    EQUALS,         // Równe (=)
    NOT_EQUALS,     // Różne (!=)
    GREATER_THAN,   // Większe niż (>)
    LESS_THAN,      // Mniejsze niż (<)
    CONTAINS,       // LIKE %...%
    IN,             // Wartość znajduje się na liście rozdzielonej przecinkami
    BETWEEN         // Wartość znajduje się między dwiema wartościami rozdzielonymi przecinkiem
}