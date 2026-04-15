package com.medicalyticsss.backend.repository;

import com.medicalyticsss.backend.model.TestType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TestTypeRepository extends JpaRepository<TestType, Long> {
    Optional<TestType> findByTestCode(String testCode);
}