package com.medicalyticsss.backend.dto;

import java.math.BigDecimal;

public class TestTypeDictionaryEntry {

    private String testCode;
    private String testName;
    private String categoryName;
    private String unit;
    private BigDecimal normMin;
    private BigDecimal normMax;

    public String getTestCode() {
        return testCode;
    }

    public void setTestCode(String testCode) {
        this.testCode = testCode;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getNormMin() {
        return normMin;
    }

    public void setNormMin(BigDecimal normMin) {
        this.normMin = normMin;
    }

    public BigDecimal getNormMax() {
        return normMax;
    }

    public void setNormMax(BigDecimal normMax) {
        this.normMax = normMax;
    }
}
