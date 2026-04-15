package com.medicalyticsss.backend.service;

import com.medicalyticsss.backend.model.*;
import com.medicalyticsss.backend.repository.*;
import com.medicalyticsss.backend.util.HashUtils;
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
import java.util.Optional;

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
                    // przekazuje fileHistory, zeby moc przupisac wynik do tego pliku
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

        // wyciaganie danych z csv

        String name = record.get("imie");
        String lastName = record.get("nazwisko");
        String birthDateStr = record.get("data_urodzenia");
        String genderStr = record.get("plec");
        String pesel = record.get("pesel");

        String facilityNameStr = record.get("placowka_nazwa");
        String cityStr = record.get("miasto");

        String testCodeStr = record.get("kod_badania");
        String testNameStr = record.get("nazwa_badania");
        String unitStr = record.get("jednostka");

        String resultStr = record.get("wynik");
        String normMinStr = record.get("norma_min");
        String normMaxStr = record.get("norma_max");

        // walidacja i zapis pacjenta

        if (name == null || name.isEmpty() || lastName == null || lastName.isEmpty()) {
            throw new IllegalArgumentException("Brak imienia lub nazwiska");
        }
        if (pesel == null || pesel.length() != 11) {
            throw new IllegalArgumentException("Nieprawidłowy numer PESEL");
        }

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

        // szukamy pacjenta, a jeśli go nie ma - tworzymy
        Patient patient = patientRepository.findByPatientHash(hash).orElseGet(() -> {
            Patient newPatient = new Patient();
            newPatient.setPatientHash(hash);
            newPatient.setBirthYear(birthYear);
            newPatient.setGender(gender);
            return patientRepository.save(newPatient);
        });

        // walidacja i zapis placowki
        if (facilityNameStr == null || facilityNameStr.isBlank()) {
            throw new IllegalArgumentException("Brak nazwy placówki");
        }
        Facility facility = facilityRepository.findByFacilityNameAndCity(facilityNameStr, cityStr).orElseGet(() -> {
            Facility newFacility = new Facility();
            newFacility.setFacilityName(facilityNameStr);
            newFacility.setCity(cityStr);
            return facilityRepository.save(newFacility);
        });

        // walidacja i zapis slownika badan
        if (testCodeStr == null || testCodeStr.isBlank() || testNameStr == null || testNameStr.isBlank()) {
            throw new IllegalArgumentException("Brak kodu lub nazwy badania");
        }
        TestType testType = testTypeRepository.findByTestCode(testCodeStr).orElseGet(() -> {
            TestType newTest = new TestType();
            newTest.setTestCode(testCodeStr);
            newTest.setTestName(testNameStr);
            newTest.setUnit(unitStr);
            return testTypeRepository.save(newTest);
        });

        // parsowanie wynikow i norm
        BigDecimal resultValue;
        try {
            // Replace zabezpiecza przed wpisaniem "15,5" zamiast "15.5"
            resultValue = new BigDecimal(resultStr.replace(",", "."));
        } catch (Exception e) {
            throw new IllegalArgumentException("Wynik badania nie jest prawidłową liczbą: " + resultStr);
        }

        BigDecimal normMin = null;
        BigDecimal normMax = null;
        try {
            if (normMinStr != null && !normMinStr.isBlank()) normMin = new BigDecimal(normMinStr.replace(",", "."));
            if (normMaxStr != null && !normMaxStr.isBlank()) normMax = new BigDecimal(normMaxStr.replace(",", "."));
        } catch (Exception e) {
            throw new IllegalArgumentException("Błąd formatu liczbowego w normach");
        }

        // czy wynik jest poza normą?
        boolean isAbnormal = false;
        if (normMin != null && resultValue.compareTo(normMin) < 0) {
            isAbnormal = true; // zbyt niski
        }
        if (normMax != null && resultValue.compareTo(normMax) > 0) {
            isAbnormal = true; // zbyt wysoki
        }

        // ZAPIS WYNIKU DO BAZY
        FactTestResult fact = new FactTestResult();
        fact.setFileHistory(fileHistory); // powiazanie z plikiem
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
}