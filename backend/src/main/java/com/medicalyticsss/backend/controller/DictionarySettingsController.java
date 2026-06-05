package com.medicalyticsss.backend.controller;

import com.medicalyticsss.backend.dto.DictionaryImportResultDto;
import com.medicalyticsss.backend.dto.TestTypeDictionaryEntry;
import com.medicalyticsss.backend.service.DictionaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settings/dictionary")
public class DictionarySettingsController {

    private final DictionaryService dictionaryService;

    public DictionarySettingsController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping
    public ResponseEntity<List<TestTypeDictionaryEntry>> listDictionary(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(dictionaryService.findAll());
    }

    @GetMapping("/export")
    public ResponseEntity<List<TestTypeDictionaryEntry>> exportDictionary(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(dictionaryService.findAll());
    }

    @PutMapping("/{testCode}")
    public ResponseEntity<?> updateEntry(@PathVariable String testCode,
                                         @RequestBody TestTypeDictionaryEntry entry,
                                         Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(401).body("Błąd: Brak autoryzacji.");
        }

        try {
            return ResponseEntity.ok(dictionaryService.update(testCode, entry));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/import")
    public ResponseEntity<?> importDictionary(@RequestBody List<TestTypeDictionaryEntry> entries,
                                              Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(401).body("Błąd: Brak autoryzacji.");
        }

        try {
            int synced = dictionaryService.syncFromEntries(entries);
            return ResponseEntity.ok(new DictionaryImportResultDto(
                    synced,
                    "Słownik badań został zaimportowany pomyślnie. Zsynchronizowano wpisów: " + synced
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }
}
