package com.medicalyticsss.backend.service;

import com.medicalyticsss.backend.dto.TestTypeDictionaryEntry;
import com.medicalyticsss.backend.model.TestType;
import com.medicalyticsss.backend.repository.TestTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DictionaryService {

    private final TestTypeRepository testTypeRepository;

    public DictionaryService(TestTypeRepository testTypeRepository) {
        this.testTypeRepository = testTypeRepository;
    }

    public List<TestTypeDictionaryEntry> findAll() {
        return testTypeRepository.findAll().stream()
                .sorted(Comparator.comparing(TestType::getTestCode))
                .map(this::toEntry)
                .toList();
    }

    @Transactional
    public TestTypeDictionaryEntry update(String testCode, TestTypeDictionaryEntry entry) {
        String normalizedCode = normalizeRequired(testCode, "testCode");
        validateEntry(entry);
        if (!normalizedCode.equals(normalizeRequired(entry.getTestCode(), "testCode"))) {
            throw new IllegalArgumentException("Kod badania w adresie nie zgadza się z danymi żądania.");
        }

        TestType existing = testTypeRepository.findByTestCode(normalizedCode)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono badania o kodzie: " + normalizedCode));

        applyEntry(existing, entry);
        testTypeRepository.save(existing);
        return toEntry(existing);
    }

    @Transactional
    public int syncFromEntries(List<TestTypeDictionaryEntry> entries) {
        validateDictionary(entries);

        int synced = 0;
        for (TestTypeDictionaryEntry entry : entries) {
            TestType authTest = toEntity(entry);
            TestType existing = testTypeRepository.findByTestCode(authTest.getTestCode()).orElse(null);

            if (existing == null) {
                testTypeRepository.save(authTest);
            } else {
                applyEntry(existing, entry);
                testTypeRepository.save(existing);
            }
            synced++;
        }
        return synced;
    }

    public void validateDictionary(List<TestTypeDictionaryEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("Słownik badań jest pusty.");
        }

        Set<String> codes = new HashSet<>();
        for (TestTypeDictionaryEntry entry : entries) {
            validateEntry(entry);
            String code = normalizeRequired(entry.getTestCode(), "testCode");
            if (!codes.add(code)) {
                throw new IllegalArgumentException("Słownik zawiera zduplikowany testCode: " + code);
            }
        }
    }

    private void validateEntry(TestTypeDictionaryEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Wpis słownika badań jest pusty.");
        }

        String code = normalizeRequired(entry.getTestCode(), "testCode");
        if (normalizeRequired(entry.getTestName(), "testName") == null) {
            throw new IllegalArgumentException("Wpis słownika badań " + code + " nie zawiera pola testName.");
        }
        if (entry.getNormMin() != null && entry.getNormMax() != null
                && entry.getNormMin().compareTo(entry.getNormMax()) > 0) {
            throw new IllegalArgumentException("Wpis słownika badań " + code + " ma normMin większe od normMax.");
        }
    }

    private void applyEntry(TestType target, TestTypeDictionaryEntry entry) {
        target.setTestName(normalizeRequired(entry.getTestName(), "testName"));
        target.setCategoryName(normalize(entry.getCategoryName()));
        target.setUnit(normalize(entry.getUnit()));
        target.setNormMin(entry.getNormMin());
        target.setNormMax(entry.getNormMax());
    }

    private TestType toEntity(TestTypeDictionaryEntry entry) {
        TestType testType = new TestType();
        testType.setTestCode(normalizeRequired(entry.getTestCode(), "testCode"));
        applyEntry(testType, entry);
        return testType;
    }

    private TestTypeDictionaryEntry toEntry(TestType testType) {
        TestTypeDictionaryEntry entry = new TestTypeDictionaryEntry();
        entry.setTestCode(testType.getTestCode());
        entry.setTestName(testType.getTestName());
        entry.setCategoryName(testType.getCategoryName());
        entry.setUnit(testType.getUnit());
        entry.setNormMin(testType.getNormMin());
        entry.setNormMax(testType.getNormMax());
        return entry;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException("Pole " + fieldName + " jest wymagane.");
        }
        return normalized;
    }
}
