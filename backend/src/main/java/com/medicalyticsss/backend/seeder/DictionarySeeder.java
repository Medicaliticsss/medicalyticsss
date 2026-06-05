package com.medicalyticsss.backend.seeder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medicalyticsss.backend.dto.TestTypeDictionaryEntry;
import com.medicalyticsss.backend.service.DictionaryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
public class DictionarySeeder implements CommandLineRunner {

    private final DictionaryService dictionaryService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ResourceLoader resourceLoader;

    @Value("${dictionary.tests.path:classpath:dictionaries/test-types.json}")
    private String dictionaryPath;

    public DictionarySeeder(DictionaryService dictionaryService, ResourceLoader resourceLoader) {
        this.dictionaryService = dictionaryService;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(String... args) throws IOException {
        List<TestTypeDictionaryEntry> authoritativeTests = loadDictionaryEntries();
        int synced = dictionaryService.syncFromEntries(authoritativeTests);
        System.out.println("Słownik badań (MDM) został pomyślnie załadowany i zsynchronizowany z: "
                + dictionaryPath + " (wpisów: " + synced + ")");
    }

    private List<TestTypeDictionaryEntry> loadDictionaryEntries() throws IOException {
        Resource dictionaryResource = resourceLoader.getResource(dictionaryPath);
        if (!dictionaryResource.exists()) {
            throw new IllegalStateException("Nie znaleziono pliku słownika badań: " + dictionaryPath);
        }

        try (InputStream dictionaryStream = dictionaryResource.getInputStream()) {
            return objectMapper.readValue(dictionaryStream, new TypeReference<>() {});
        }
    }
}
