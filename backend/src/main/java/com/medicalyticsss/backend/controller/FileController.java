package com.medicalyticsss.backend.controller;

import com.medicalyticsss.backend.model.FileHistory;
import com.medicalyticsss.backend.model.FileStatus;
import com.medicalyticsss.backend.repository.FileHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/files")
public class FileController {

    // Pobiera ścieżkę "uploads" z pliku application.properties (domyślnie "uploads")
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private final FileHistoryRepository fileHistoryRepository;

    public FileController(FileHistoryRepository fileHistoryRepository) {
        this.fileHistoryRepository = fileHistoryRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {

        // 1. Walidacja: Czy plik nie jest pusty?
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Błąd: Wybrany plik jest pusty!");
        }

        // 2. Walidacja: Czy to na pewno plik CSV?
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !originalFileName.toLowerCase().endsWith(".csv")) {
            return ResponseEntity.badRequest().body("Błąd: Akceptujemy tylko pliki z rozszerzeniem .csv!");
        }

        try {
            // 3. Przygotowanie folderu na dysku (tworzy folder uploads jeśli nie istnieje)
            Path copyLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(copyLocation);

            // 4. Pełna ścieżka docelowa pliku
            Path targetLocation = copyLocation.resolve(originalFileName);

            // 5. Zapis fizyczny pliku na dysku (nadpisuje, jeśli taki już istnieje)
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 6. Zapis informacji o wgraniu do bazy danych
            FileHistory history = new FileHistory();
            history.setFileName(originalFileName);

            // 7. Czas uploadu
            history.setUploadTime(LocalDateTime.now());

            // 8. Użyte ENUMy
            history.setStatus(FileStatus.UPLOADED);

            // Inicjalizacja liczników
            history.setSuccessCount(0);
            history.setErrorCount(0);

            // history.setUser(null); // Tutaj bedzie user w przyslzosci

            fileHistoryRepository.save(history);

            return ResponseEntity.ok("Plik '" + originalFileName + "' został pomyślnie wgrany i zapisany w bazie.");

        } catch (Exception e) {
            //  Ogólne Exception, żeby złapać też błędy SQL
            e.printStackTrace();
            return ResponseEntity.status(500).body("Błąd serwera podczas przetwarzania pliku: " + e.getMessage());
        }
    }
}