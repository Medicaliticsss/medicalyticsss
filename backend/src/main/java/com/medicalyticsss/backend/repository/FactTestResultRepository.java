package com.medicalyticsss.backend.repository;

import com.medicalyticsss.backend.model.FactTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FactTestResultRepository extends JpaRepository<FactTestResult, Long> {

    @Modifying
    @Query("DELETE FROM FactTestResult f WHERE f.fileHistory.id = :fileId")
    void deleteByFileHistoryId(@Param("fileId") Long fileId);
    long countByIsAbnormal(boolean isAbnormal);
}