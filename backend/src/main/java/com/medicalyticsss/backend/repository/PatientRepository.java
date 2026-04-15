package com.medicalyticsss.backend.repository;

import com.medicalyticsss.backend.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    // do sprawdzania, czy pacjent już jest w bazie
    Optional<Patient> findByPatientHash(String patientHash);
}