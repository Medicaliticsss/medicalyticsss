package com.medicalyticsss.backend.repository;

import com.medicalyticsss.backend.model.FileHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileHistoryRepository extends JpaRepository<FileHistory, Long> {
}