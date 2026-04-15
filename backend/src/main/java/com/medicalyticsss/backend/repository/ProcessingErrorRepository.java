package com.medicalyticsss.backend.repository;

import com.medicalyticsss.backend.model.ProcessingError;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessingErrorRepository extends JpaRepository<ProcessingError, Long> {
}