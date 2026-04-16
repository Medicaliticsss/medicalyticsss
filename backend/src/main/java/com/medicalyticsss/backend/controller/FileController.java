package com.medicalyticsss.backend.controller;

import com.medicalyticsss.backend.model.FileHistory;
import com.medicalyticsss.backend.model.FileStatus;
import com.medicalyticsss.backend.repository.FileHistoryRepository;
import com.medicalyticsss.backend.service.CsvProcessingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    public FileController(FileHistoryRepository fileHistoryRepository, CsvProcessingService csvProcessingService) {
        this.fileHistoryRepository = fileHistoryRepository;
        this.csvProcessingService = csvProcessingService;
    }
    // tu ja Natalia dodalam endpointa
    @GetMapping
    public ResponseEntity<Iterable<FileHistory>> getAllFiles() {
        // Pobiera wszystkie rekordy z bazy, abyś mogła je wyświetlić na liście
        return ResponseEntity.ok(fileHistoryRepository.findAll());
    }

    // TYLKO WGRYWANIE NA SUCHO
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {

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
            Path targetLocation = copyLocation.resolve(originalFileName);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            FileHistory history = new FileHistory();
            history.setFileName(originalFileName);
            history.setUploadTime(LocalDateTime.now());
            history.setStatus(FileStatus.UPLOADED); // Zostaje w statusie UPLOADED
            history.setSuccessCount(0);
            history.setErrorCount(0);

            fileHistoryRepository.save(history);

            return ResponseEntity.ok("Plik '" + originalFileName + "' został pomyślnie wgrany na serwer.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Błąd serwera podczas zapisu pliku: " + e.getMessage());
        }
    }

    // NOWY ENDPOINT: WYWOŁANIE PRZETWARZANIA
    // tymczasoweo GET - tylko do testow!!!!! NATALKA TY ZROB FRONTEND PRZEZ PostMapping!!!!!!
  //tu Natalia zmienilam na Post
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

            // Odpala
            csvProcessingService.processFile(history, filePath);

            return ResponseEntity.ok("Proces przetwarzania zakończony. Sprawdź status pliku.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Błąd podczas przetwarzania: " + e.getMessage());
        }
    }
}