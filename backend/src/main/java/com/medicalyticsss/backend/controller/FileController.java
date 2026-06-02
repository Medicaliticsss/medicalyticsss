package com.medicalyticsss.backend.controller;

import com.medicalyticsss.backend.model.FileHistory;
import com.medicalyticsss.backend.model.FileStatus;
import com.medicalyticsss.backend.model.ProcessingError;
import com.medicalyticsss.backend.repository.FileHistoryRepository;
import com.medicalyticsss.backend.repository.ProcessingErrorRepository;
import com.medicalyticsss.backend.service.CsvProcessingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import com.medicalyticsss.backend.model.User;
import com.medicalyticsss.backend.repository.UserRepository;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private final FileHistoryRepository fileHistoryRepository;
    private final CsvProcessingService csvProcessingService;
    private final ProcessingErrorRepository processingErrorRepository;
    private final UserRepository userRepository;

    public FileController(FileHistoryRepository fileHistoryRepository, CsvProcessingService csvProcessingService, ProcessingErrorRepository processingErrorRepository, UserRepository userRepository) {
        this.fileHistoryRepository = fileHistoryRepository;
        this.csvProcessingService = csvProcessingService;
        this.processingErrorRepository = processingErrorRepository;
        this.userRepository = userRepository;
    }

    // tu ja Natalia dodalam endpointa - a potem ZAKTUALIZOWALAM pod filtrowanie uzytkownika
    @GetMapping
    public ResponseEntity<Iterable<FileHistory>> getAllFiles(Authentication authentication) {

        // Zabezpieczenie na wypadek, gdyby ktoś z frontendu próbował pobrać pliki bez logowania
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        // 1. Pobieramy login (username) ze Spring Security
        String username = authentication.getName();

        // 2. Pobieramy z bazy tylko pliki przypisane do tego loginu
        return ResponseEntity.ok(fileHistoryRepository.findByUser_Username(username));
    }

    // TYLKO WGRYWANIE NA SUCHO - ZAKTUALIZOWANE O USERA
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file, Authentication authentication) { // <--- DODANY PARAMETR

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Błąd: Wybrany plik jest pusty!");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !originalFileName.toLowerCase().endsWith(".csv")) {
            return ResponseEntity.badRequest().body("Błąd: Akceptujemy tylko pliki z rozszerzeniem .csv!");
        }

        try {
            Path copyLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(copyLocation);

            int dotIndex = originalFileName.lastIndexOf('.');
            String baseName = originalFileName.substring(0, dotIndex);
            String extension = originalFileName.substring(dotIndex);

            String finalFileName = originalFileName;
            Path targetLocation = copyLocation.resolve(finalFileName);
            int counter = 1;

            while (Files.exists(targetLocation)) {
                finalFileName = baseName + "(" + counter + ")" + extension;
                targetLocation = copyLocation.resolve(finalFileName);
                counter++;
            }

            Files.copy(file.getInputStream(), targetLocation);

            FileHistory history = new FileHistory();
            history.setFileName(finalFileName);
            history.setUploadTime(LocalDateTime.now());
            history.setStatus(FileStatus.UPLOADED);
            history.setSuccessCount(0);
            history.setErrorCount(0);

            // >>> NOWA LOGIKA: Szukamy zalogowanego użytkownika i przypisujemy go do pliku <<<
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                Optional<User> loggedInUser = userRepository.findByUsername(username);

                if (loggedInUser.isPresent()) {
                    history.setUser(loggedInUser.get()); // Przypisujemy pełny obiekt User do pliku!
                } else {
                    return ResponseEntity.status(401).body("Błąd: Nie znaleziono zalogowanego użytkownika w bazie.");
                }
            } else {
                return ResponseEntity.status(401).body("Błąd: Brak autoryzacji do wgrania pliku.");
            }
            // >>> KONIEC NOWEJ LOGIKI <<<

            fileHistoryRepository.save(history);

            return ResponseEntity.ok("Plik '" + finalFileName + "' został pomyślnie wgrany na serwer.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Błąd serwera podczas zapisu pliku: " + e.getMessage());
        }
    }

    // WYWOŁANIE PRZETWARZANIA
    // tu Natalia zmienilam na Post
    @PostMapping("/{id}/process")
    public ResponseEntity<String> processExistingFile(@PathVariable Long id) {

        // Szukamy pliku w bazie po ID
        Optional<FileHistory> fileHistoryOpt = fileHistoryRepository.findById(id);

        if (fileHistoryOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Błąd: Nie znaleziono pliku o ID " + id);
        }

        FileHistory history = fileHistoryOpt.get();

        // Zabezpieczenie: Przetwarzamy tylko te, które mają status UPLOADED
        if (history.getStatus() != FileStatus.UPLOADED) {
            return ResponseEntity.badRequest().body("Błąd: Plik został już przetworzony lub ma status błędu.");
        }

        try {
            // Znajdujemy ścieżkę do pliku na dysku
            Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(history.getFileName());

            // Odpala przetwarzanie
            csvProcessingService.processFile(history, filePath);

            return ResponseEntity.ok("Proces przetwarzania zakończony. Sprawdź status pliku.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Błąd podczas przetwarzania: " + e.getMessage());
        }
    }

    // SOFT DELETE I ROLLBACK
    @PostMapping("/{id}/delete")
    public ResponseEntity<String> deleteFile(@PathVariable Long id) {
        try {
            // Wywołujemy przygotowaną logikę czyszczenia danych i zmiany statusu
            csvProcessingService.softDeleteAndRollback(id);
            return ResponseEntity.ok("Plik pomyślnie usunięty, a dane zrolowane.");
        } catch (IllegalArgumentException e) {
            // Zwracamy kod 404 Not Found, jeśli ID pliku nie istnieje
            return ResponseEntity.status(404).body("Błąd: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Wystąpił błąd podczas usuwania pliku: " + e.getMessage());
        }
    }

    // PODGLĄD PLIKU (PREVIEW)
    @GetMapping("/{id}/preview")
    public ResponseEntity<?> previewFile(@PathVariable Long id, @RequestParam(defaultValue = "50") int limit) {
        // Szukamy historii pliku w bazie
        Optional<FileHistory> fileHistoryOpt = fileHistoryRepository.findById(id);

        if (fileHistoryOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Błąd: Nie znaleziono pliku o ID " + id);
        }

        FileHistory history = fileHistoryOpt.get();

        // Szukamy pliku na dysku
        Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(history.getFileName());

        if (!Files.exists(filePath)) {
            return ResponseEntity.status(404).body("Błąd: Fizyczny plik nie istnieje na serwerze.");
        }

        // Strumieniowe czytanie pliku
        try (java.util.stream.Stream<String> lines = Files.lines(filePath, java.nio.charset.StandardCharsets.UTF_8)) {
            // Pobieramy tylko 'limit' pierwszych linii (domyślnie 50) i pakujemy do listy
            java.util.List<String> previewLines = lines.limit(limit).collect(java.util.stream.Collectors.toList());

            // Zwracamy listę wierszy w formacie JSON
            return ResponseEntity.ok(previewLines);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Błąd podczas odczytu pliku: " + e.getMessage());
        }
    }
    @GetMapping("/{id}/errors")
    public ResponseEntity<java.util.List<ProcessingError>> getFileErrors(@PathVariable Long id) {
        if (!fileHistoryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        java.util.List<ProcessingError> errors = processingErrorRepository.findByFileHistoryId(id);
        return ResponseEntity.ok(errors);
    }
}
