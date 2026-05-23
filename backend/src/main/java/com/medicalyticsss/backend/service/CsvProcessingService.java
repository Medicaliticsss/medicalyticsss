package com.medicalyticsss.backend.service;

import com.medicalyticsss.backend.model.*;
import com.medicalyticsss.backend.repository.*;
import com.medicalyticsss.backend.util.HashUtils;
import com.medicalyticsss.backend.util.StringSanitizerUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Service
public class CsvProcessingService {

    private final PatientRepository patientRepository;
    private final ProcessingErrorRepository errorRepository;
    private final FileHistoryRepository fileHistoryRepository;
    private final FacilityRepository facilityRepository;
    private final TestTypeRepository testTypeRepository;
    private final FactTestResultRepository factTestResultRepository;

    public CsvProcessingService(PatientRepository patientRepository,
                                ProcessingErrorRepository errorRepository,
                                FileHistoryRepository fileHistoryRepository,
                                FacilityRepository facilityRepository,
                                TestTypeRepository testTypeRepository,
                                FactTestResultRepository factTestResultRepository) {
        this.patientRepository = patientRepository;
        this.errorRepository = errorRepository;
        this.fileHistoryRepository = fileHistoryRepository;
        this.facilityRepository = facilityRepository;
        this.testTypeRepository = testTypeRepository;
        this.factTestResultRepository = factTestResultRepository;
    }

    @Transactional
    public void processFile(FileHistory fileHistory, Path filePath) {
        int successCount = 0;
        int errorCount = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build();

        try (Reader reader = Files.newBufferedReader(filePath);
             CSVParser csvParser = new CSVParser(reader, format)) {

            for (CSVRecord record : csvParser) {
                try {
                    processSingleRecord(record, fileHistory);
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    logError(fileHistory, record, e.getMessage());
                }
            }

            fileHistory.setSuccessCount(successCount);
            fileHistory.setErrorCount(errorCount);

            if (errorCount == 0) {
                fileHistory.setStatus(FileStatus.SUCCESS);
            } else if (successCount > 0) {
                fileHistory.setStatus(FileStatus.PARTIAL_SUCCESS);
            } else {
                fileHistory.setStatus(FileStatus.ERROR);
            }

            fileHistoryRepository.save(fileHistory);

        } catch (Exception e) {
            fileHistory.setStatus(FileStatus.ERROR);
            fileHistoryRepository.save(fileHistory);

            ProcessingError criticalError = new ProcessingError();
            criticalError.setFileHistory(fileHistory);
            criticalError.setErrorMessage("Krytyczny błąd odczytu pliku: " + e.getMessage());
            errorRepository.save(criticalError);
        }
    }

    private void processSingleRecord(CSVRecord record, FileHistory fileHistory) {

        // 1. WYCIĄGANIE DANYCH Z CSV
        String name = record.get("imie");
        String lastName = record.get("nazwisko");
        String birthDateStr = record.get("data_urodzenia");
        String genderStr = record.get("plec");
        String pesel = record.get("pesel");
        String facilityNameStr = record.get("placowka_nazwa");
        String cityStr = record.get("miasto");
        String provinceStr = record.get("wojewodztwo");
        String testCodeStr = record.get("kod_badania");
        String testNameStr = record.get("nazwa_badania");
        String categoryNameStr = record.get("kategoria_badania");
        String unitStr = record.get("jednostka");
        String resultStr = record.get("wynik");
        String normMinStr = record.get("norma_min");
        String normMaxStr = record.get("norma_max");

        // 2. ŚCISŁA WALIDACJA (ŻELAZNA BRAMKA)
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Brak imienia");
        if (lastName == null || lastName.isBlank()) throw new IllegalArgumentException("Brak nazwiska");
        if (birthDateStr == null || birthDateStr.isBlank()) throw new IllegalArgumentException("Brak daty urodzenia");
        if (genderStr == null || genderStr.isBlank()) throw new IllegalArgumentException("Brak płci");
        if (pesel == null || pesel.isBlank() || pesel.length() != 11) throw new IllegalArgumentException("Nieprawidłowy lub brakujący numer PESEL");
        if (facilityNameStr == null || facilityNameStr.isBlank()) throw new IllegalArgumentException("Brak nazwy placówki");
        if (cityStr == null || cityStr.isBlank()) throw new IllegalArgumentException("Brak miasta placówki");
        if (provinceStr == null || provinceStr.isBlank()) throw new IllegalArgumentException("Brak województwa placówki");
        if (testCodeStr == null || testCodeStr.isBlank()) throw new IllegalArgumentException("Brak kodu badania");
        if (testNameStr == null || testNameStr.isBlank()) throw new IllegalArgumentException("Brak nazwy badania");
        if (categoryNameStr == null || categoryNameStr.isBlank()) throw new IllegalArgumentException("Brak kategorii badania");
        if (unitStr == null || unitStr.isBlank()) throw new IllegalArgumentException("Brak jednostki badania");
        if (resultStr == null || resultStr.isBlank()) throw new IllegalArgumentException("Brak wyniku badania");
        if (normMinStr == null || normMinStr.isBlank()) throw new IllegalArgumentException("Brak normy minimalnej");
        if (normMaxStr == null || normMaxStr.isBlank()) throw new IllegalArgumentException("Brak normy maksymalnej");

        // 3. PARSOWANIE I ZAPIS PACJENTA
        int birthYear;
        try {
            LocalDate birthDate = LocalDate.parse(birthDateStr);
            birthYear = birthDate.getYear();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Nieprawidłowy format daty (oczekiwany RRRR-MM-DD): " + birthDateStr);
        }

        Gender gender;
        try {
            gender = Gender.valueOf(genderStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Nierozpoznana płeć (oczekiwane M lub F): " + genderStr);
        }

        String hash = HashUtils.generateHash(pesel);
        Patient patient = patientRepository.findByPatientHash(hash).orElseGet(() -> {
            Patient newPatient = new Patient();
            newPatient.setPatientHash(hash);
            newPatient.setBirthYear(birthYear);
            newPatient.setGender(gender);
            return patientRepository.save(newPatient);
        });

        // 4. SANITIZACJA I ZAPIS PLACÓWKI (MDM)
        String sanitizedFacilityName = StringSanitizerUtils.sanitize(facilityNameStr);
        String sanitizedCity = StringSanitizerUtils.sanitize(cityStr);
        String sanitizedProvince = StringSanitizerUtils.sanitize(provinceStr);

        Facility facility = facilityRepository.findByFacilityNameAndCity(sanitizedFacilityName, sanitizedCity).orElse(null);
        if (facility == null) {
            facility = new Facility();
            facility.setFacilityName(sanitizedFacilityName);
            facility.setCity(sanitizedCity);
            facility.setProvince(sanitizedProvince);
            facility = facilityRepository.save(facility);
        }

        // 5. PARSOWANIE NORM Z PLIKU (Tylko po to, by założyć badanie po raz pierwszy, jeśli go nie ma)
        BigDecimal parsedNormMin;
        BigDecimal parsedNormMax;
        try {
            parsedNormMin = new BigDecimal(normMinStr.replace(",", "."));
            parsedNormMax = new BigDecimal(normMaxStr.replace(",", "."));
        } catch (Exception e) {
            throw new IllegalArgumentException("Błąd formatu liczbowego w normach");
        }

        // 6. WALIDACJA I ZAPIS SŁOWNIKA BADAŃ (TWARDY SŁOWNIK)
        String sanitizedTestName = StringSanitizerUtils.sanitize(testNameStr);
        String sanitizedCategoryName = StringSanitizerUtils.sanitize(categoryNameStr);

        TestType testType = testTypeRepository.findByTestCode(testCodeStr).orElse(null);

        if (testType == null) {
            // Inicjalizacja słownika w bazie, jeśli kodu badania jeszcze nie było
            testType = new TestType();
            testType.setTestCode(testCodeStr);
            testType.setTestName(sanitizedTestName);
            testType.setCategoryName(sanitizedCategoryName);
            testType.setUnit(unitStr);
            testType.setNormMin(parsedNormMin);
            testType.setNormMax(parsedNormMax);
            testType = testTypeRepository.save(testType);
        }
        // USUNIĘTO Smart Update! Jeśli `testType` istnieje, kompletnie ignorujemy normy i jednostki z pliku CSV.

        // 7. PARSOWANIE WYNIKÓW
        BigDecimal resultValue;
        try {
            resultValue = new BigDecimal(resultStr.replace(",", "."));
        } catch (Exception e) {
            throw new IllegalArgumentException("Wynik badania nie jest prawidłową liczbą: " + resultStr);
        }

        // 8. CZY WYNIK JEST POZA NORMĄ? (Używamy norm z BAZY, a nie z CSV!)
        BigDecimal dbNormMin = testType.getNormMin();
        BigDecimal dbNormMax = testType.getNormMax();

        boolean isAbnormal = false;
        if (dbNormMin != null && resultValue.compareTo(dbNormMin) < 0) {
            isAbnormal = true; // zbyt niski
        } else if (dbNormMax != null && resultValue.compareTo(dbNormMax) > 0) {
            isAbnormal = true; // zbyt wysoki
        }

        // 9. ZAPIS WYNIKU DO BAZY
        FactTestResult fact = new FactTestResult();
        fact.setFileHistory(fileHistory);
        fact.setPatient(patient);
        fact.setFacility(facility);
        fact.setTestType(testType);
        fact.setResultValue(resultValue);
        fact.setIsAbnormal(isAbnormal);

        factTestResultRepository.save(fact);
    }

    private void logError(FileHistory fileHistory, CSVRecord record, String message) {
        ProcessingError error = new ProcessingError();
        error.setFileHistory(fileHistory);
        error.setErrorRowNumber((int) record.getRecordNumber());
        error.setErrorMessage(message);
        error.setRawLineData(record.toString());
        errorRepository.save(error);
    }

    @Transactional
    public void softDeleteAndRollback(Long fileId) {
        FileHistory fileHistory = fileHistoryRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono pliku o ID: " + fileId));

        errorRepository.deleteByFileHistoryId(fileId);
        factTestResultRepository.deleteByFileHistoryId(fileId);

        fileHistory.setStatus(FileStatus.DELETED);
        fileHistory.setSuccessCount(0);
        fileHistory.setErrorCount(0);

        fileHistoryRepository.save(fileHistory);
    }
}