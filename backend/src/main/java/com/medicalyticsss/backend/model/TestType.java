package com.medicalyticsss.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "dim_test_type")
public class TestType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_code", nullable = false, length = 10)
    private String testCode;

    @Column(name = "test_name", nullable = false, length = 100)
    private String testName;

    @Column(name = "category_name", length = 50)
    private String categoryName;

    @Column(name = "norm_min", precision = 10, scale = 2)
    private BigDecimal normMin;

    @Column(name = "norm_max", precision = 10, scale = 2)
    private BigDecimal normMax;

    @Column(name = "unit", length = 20)
    private String unit;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTestCode() { return testCode; }
    public void setTestCode(String testCode) { this.testCode = testCode; }
    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public BigDecimal getNormMin() { return normMin; }
    public void setNormMin(BigDecimal normMin) { this.normMin = normMin; }
    public BigDecimal getNormMax() { return normMax; }
    public void setNormMax(BigDecimal normMax) { this.normMax = normMax; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}