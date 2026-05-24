package com.medicalyticsss.backend.repository;

import com.medicalyticsss.backend.model.ProcessingError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProcessingErrorRepository extends JpaRepository<ProcessingError, Long> {

    @Modifying
    @Query("DELETE FROM ProcessingError p WHERE p.fileHistory.id = :fileId")
    void deleteByFileHistoryId(@Param("fileId") Long fileId);
    List<ProcessingError> findByFileHistoryId(Long fileHistoryId);
}
