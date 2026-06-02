package com.medicalyticsss.backend.repository;

import com.medicalyticsss.backend.model.FileHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileHistoryRepository extends JpaRepository<FileHistory, Long> {
    List<FileHistory> findByUser_Username(String username);
}
