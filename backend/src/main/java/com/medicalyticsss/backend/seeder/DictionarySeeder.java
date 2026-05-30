package com.medicalyticsss.backend.seeder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medicalyticsss.backend.dto.TestTypeDictionaryEntry;
import com.medicalyticsss.backend.model.TestType;
import com.medicalyticsss.backend.repository.TestTypeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DictionarySeeder implements CommandLineRunner {

    private final TestTypeRepository testTypeRepository;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${dictionary.tests.path:classpath:dictionaries/test-types.json}")
    private String dictionaryPath;

    public DictionarySeeder(TestTypeRepository testTypeRepository, ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.testTypeRepository = testTypeRepository;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(String... args) throws IOException {
        List<TestTypeDictionaryEntry> authoritativeTests = loadDictionaryEntries();

        for (TestTypeDictionaryEntry entry : authoritativeTests) {
            TestType authTest = createTest(entry);
            TestType existing = testTypeRepository.findByTestCode(authTest.getTestCode()).orElse(null);

            if (existing == null) {
                // Jeśli badania nie ma jeszcze w bazie, dodajemy je
                testTypeRepository.save(authTest);
            } else {
                // MDM: Jeśli badanie istnieje, WYMUSZAMY poprawne normy, nazwy i jednostki.
                // To naprawi historyczne błędy (np. złe normy wgrane ze starych plików CSV).
                existing.setTestName(authTest.getTestName());
                existing.setCategoryName(authTest.getCategoryName());
                existing.setUnit(authTest.getUnit());
                existing.setNormMin(authTest.getNormMin());
                existing.setNormMax(authTest.getNormMax());
                testTypeRepository.save(existing);
            }
        }

        System.out.println("Słownik badań (MDM) został pomyślnie załadowany i zsynchronizowany z: " + dictionaryPath);
    }

    private List<TestTypeDictionaryEntry> loadDictionaryEntries() throws IOException {
        Resource dictionaryResource = resourceLoader.getResource(dictionaryPath);
        if (!dictionaryResource.exists()) {
            throw new IllegalStateException("Nie znaleziono pliku słownika badań: " + dictionaryPath);
        }

        List<TestTypeDictionaryEntry> entries;
        try (InputStream dictionaryStream = dictionaryResource.getInputStream()) {
            entries = objectMapper.readValue(dictionaryStream, new TypeReference<>() {});
        }
        validateDictionary(entries);
        return entries;
    }

    private void validateDictionary(List<TestTypeDictionaryEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new IllegalStateException("Plik słownika badań jest pusty: " + dictionaryPath);
        }

        Set<String> codes = new HashSet<>();
        for (TestTypeDictionaryEntry entry : entries) {
            if (entry == null) {
                throw new IllegalStateException("Plik słownika badań zawiera pusty wpis: " + dictionaryPath);
            }

            String code = normalize(entry.getTestCode());
            if (code == null) {
                throw new IllegalStateException("Wpis słownika badań nie zawiera pola testCode: " + dictionaryPath);
            }
            if (!codes.add(code)) {
                throw new IllegalStateException("Plik słownika badań zawiera zduplikowany testCode: " + code);
            }
            if (normalize(entry.getTestName()) == null) {
                throw new IllegalStateException("Wpis słownika badań " + code + " nie zawiera pola testName");
            }
            if (entry.getNormMin() != null && entry.getNormMax() != null
                    && entry.getNormMin().compareTo(entry.getNormMax()) > 0) {
                throw new IllegalStateException("Wpis słownika badań " + code + " ma normMin większe od normMax");
            }
        }
    }

    private TestType createTest(TestTypeDictionaryEntry entry) {
        TestType t = new TestType();
        t.setTestCode(normalize(entry.getTestCode()));
        t.setTestName(normalize(entry.getTestName()));
        t.setCategoryName(normalize(entry.getCategoryName()));
        t.setUnit(normalize(entry.getUnit()));
        t.setNormMin(entry.getNormMin());
        t.setNormMax(entry.getNormMax());
        return t;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}