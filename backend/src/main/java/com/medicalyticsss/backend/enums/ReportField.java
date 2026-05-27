package com.medicalyticsss.backend.enums;

public enum ReportField {
    // Wymiar: Pacjent
    PATIENT_GENDER,
    PATIENT_BIRTH_YEAR,

    // Wymiar: Placówka
    FACILITY_NAME,
    FACILITY_CITY,
    FACILITY_PROVINCE,

    // Wymiar: Badanie (Słownik)
    TEST_CODE,
    TEST_NAME,
    TEST_CATEGORY,

    // Fakty (Wartości liczbowe do agregacji)
    RESULT_VALUE,
    IS_ABNORMAL
}